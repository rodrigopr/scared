package com.github.rodrigopr.scared.serializer

trait SerDe {
  def serialize[T <: AnyRef](obj: T)(implicit m: Manifest[T]): Array[Byte]
  def deserialize[T](data: Array[Byte])(implicit m: Manifest[T]): T
}

object SerDe {
  implicit val defaultSerializer: SerDe = new KryoSerDe
}
