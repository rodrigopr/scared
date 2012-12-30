import com.github.rodrigopr.scared.db.Queryable.{JoinMethod, ScorePagination, FixedInterval, Order}
import com.github.rodrigopr.scared.db.{RedisContext, Where}
import java.util.Date
import models._

/**
 * creating context
 */
val context = new RedisContext


val z = new Occurrence(1000, "name2", new java.util.Date(0), "Working", List(1, 2, 4, 3), List(1, 2), List(1))
context.save(z)

println("============== Loading ==============")
val loaded: Option[Occurrence] = context.load[Occurrence](1000)
println( loaded )


context.delete[Occurrence](1000)
/**
 * Or:
 */
context.deleteModel(z)

for (x <- 1.to(20) ) {
  val n = new Occurrence(x, "name" + x, new java.util.Date(x), "Working", List(1, 2, 3), List(x), List(1))
  context.save(n)
}

println("============== simple query ==============")

val query = context.query[Occurrence](new Where {
  'name === "name1"
})

query.execute().foreach(println)

println("============== paginate (0, 9) ==============")

context
  .query[Occurrence](new Where { 'status === "Working" && 'related === 1 })
  .paginate(FixedInterval(0, 9))
  .order(Order.DESC)
  .execute()
  .foreach(println)

println("============== paginate (10, 19) ==============")

context
  .query[Occurrence](new Where { 'status === "Working" && 'related === 1 })
  .paginate(FixedInterval(10, 19))
  .order(Order.DESC)
  .execute()
  .foreach(println)

println("============== paginate score ==============")

context
  .query[Occurrence](new Where { 'status === "Working" && 'related === 1 })
  .paginate(ScorePagination(new Date(5), new Date(13), minInclusive = true))
  .execute()
  .foreach(println)

println("============== join indexes - Intersection ==============")

context
  .query[Occurrence](new Where { 'status === "Working" && 'related === 1 })
  .and(new Where { 'status === "Working" && 'servers === 1 })
  .paginate(FixedInterval(0, 10))
  .order(Order.DESC)
  .execute()
  .foreach(println)

println("============== join indexes - Union ==============")

context
  .query[Occurrence](new Where { 'status === "Working" && 'servers === List(1,2) })
  .paginate(FixedInterval(0, 10))
  .order(Order.DESC)
  .joinMethod(JoinMethod.UNION)
  .execute()
  .foreach(println)

/**
 * value List(1,2) became automatically a join, is the same than:
 * {{{
 *   context
 *   .query[Occurrence](new Where { 'status === "Working" && 'servers === 1 })
     .and(new Where { 'status === "Working" && 'servers === 2 })
 * }}}
 */
