package models

import org.joda.time.DateTime
import play.api.libs.json.{Json, OFormat}

case class SearchParam(
                        query:String,
                        fromTime: Option[DateTime],
                        tillTime: Option[DateTime]
                      )

case object SearchParam {
  implicit val searchParamFormat: OFormat[SearchParam] = Json.format[SearchParam]
}