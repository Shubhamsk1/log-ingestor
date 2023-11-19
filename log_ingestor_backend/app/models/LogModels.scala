package models

import play.api.libs.json.{Json, OFormat}

case class LogEntry(
                     level: String,
                     message: String,
                     resourceId: String,
                     timestamp: String,
                     traceId: String,
                     spanId: String,
                     commit: String,
                     metadata: Option[Metadata]
                   )

object LogEntry {
  implicit val logEntryFormat: OFormat[LogEntry] = Json.format[LogEntry]
}

case class Metadata(parentResourceId: String)

object Metadata {
  implicit val metadataFormat: OFormat[Metadata] = Json.format[Metadata]
}


case class LogFilters(
                       level: String,
                       message: String,
                       resourceId: String,
                       timestamp: String,
                       traceId: String,
                       spanId: String,
                       commit: String,
                       metadata: Option[Metadata]
                     )

object LogFilters {
  implicit val logFiltersFormat: OFormat[LogFilters] = Json.format[LogFilters]
}



