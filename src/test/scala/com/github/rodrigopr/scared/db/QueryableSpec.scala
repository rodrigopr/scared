package com.github.rodrigopr.scared.db

import org.scalatest.{BeforeAndAfterEach, FeatureSpec}
import org.scalatest.matchers.ShouldMatchers
import com.redis.RedisClient
import com.github.rodrigopr.scared.testmodel.Occurrence
import com.github.rodrigopr.scared.db.Queryable.{JoinMethod, ScorePagination, Order, FixedInterval}
import java.util.Date

class QueryableSpec extends FeatureSpec with BeforeAndAfterEach with ShouldMatchers {
  val redis = new RedisClient
  val context = new RedisContext

  override protected def beforeEach() {
    super.beforeEach()

    redis.flushall

    1.to(100).foreach { n =>
      val o = new Occurrence(n, "name" + n, new java.util.Date(n), "Failing", List((n % 3), 10, 20), List(n, 1000 + (n % 2)), List(1))
      context.save(o)
    }
  }

  feature("Querying object in redis") {
    scenario("Simple query") {
      val items = context.query[Occurrence](Where("name", "name10")).execute().toList

      items.size should be (1)
      items.head.id should be(10)
    }

    scenario("Compositive query single result") {
      val items = context
        .query[Occurrence](new Where{ 'status === "Failing" && 'servers === 10})
        .execute().toList

      items.size should be (1)
      items.head.id should be(10)
    }

    scenario("Compositive query many results") {
      val items = context
        .query[Occurrence](new Where{ 'status === "Failing" && 'related === 1})
        .execute().toList

      items.size should be (100)
      items.map(_.id).toList should be(1.to(100).toList)
    }
    scenario("Query pagination using FixedPositions") {
      val query = context.query[Occurrence](new Where{ 'status === "Failing" && 'related === 1})

      var items = query.paginate(FixedInterval(0, 9)).execute().toList

      items.size should be (10)
      items.map(_.id).toList should be(1.to(10).toList)

      items = query.paginate(FixedInterval(10, 19)).execute().toList

      items.size should be (10)
      items.map(_.id).toList should be(11.to(20).toList)

      items = query.paginate(FixedInterval(50, -1)).execute().toList

      items.size should be (50)
      items.map(_.id).toList should be(51.to(100).toList)
    }

    scenario("Query pagination ordering using FixedPositions") {
      val query = context.query[Occurrence](new Where{ 'status === "Failing" && 'related === 1}).order(Order.DESC)

      var items = query.paginate(FixedInterval(0, 9)).execute().toList

      items.size should be (10)
      items.map(_.id).toList should be(100.to(91, -1).toList)

      items = query.paginate(FixedInterval(10, 19)).execute().toList

      items.size should be (10)
      items.map(_.id).toList should be(90.to(81, -1).toList)

      items = query.paginate(FixedInterval(50, -1)).execute().toList

      items.size should be (50)
      items.map(_.id).toList should be(50.to(1, -1).toList)
    }

    scenario("Query pagination using ScorePagination") {
      val query = context.query[Occurrence](new Where{ 'status === "Failing" && 'related === 1})

      var items = query.paginate(ScorePagination(new Date(0), take = Some(10))).execute().toList

      items.size should be (10)
      items.map(_.id).toList should be(1.to(10).toList)

      items = query.paginate(ScorePagination(new Date(10), take = Some(10))).execute().toList

      items.size should be (10)
      items.map(_.id).toList should be(11.to(20).toList)

      items = query.paginate(ScorePagination(new Date(50))).execute().toList

      items.size should be (50)
      items.map(_.id).toList should be(51.to(100).toList)
    }

    scenario("Query pagination using ScorePagination inclusive") {
      val query = context.query[Occurrence](new Where{ 'status === "Failing" && 'related === 1})

      var items = query.paginate(ScorePagination(new Date(1), new Date(10), minInclusive = true)).execute().toList

      items.size should be (10)
      items.map(_.id).toList should be(1.to(10).toList)

      items = query.paginate(ScorePagination(new Date(11), new Date(20), minInclusive = true)).execute().toList

      items.size should be (10)
      items.map(_.id).toList should be(11.to(20).toList)

      items = query.paginate(ScorePagination(new Date(51), new Date(Long.MaxValue), minInclusive = true)).execute().toList

      items.size should be (50)
      items.map(_.id).toList should be(51.to(100).toList)
    }

    scenario("Query pagination ordering using ScorePagination") {
      val query = context.query[Occurrence](new Where{ 'status === "Failing" && 'related === 1}).order(Order.DESC)

      var items = query.paginate(ScorePagination(new Date(Long.MaxValue), take = Some(10))).execute().toList

      items.size should be (10)
      items.map(_.id).toList should be(100.to(91, -1).toList)

      items = query.paginate(ScorePagination(new Date(91), take = Some(10))).execute().toList

      items.size should be (10)
      items.map(_.id).toList should be(90.to(81, -1).toList)

      items = query.paginate(ScorePagination(new Date(51))).execute().toList

      items.size should be (50)
      items.map(_.id).toList should be(50.to(1, -1).toList)
    }

    scenario("Joining indexes - Union") {
      val items = context
        .query[Occurrence](new Where{ 'status === "Failing" && 'servers === 1})
        .and(new Where{ 'status === "Failing" && 'servers === 50})
        .joinMethod(JoinMethod.UNION)
        .execute().toList

      items.size should be (2)
      items.map(_.id).toList should be(List(1, 50))
    }

    scenario("Joining indexes - Intersection") {
      val items = context
        .query[Occurrence](new Where{ 'status === "Failing" && 'services === 0})
        .and(new Where{ 'status === "Failing" && 'servers === 1000})
        .joinMethod(JoinMethod.INTERSECTION)
        .execute().toList

      val expectId = 1.to(100).filter(i => (i % 3 + i % 2) == 0).toList

      items.size should be (expectId.size)
      items.map(_.id).toList should be(expectId)
    }

    scenario("Joining indexes with list condition") {
      val items = context
        .query[Occurrence](new Where{ 'status === "Failing" && 'servers === List(1, 50)})
        .joinMethod(JoinMethod.UNION)
        .execute().toList

      items.size should be (2)
      items.map(_.id).toList should be(List(1, 50))
    }

    scenario("Try query with no suitable index") {
      val exception = intercept[RuntimeException] {
        context.query[Occurrence](new Where{ 'status === "Failing" && 'name === "abc"})
      }

      exception.getMessage should be("No suitable index for Set(name, status) in entity occurrence")
    }

    scenario("Try query with invalid field") {
      val exception = intercept[RuntimeException] {
        context.query[Occurrence](new Where{ 'invalidField === true})
      }

      exception.getMessage should be("Field invalidField not found")
    }
  }
}
