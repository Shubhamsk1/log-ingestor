package controllers

import models.{LogEntry, LogFilters}
import play.api.mvc.{BaseController, ControllerComponents}
import play.api.libs.json.{Json}

import javax.inject.Inject

class LogController @Inject()(val controllerComponents: ControllerComponents) extends BaseController{

  var logs = List.empty[LogEntry]

  def ingestLog() = Action(parse.json[LogEntry]) { request =>
    val logEntry = request.body
    logs = logEntry :: logs
    Ok("Log ingested successfully")
  }

  def searchLogs(query: String, filters: LogFilters) = Action {
    val results = logs.filter(log =>
      log.level.contains(filters.level) &&
        log.message.contains(filters.message) &&
        log.resourceId.contains(filters.resourceId) &&
        // ... similar checks for other filters
        (log.message.contains(query) || log.level.contains(query)) // Full-text search
    )
    Ok(Json.toJson(results))
  }
}
