package services

import models.LogInput

import scala.concurrent.Future

trait MessageQueuePublisher {
  def publish(logs: Seq[LogInput]): Future[Unit]
  def close(): Unit
}
