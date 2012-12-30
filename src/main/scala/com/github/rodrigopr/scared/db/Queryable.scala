package com.github.rodrigopr.scared.db

import collection.mutable
import com.github.rodrigopr.scared.db.Queryable.{Order, JoinMethod}
import com.github.rodrigopr.scared.db.Queryable.{Pagination, ScorePagination, FixedInterval}
import com.github.rodrigopr.scared.mapping.{Index, Model}
import java.util.UUID
import com.redis.RedisClient

protected class Queryable[T](context: RedisContext, where: Where, modelInfo: Model)(implicit m: Manifest[T]) {
  private val indexKeys = mutable.ArrayBuffer[String]()
  private var joinMethod = JoinMethod.INTERSECTION
  private var pagination: Pagination = FixedInterval(0, -1)
  private var order = Order.ASC

  // populate with main Where clause
  and(where)

  /**
   * Used for join index results, take in mind that all joins are copulated in execution time,
   * and can hurt the overall performance, as redis will only answer one request at time. <br>
   *
   * Also all whereClauses need to be complete, ie, need to map to a index,
   * you can use the same index many times though.
   *
   * @see com.github.rodrigopr.scared.db.Where
   */
  def and(whereClause: Where): Queryable[T] = {
    val clauses = whereClause.getClauses

    clauses.keys.foreach(field =>
      if (!modelInfo.fieldsInfo.contains(field)) {
        sys.error("Field %s not found".format(field))
      }
    )

    val index = modelInfo.getIndexFor(clauses.keySet)

    if (!index.isValid(clauses)) {
      sys.error("Where clause is not valid, can't contains null or empty list")
    }

    val entries: List[IndexEntry] = index.genIndexEntry(clauses)

    entries.map(e => e.key).foreach(i => indexKeys.append(i))

    this
  }

  /**
   * Define the query join method, default is Intersection
   */
  def joinMethod(joinMethod: JoinMethod.JoinMethod): Queryable[T] = {
    this.joinMethod = joinMethod

    this
  }

  /**
   * Paginate the result, use either [[com.github.rodrigopr.scared.db.Queryable.FixedInterval]]
   * or [[com.github.rodrigopr.scared.db.Queryable.ScorePagination]]
   */
  def paginate(p: Pagination): Queryable[T] = {
    this.pagination = p
    this
  }

  def order(o: Order.Order) : Queryable[T] = {
    this.order = o
    this
  }

  /**
   * Returns a lazy-loaded iterator of objects T
   */
  def execute(): Iterator[T] = {
    val redis = context.redis
    val allIndexKeys = indexKeys.toList

    val key = allIndexKeys match {
      case List(singleKey) => singleKey

      case allKeys => {
        // if multiple keys, need to join it first, each pagination needs to recompute this!
        // TODO: cache join

        val dest: String = "join::%s::%s".format(modelInfo.name, UUID.randomUUID().toString)

        joinMethod match {
          case JoinMethod.INTERSECTION => redis.zinterstore(dest, allKeys, RedisClient.MAX)
          case JoinMethod.UNION => redis.zunionstore(dest, allKeys, RedisClient.MAX)
        }

        dest
      }
    }

    val redisOrder = if(order == Order.ASC) RedisClient.ASC else RedisClient.DESC

    // use proper pagination
    val keys = pagination match {
      case FixedInterval(start, end) =>
        redis.zrange(key, start, end, redisOrder)
      case ScorePagination(fromScoreAny, toScoreAny, minInclusive, maxInclusive, skip, take) =>
        val fromScore = Index.getScore(Some(fromScoreAny))
        val toScore = Option(toScoreAny).map(s => Index.getScore(Some(s))).getOrElse {
          if (order == Order.ASC) Double.MaxValue else Double.MinValue
        }

        if (order == Order.ASC) {
          redis.zrangebyscore(key, fromScore, minInclusive, toScore, maxInclusive, take.map(t => (skip, t)), redisOrder)
        } else {
          redis.zrangebyscore(key, toScore, maxInclusive, fromScore, minInclusive, take.map(t => (skip, t)), redisOrder)
        }
    }

    // return lazy-loaded iterator
    keys.get.iterator
      .map(id => context.load[T](id))
      .filter(_.isDefined)
      .map(_.get)
  }
}


object Queryable {
  object JoinMethod extends Enumeration {
    type JoinMethod = Value
    val UNION, INTERSECTION = Value
  }

  object Order extends Enumeration {
    type Order = Value
    val DESC, ASC = Value
  }

  sealed abstract class Pagination

  /**
   * Paginate the index through a fixed interval
   *
   * @param start 0-based position to start
   * @param end 0-based position to end
   */
  case class FixedInterval(start: Int, end: Int) extends Pagination

  /**
   * Paginate from a score interval. <br>
   *
   * If `skip` and `take` isn't specified(`None`) it will result all elements in the score range. <br>
   *
   * Both `fromScore` and `toScore` need to be Double, Number or Boolean. <br>
   *
   * @param skip optional num items to skip
   * @param take optional num items to take, need to be specified if skip was also used
   */
  case class ScorePagination(
    fromScore: Any,
    toScore: Any = null,
    minInclusive: Boolean = false,
    maxInclusive: Boolean = true,
    skip: Int = 0,
    take: Option[Int] = None
  ) extends Pagination
}

