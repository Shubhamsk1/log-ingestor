package controllers

import models.{LogInput , QueryType, SearchParam}
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

    request.body.validate[List[LogInput]]  match {
      case JsError(errors) =>Future.successful(BadRequest(Json.obj("error" -> s"Invalid Log Entry case class:: ${errors.toString()}")))

      case JsSuccess(value, _) =>
       logService.insertLogs(logEntries = value).map{res =>
          Ok(Json.obj("success"->"Log ingested successfully")).withHeaders(
            "Access-Control-Allow-Origin" -> "http://localhost:3001"
          )
        }.recover{ case e =>
          BadRequest(Json.obj("error" -> e.getMessage))
       }
    }
  }


  def searchLogs() = Action.async(parse.json) {request =>


    request.body.validate[SearchParam] match {
      case JsError(errors) =>{
        println("Validation failed")
        Future.successful(BadRequest(Json.obj("error" -> s"Invalid Search PARAM case class ::${errors.toString()}")))
      }

      case JsSuccess(value,path) => {
        if(value.query.isEmpty){
          println("IT FAILED HERE")
          Future.successful(BadRequest(Json.obj("error" -> "Missing or invalid query type")))
        }
        else{
          value.query.get match {
            case QueryType.LevelFilter =>
              println("Inside level")
             logService.getLogsByLevel(level = value.level).map { res =>
               Ok(Json.toJson(res)).withHeaders(
                 "Access-Control-Allow-Origin" -> "http://localhost:3001",
                 "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
                 "Access-Control-Allow-Headers" -> "Content-Type, Authorization"
               )
              }.recover{ case e  =>
               println(s"Level failed ${e.getMessage}")
               BadRequest(e.getMessage)

             }
            case QueryType.MessageFilter =>
              logService.getLogsByMessage(search_str = value.search_string).map { res =>
                Ok(Json.toJson(res)).withHeaders(
                  "Access-Control-Allow-Origin" -> "http://localhost:3001",
                  "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
                  "Access-Control-Allow-Headers" -> "Content-Type, Authorization"
                )
              }
            case QueryType.ResourceIdFilter =>
              logService.getLogsByResource(resource_id = value.resource_id).map { res =>
                Ok(Json.toJson(res)).withHeaders(
                  "Access-Control-Allow-Origin" -> "http://localhost:3001",
                  "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
                  "Access-Control-Allow-Headers" -> "Content-Type, Authorization"
                )
              }
            case QueryType.TimestampFilter =>
              logService.getLogsByRange(fromTime = value.fromTime,tillTime = value.tillTime).map { res =>
                Ok(Json.toJson(res)).withHeaders(
                  "Access-Control-Allow-Origin" -> "http://localhost:3001",
                  "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
                  "Access-Control-Allow-Headers" -> "Content-Type, Authorization"
                )
              }

            case _ => Future.successful(BadRequest(Json.obj("error" -> "Missing or invalid query type")))


          }
        }

      }
    }

  }
}
