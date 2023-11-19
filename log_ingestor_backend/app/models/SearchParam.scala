package models

import org.joda.time.DateTime
import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue, Json, OFormat, OWrites, Reads}

import scala.util.{Failure, Success, Try}





sealed trait QueryType {
  def toString: String
}

object QueryType {
  private val level = "level"
  private val message = "message"
  private val resource = "resource"
  private val timestamp = "timestamp"

  case object LevelFilter extends QueryType {
    override def toString: String = level
  }

  case object MessageFilter extends QueryType {
    override def toString: String = message
  }

  case object ResourceIdFilter extends QueryType {
    override def toString: String = resource
  }

  case object TimestampFilter extends QueryType {
    override def toString: String = timestamp
  }

  implicit val format: Reads[QueryType] = new Reads[QueryType] {
    override def reads(json: JsValue): JsResult[QueryType] = {
      Try {
        val key = (json \ "query").as[String]
        fromString(key).get
      } match {
        case scala.util.Success(queryType) => JsSuccess(queryType)
        case scala.util.Failure(exception) => JsError(exception.getMessage)
      }
    }

//    override def writes(o: QueryType): JsValue = Json.obj("query" -> o.toString)
  }

  def fromString(key: String): Try[QueryType] = Try {
    key match {
      case `level` => LevelFilter
      case `message` => MessageFilter
      case `resource` => ResourceIdFilter
      case `timestamp` => TimestampFilter
    }
  }
}


case class SearchParam(
                        query:Option[QueryType],
                        level:Option[String],
                        search_string: Option[String],
                        resource_id: Option[String],
                        fromTime: Option[DateTime],
                        tillTime: Option[DateTime]
                      )

case object SearchParam {
//  implicit val searchParamFormat: OWrites[SearchParam] = Json.writes[SearchParam]

  implicit val reads = new Reads[SearchParam] {
    override def reads(ev: JsValue): JsResult[SearchParam] = Try{

      SearchParam(
        query = QueryType.fromString(key = (ev \ "query").as[String]).toOption,
        level = (ev \ "level").asOpt[String],
        resource_id = (ev \ "resource_id").asOpt[String],
        search_string = (ev \ "search_string").asOpt[String],
        fromTime =(ev \"fromTime").asOpt[String].map(ft => DateTime.parse(ft)),
        tillTime =(ev \"tillTime").asOpt[String].map(ft => DateTime.parse(ft))
      )

    } match {
      case Failure(exception) => JsError(exception.getMessage)
      case Success(value)  => JsSuccess(value)
     }
  }



}