package controllers

import models.{LogEntry, LogFilters}
import org.joda.time.DateTime
import play.api.mvc.{BaseController, ControllerComponents}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, OFormat}
import services.LogService
import scalikejdbc.{DB, DBSession, SQL}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}



class LogController @Inject()(val controllerComponents: ControllerComponents,logService: LogService) extends BaseController{

  private implicit val ec: ExecutionContext = controllerComponents.executionContext

  def ingestLog() = Action.async(parse.json) { request =>

    request.body.validate[List[LogEntry]]  match {
      case JsError(errors) =>Future.successful(BadRequest(Json.obj("error" -> "Invalid Log Entry case class")))

      case JsSuccess(value, _) =>
       logService.insertLogs(logEntries = value).map{res =>
          Ok(Json.obj("success"->"Log ingested successfully"))
        }.recover{ case e =>
          BadRequest(Json.obj("error" -> e.getMessage))
       }
    }
  }

//  def searchLogs(query: String, filters: LogFilters) = Action.async { request =>
//    val results = logs.filter(log =>
//      log.level.contains(filters.level) &&
//        log.message.contains(filters.message) &&
//        log.resourceId.contains(filters.resourceId) &&
//        // ... similar checks for other filters
//        (log.message.contains(query) || log.level.contains(query)) // Full-text search
//    )
//    Future.successful(Ok(Json.toJson(results)))
//  }

//  def searchLogs() = Action.async(parse.json) {request =>
//    request.body.validate[]
//
//  }
}
