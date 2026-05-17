package com.logingestor.consumer.storage

import com.logingestor.consumer.model.LogEntry
import org.slf4j.{Logger, LoggerFactory}
import scalikejdbc._

class PostgresBatchWriter extends LogStorageWriter {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  override def write(entries: Seq[LogEntry]): Either[String, Int] = {
    if (entries.isEmpty) return Right(0)

    try {
      val inserted = DB.localTx { implicit session =>
        val batchParams: Seq[Seq[Any]] = entries.map { e =>
          Seq(e.level, e.message, e.resourceId, e.timestamp, e.traceId, e.spanId, e.commit, e.parentResourceId.getOrElse(""))
        }

        val sql = SQL("""
          INSERT INTO logs (level, message, resource_id, timestamp, trace_id, span_id, commit, parent_resource_id)
          VALUES (?, ?, ?, ?::timestamptz, ?, ?, ?, ?)
        """)

        val count = sql.batch(batchParams: _*).apply().sum
        logger.debug("Inserted {} log entries", count)
        count
      }

      Right(inserted)
    } catch {
      case e: Exception =>
        val msg = s"Batch insert failed: ${e.getMessage}"
        logger.error(msg)
        Left(msg)
    }
  }

  override def close(): Unit = {
    logger.info("PostgresBatchWriter closed")
  }
}
