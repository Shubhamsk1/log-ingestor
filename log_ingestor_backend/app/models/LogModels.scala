package models

import org.joda.time.DateTime
import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue, Json, OFormat, Reads, Writes}
import scalikejdbc.WrappedResultSet

import scala.util.{Failure, Success, Try}

case class LogInputMetadata(parent_resource_id: String)



object LogInputMetadata {
  implicit val metadataFormat: OFormat[LogInputMetadata] = Json.format[LogInputMetadata]
}


case class LogInput(
                     level: String,
                     message: String,
                     resource_id: String,
                     timestamp: DateTime,
                     trace_id: String,
                     span_id: String,
                     commit: String,
                     metadata: Option[LogInputMetadata]
                   )

object LogInput {
  implicit val logEntryReads: Reads[LogInput] = new Reads[LogInput] {
    override def reads(json: JsValue): JsResult[LogInput] = Try {
      LogInput(
        level = (json \ "level").as[String],
        message = (json \ "message").as[String],
        resource_id = (json \ "resourceId").as[String],
        timestamp = DateTime.parse((json \ "timestamp").as[String]),
        trace_id = (json \ "traceId").as[String],
        span_id = (json \ "spanId").as[String],
        commit = (json \ "commit").as[String],
        metadata = (json \ "metadata" \ "parentResourceId").asOpt[String].map(id => LogInputMetadata(parent_resource_id = id))
      )
    } match {
      case Failure(e) => JsError(e.getMessage)
      case Success(logEntry) => JsSuccess(logEntry)
    }
  }

  implicit val logEntryWrites: Writes[LogInput] = new Writes[LogInput] {
    override def writes(o: LogInput): JsValue = Json.obj(
      "level" -> o.level,
      "message" -> o.message,
      "timestamp" -> o.timestamp.toString,
      "trace_id" -> o.trace_id,
      "span_id" -> o.span_id,
      "commit" -> o.commit,
      "metadata" -> Json.obj("parent_resource_id" -> o.metadata.map(_.parent_resource_id))
    )
  }

  def formatDateTime(rs: WrappedResultSet, columnName: String): Option[String] = {
    Option(rs.zonedDateTime(columnName)).map { zdt =>
      zdt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
    }
    }

  def fromDb(rs: WrappedResultSet): LogInput = {
    LogInput(
      level = rs.string("level"),
      message = rs.string("message"),
      resource_id = rs.string("resource_id"),
      timestamp = DateTime.parse(formatDateTime(rs,"timestamp").get),
      trace_id = rs.string("trace_id"),
      span_id = rs.string("span_id"),
      commit = rs.string("commit"),
      metadata = Option(LogInputMetadata(parent_resource_id = rs.string("parent_resource_id")))
    )
  }
}
