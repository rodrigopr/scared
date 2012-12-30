package com.github.rodrigopr.scared.mapping

import java.util.Date
import com.github.rodrigopr.scared.db.IndexEntry

class Index(val fields: List[String], val orderField: Option[String], modelInfo: Model) {
  val name = "@" + modelInfo.name + fields.mkString("[", ":", "]")

  /**
   * Returns if `data` is valid for this Index.
   * null values and empty list are considered invalids.
   */
  def isValid(data: Map[String, Any]): Boolean = fields.forall { f =>
      data.get(f).map {
        case v: List[_] => !v.isEmpty
        case v => v != null
      }.getOrElse(false)
    }

  /**
   * Return a list of IndexEntry, extracted from `data` for this Index
   */
  def genIndexEntry(data: Map[String, Any]): List[IndexEntry] = if (isValid(data)) {
    fields.foldRight(List(List[String]())) {
      case (field, list) =>
        list.map { indexEntry =>
          data(field) match {
            case d: Date => List(d.getTime.toString :: indexEntry)
            case l: List[_] => l.filter(null !=).map(v => v.toString :: indexEntry)
            case o => List(o.toString :: indexEntry)
          }
        }.flatten
    }.map(f => IndexEntry(name + "=>" + f.mkString(":$:"), Index.getScore(orderField.map(data.get).getOrElse(None))))
  } else List()
}

object Index {
  /**
   * Return a score for a value, or 0.0 if it's none.
   * Score for Date is the unix time, number is itself, boolean are mapped to 1 and 0.
   * Any other type will throw a exception
   */
  def getScore(value: Option[Any]): Double = value.map {
    case d: Date => d.getTime.toDouble
    case n: Number => n.doubleValue
    case b: Boolean => if (b) 1.0 else 0.0
    case _ => sys.error("Wrong type for score: " + value)
  }.getOrElse(0.0) //TODO: make it possible to compute outside framework

}
