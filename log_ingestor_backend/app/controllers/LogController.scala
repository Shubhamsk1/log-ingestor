package controllers

import models.{LogEntry, LogFilters}
import play.api.mvc.{BaseController, ControllerComponents}
import play.api.libs.json.Json
import services.LogService
import scalikejdbc.{DB, DBSession, SQL}

import javax.inject.Inject
import scala.concurrent.Future

class LogController @Inject()(val controllerComponents: ControllerComponents,logService: LogService) extends BaseController{

  var logs = List.empty[LogEntry]

  def ingestLog() = Action.async(parse.json[LogEntry]) { request =>
    val logEntry = request.body

    request.body
    logs = logEntry :: logs
    Future.successful(Ok("Log ingested successfully"))
  }

  def searchLogs(query: String, filters: LogFilters) = Action.async { request =>
    val results = logs.filter(log =>
      log.level.contains(filters.level) &&
        log.message.contains(filters.message) &&
        log.resourceId.contains(filters.resourceId) &&
        // ... similar checks for other filters
        (log.message.contains(query) || log.level.contains(query)) // Full-text search
    )
    Future.successful(Ok(Json.toJson(results)))
  }
}
