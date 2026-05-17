package com.logingestor.consumer

import com.logingestor.consumer.dlq.DeadLetterQueue
import com.logingestor.consumer.model.LogEntry
import com.logingestor.consumer.parser.LogParser
import com.logingestor.consumer.storage.{LogStorageWriter, StorageWriterFactory}
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.{Logger, LoggerFactory}
import scalikejdbc.ConnectionPool

import java.time.Duration
import scala.jdk.CollectionConverters._

object LogConsumer extends App {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val bootstrapServers = sys.env.getOrElse("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
  private val dbUrl = sys.env.getOrElse("DB_URL", "jdbc:postgresql://localhost:5432/logs_ingestor_db")
  private val dbUser = sys.env.getOrElse("DB_USERNAME", "shubhamkudekar")
  private val dbPass = sys.env.getOrElse("DB_PASSWORD", "")
  private val topic = "logs-raw"
  private val batchSize = sys.env.get("BATCH_SIZE").flatMap(v => scala.util.Try(v.toInt).toOption).getOrElse(100)
  private val pollTimeoutMs = sys.env.get("POLL_TIMEOUT_MS").flatMap(v => scala.util.Try(v.toLong).toOption).getOrElse(1000L)

  ConnectionPool.singleton(dbUrl, dbUser, dbPass)
  logger.info("Database connection pool initialized — url: {}", dbUrl)

  private val parser = new LogParser()
  private val storageWriters: Seq[LogStorageWriter] = StorageWriterFactory.createWriters()
  private val deadLetterQueue = new DeadLetterQueue(bootstrapServers)

  private val props = new java.util.Properties()
  props.put("bootstrap.servers", bootstrapServers)
  props.put("group.id", "log-ingestor-consumer")
  props.put("key.deserializer", classOf[StringDeserializer].getName)
  props.put("value.deserializer", classOf[StringDeserializer].getName)
  props.put("auto.offset.reset", "earliest")
  props.put("enable.auto.commit", "false")
  props.put("max.poll.records", batchSize.toString)

  private val consumer = new KafkaConsumer[String, String](props)
  consumer.subscribe(List(topic).asJava)

  private val mainThread = Thread.currentThread()
  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run(): Unit = {
      logger.info("Shutdown signal received")
      consumer.wakeup()
      mainThread.join()
    }
  })

  logger.info("Consumer started — bootstrap: {}, topic: {}, batchSize: {}, pollTimeoutMs: {}",
    bootstrapServers, topic, batchSize, pollTimeoutMs)

  try {
    while (true) {
      val records: ConsumerRecords[String, String] = consumer.poll(Duration.ofMillis(pollTimeoutMs))

      if (!records.isEmpty) {
        processBatch(records)
        consumer.commitSync()
      }
    }
  } catch {
    case _: WakeupException =>
      logger.info("Consumer wakeup — shutting down")
    case e: Exception =>
      logger.error("Consumer error: {}", e.getMessage, e)
  } finally {
    consumer.close()
    deadLetterQueue.close()
    storageWriters.foreach(_.close())
    logger.info("Consumer shut down complete")
  }

  private def processBatch(records: ConsumerRecords[String, String]): Unit = {
    var parsedCount = 0
    var writeFailures = 0

    records.asScala.foreach { record =>
      parser.parse(record.value()) match {
        case Right(entries) =>
          if (entries.nonEmpty) {
            val results = storageWriters.map { w =>
              w.write(entries) match {
                case Right(count) => Right(count)
                case Left(error) =>
                  logger.error("Write failed for {} from partition={}, offset={}: {}",
                    w.getClass.getSimpleName, record.partition(), record.offset(), error)
                  Left(error)
              }
            }

            val allFailed = results.forall(_.isLeft)
            if (allFailed) {
              writeFailures += 1
              val errors = results.collect { case Left(e) => e }.mkString("; ")
              deadLetterQueue.send(entries, errors)
            } else {
              parsedCount += entries.length
            }
          }
        case Left(parseError) =>
          writeFailures += 1
          logger.error("Parse failed for record partition={}, offset={}: {}",
            record.partition(), record.offset(), parseError)
      }
    }

    if (writeFailures > 0) {
      logger.warn("Batch processed — {} entries written, {} failures", parsedCount, writeFailures)
    } else {
      logger.info("Batch processed — {} entries written successfully", parsedCount)
    }
  }
}
