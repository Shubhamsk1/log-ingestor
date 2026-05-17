package services

import models.LogInput
import org.slf4j.{Logger, LoggerFactory}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LogIngestionService @Inject()(
  publisher: MessageQueuePublisher
)(implicit ec: ExecutionContext) {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def ingest(logs: Seq[LogInput]): Future[Unit] = {
    if (logs.isEmpty) {
      logger.warn("Received empty log batch")
      return Future.unit
    }

    logger.info("Publishing {} log entries to queue", logs.length)

    publisher.publish(logs).recover { case e =>
      logger.error("Failed to queue {} logs: {}", logs.length, e.getMessage)
      throw e
    }
  }
}
