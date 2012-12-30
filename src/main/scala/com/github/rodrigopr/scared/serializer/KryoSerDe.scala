package com.github.rodrigopr.scared.serializer

import com.esotericsoftware.kryo.Kryo
import com.twitter.chill.KryoSerializer
import org.objenesis.strategy.SerializingInstantiatorStrategy
import java.io.ByteArrayOutputStream
import com.esotericsoftware.kryo.io.{Input, Output}

class KryoSerDe extends SerDe {
  // Kryo ain't thread safe
  private val kryo = new ThreadLocal[Kryo] {
    override def initialValue(): Kryo = {
      val kryo = new Kryo
      KryoSerializer.registerCollectionSerializers(kryo)
      kryo.setInstantiatorStrategy(new SerializingInstantiatorStrategy())
      kryo
    }
  }

  def serialize[T <: AnyRef](obj: T)(implicit m: Manifest[T]): Array[Byte] = {
    val baos: ByteArrayOutputStream = new ByteArrayOutputStream(8192)
    val output: Output = new Output(baos)
    kryo.get.writeClassAndObject(output, obj)
    output.flush()
    baos.toByteArray
  }

  def deserialize[T](data: Array[Byte])(implicit m: Manifest[T]): T = {
    kryo.get.readClassAndObject(new Input(data)).asInstanceOf[T]
  }
}
