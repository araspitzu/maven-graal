package fr.acinq.sample

import org.json4s.CustomSerializer
import org.json4s.JsonAST.{JField, JInt, JObject, JString}

object Utils {

  case class InfoResponse(date: String)

  object InfoResponseSerializer extends CustomSerializer[InfoResponse](format => ({
    null
  },{
    case infoResponse: InfoResponse => JObject(List(
      JField("date", JString(infoResponse.date))
    ))
  }))
}
