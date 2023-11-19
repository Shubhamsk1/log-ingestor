package models

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
