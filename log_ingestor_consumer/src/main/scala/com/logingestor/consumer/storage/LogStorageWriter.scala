package com.logingestor.consumer.storage

import com.logingestor.consumer.model.LogEntry

trait LogStorageWriter {
  def write(entries: Seq[LogEntry]): Either[String, Int]
  def close(): Unit
}
