package fr.acinq.sample

import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.codec.binary.Base32
import org.json4s.CustomSerializer
import org.json4s.JsonAST.{JField, JInt, JObject, JString}
import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs._

object Utils extends LazyLogging {

  case class InfoResponse(date: String)
  case class Point(x: Int, y: Int, z: Int)
  case class Person(firstName: String, lastName: String)

  /**
    * JSON Deserializer
    */
  object InfoResponseSerializer extends CustomSerializer[InfoResponse](format => ({
    null
  },{
    case infoResponse: InfoResponse => JObject(List(
      JField("date", JString(infoResponse.date))
    ))
  }))

  object PointSerializer extends CustomSerializer[Point](format => ({
    null
  },{
    case p: Point => JObject(List(
      JField("x", JInt(p.x)),
      JField("y", JInt(p.y)),
      JField("z", JInt(p.z))
    ))
  }))

  object PersonSerializer extends CustomSerializer[Person](format => ({
    null
  },{
    case p: Person => JObject(List(
      JField("firstName", JString(p.firstName)),
      JField("lastName", JString(p.lastName))
    ))
  }))

  /**
    * Scodec usage
    */
  val pointCodec = (
    ("x" | int8) ::
      ("y" | int8) ::
      ("z" | int8)).as[Point]

  def showScodecUsage(): List[String] = {
    logger.info(s"encoding ${points.size} points via scodec")
    points.map(pointCodec.encode(_).require.toByteVector.toHex)
  }


  /**
    * JHeap usage
    */
  object PointComparator extends Ordering[Point]{
    override def compare(a: Point, b: Point): Int = {
      a.x.compareTo(b.x)
    }
  }

  val points = List(
    Point(3,4,5),
    Point(4,5,6),
    Point(2,3,4),
    Point(5,6,7),
    Point(1,2,3)
  )

  def showJHeapUsage():Point = {
    logger.info(s"populating a sorted heap and finding the min")
    val heap = new org.jheaps.tree.SimpleFibonacciHeap[Point, Int](PointComparator)
    points.foreach(heap.insert)
    val min = heap.findMin().getKey
    logger.info(s"min = $min")
    min
  }

  /**
    * Commons-codec base32 example
    */
  def encodeBase32(s: String) = {
    new Base32().encodeAsString(s.getBytes)
  }

}
