import com.github.rodrigopr.scared.db.RedisContext
import com.github.rodrigopr.scared.serializer.SerDe

class MyOwnSerDe extends SerDe {
  def serialize[T <: AnyRef](obj: T)(implicit m: Manifest[T]): Array[Byte] = {
    sys.error("stub")
  }

  def deserialize[T](data: Array[Byte])(implicit m: Manifest[T]): T = {
    sys.error("stub")
  }
}

val redisContext = new RedisContext(new MyOwnSerDe)