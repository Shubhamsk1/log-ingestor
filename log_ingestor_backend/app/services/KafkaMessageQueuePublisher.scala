package services

import models.LogInput
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.*

@Singleton
class KafkaMessageQueuePublisher @Inject()(
  config: Configuration,
  lifecycle: ApplicationLifecycle
)(implicit ec: ExecutionContext) extends MessageQueuePublisher {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val bootstrapServers: String = config.getOptional[String]("kafka.bootstrap.servers")
    .filter(_.nonEmpty)
    .orElse(sys.env.get("KAFKA_BOOTSTRAP_SERVERS").filter(_.nonEmpty))
    .getOrElse("localhost:9092")

  private val topic: String = config.getOptional[String]("kafka.topic.logs").getOrElse("logs-raw")
  private val acks: String = config.getOptional[String]("kafka.producer.acks").getOrElse("1")
  private val retries: Long = config.getOptional[Long]("kafka.producer.retries").getOrElse(3L)
  private val lingerMs: Long = config.getOptional[Long]("kafka.producer.linger.ms").getOrElse(10L)
  private val requestTimeoutMs: Int = config.getOptional[Int]("kafka.producer.request.timeout.ms").getOrElse(5000)

  private val props = new java.util.Properties()
  props.put("bootstrap.servers", bootstrapServers)
  props.put("key.serializer", classOf[StringSerializer].getName)
  props.put("value.serializer", classOf[StringSerializer].getName)
  props.put("acks", acks)
  props.put("retries", retries.toString)
  props.put("linger.ms", lingerMs.toString)
  props.put("request.timeout.ms", requestTimeoutMs.toString)

  private val producer = new KafkaProducer[String, String](props)

  logger.info("Kafka publisher initialized — bootstrap: {}, topic: {}", bootstrapServers, topic)

  lifecycle.addStopHook { () =>
    logger.info("Shutting down Kafka publisher")
    Future.successful(close())
  }

  override def publish(logs: Seq[LogInput]): Future[Unit] = {
    val json = Json.toJson(logs).toString()
    val record = new ProducerRecord[String, String](topic, json)
    val promise = Promise[Unit]()

    try {
      producer.send(record, (metadata: org.apache.kafka.clients.producer.RecordMetadata, exception: Exception) => {
        if (exception != null) {
          logger.error("Failed to publish to Kafka [topic={}]: {}", topic, exception.getMessage)
          promise.failure(exception)
        } else {
          logger.debug("Published to Kafka [topic={}, partition={}, offset={}]",
            metadata.topic(), metadata.partition(), metadata.offset())
          promise.success(())
        }
      })
    } catch {
      case e: Exception =>
        logger.error("Exception sending to Kafka: {}", e.getMessage)
        promise.failure(e)
    }

    promise.future
  }

  override def close(): Unit = {
    producer.close(java.time.Duration.ofSeconds(5))
    logger.info("Kafka publisher closed")
  }
}
