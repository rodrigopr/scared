package com.github.rodrigopr.scared.serializer
import sjson.json.Serializer.{ SJSON => jsonSerializer }

class SJsonSerDe extends SerDe {
  def serialize[T <: AnyRef](obj: T)(implicit m: Manifest[T]): Array[Byte] = jsonSerializer.out(obj)
  def deserialize[T](data: Array[Byte])(implicit m: Manifest[T]): T = jsonSerializer.in[T](data)
}
