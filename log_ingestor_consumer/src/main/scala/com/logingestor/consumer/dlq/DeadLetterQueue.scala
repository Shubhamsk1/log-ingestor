package com.logingestor.consumer.dlq

import com.logingestor.consumer.model.LogEntry
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Json

class DeadLetterQueue(bootstrapServers: String, topic: String = "logs-raw-dlq") {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val props = new java.util.Properties()
  props.put("bootstrap.servers", bootstrapServers)
  props.put("key.serializer", classOf[StringSerializer].getName)
  props.put("value.serializer", classOf[StringSerializer].getName)
  props.put("acks", "1")
  props.put("retries", "3")
  props.put("linger.ms", "10")

  private val producer = new KafkaProducer[String, String](props)

  def send(failedEntries: Seq[LogEntry], reason: String): Unit = {
    try {
      val payload = Json.obj(
        "reason" -> reason,
        "entries" -> failedEntries.map { e =>
          Json.obj(
            "level" -> e.level,
            "message" -> e.message,
            "resourceId" -> e.resourceId,
            "timestamp" -> e.timestamp,
            "traceId" -> e.traceId,
            "spanId" -> e.spanId,
            "commit" -> e.commit,
            "parentResourceId" -> e.parentResourceId
          )
        }
      )

      val record = new ProducerRecord[String, String](topic, payload.toString())
      producer.send(record, (metadata: org.apache.kafka.clients.producer.RecordMetadata, exception: Exception) => {
        if (exception != null) {
          logger.error("Failed to send to DLQ [topic={}]: {}", topic, exception.getMessage)
        } else {
          logger.warn("Sent {} failed entries to DLQ [topic={}, partition={}, offset={}]",
            failedEntries.length, topic, metadata.partition(), metadata.offset())
        }
      })
    } catch {
      case e: Exception =>
        logger.error("Exception sending to DLQ: {}", e.getMessage)
    }
  }

  def close(): Unit = {
    producer.close(java.time.Duration.ofSeconds(5))
    logger.info("DLQ producer closed")
  }
}
