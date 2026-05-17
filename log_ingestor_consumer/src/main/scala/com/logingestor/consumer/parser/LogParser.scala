package com.logingestor.consumer.parser

import com.logingestor.consumer.model.LogEntry
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.util.Try

class LogParser {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def parse(json: String): Either[String, Seq[LogEntry]] = {
    Try {
      Json.parse(json) match {
        case arr: JsArray =>
          val entries = arr.value.flatMap(parseEntry)
          if (entries.isEmpty && arr.value.nonEmpty) {
            logger.warn("All entries in batch failed to parse")
          }
          entries.toSeq
        case other =>
          logger.warn("Expected JSON array, got: {}", other.getClass.getSimpleName)
          Seq.empty
      }
    }.toEither.left.map { e =>
      val msg = s"Failed to parse JSON: ${e.getMessage}"
      logger.error(msg)
      msg
    }
  }

  private def parseEntry(entry: JsValue): Option[LogEntry] = {
    val level = (entry \ "level").asOpt[String]
    val message = (entry \ "message").asOpt[String]
    val rid = (entry \ "resourceId").asOpt[String].orElse((entry \ "resource_id").asOpt[String])
    val ts = (entry \ "timestamp").asOpt[String]
    val tid = (entry \ "traceId").asOpt[String].orElse((entry \ "trace_id").asOpt[String])
    val sid = (entry \ "spanId").asOpt[String].orElse((entry \ "span_id").asOpt[String])
    val cmt = (entry \ "commit").asOpt[String]
    val parent = (entry \ "metadata" \ "parentResourceId").asOpt[String]
      .orElse((entry \ "metadata" \ "parent_resource_id").asOpt[String])

    (level, message, rid, ts, tid, sid, cmt) match {
      case (Some(l), Some(m), Some(r), Some(t), Some(ti), Some(s), Some(c)) =>
        Some(LogEntry(l, m, r, t, ti, s, c, parent))
      case _ =>
        logger.warn("Skipping entry with missing required fields: level={}, message={}, resourceId={}, timestamp={}, traceId={}, spanId={}, commit={}",
          level, message, rid, ts, tid, sid, cmt)
        None
    }
  }
}
