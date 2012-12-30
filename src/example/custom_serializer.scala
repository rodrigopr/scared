import com.github.rodrigopr.scared.db.RedisContext

/**
 * Scala rules!
 * To modify this behaviour, jus modify those two methods with a trait
 */

trait CustomSerializer { self: RedisContext =>
  override protected def serialize[T <: AnyRef](obj: T)(implicit m: Manifest[T]): Array[Byte] = {
    sys.error("stub")
  }
  override protected def deserialize[T](data: Array[Byte])(implicit m: Manifest[T]): T = {
    sys.error("stub")
  }
}

val redisContext = new RedisContext with CustomSerializer