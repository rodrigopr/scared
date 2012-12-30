package com.github.rodrigopr.scared.mapping

import collection.Set
import com.github.rodrigopr.scared.annotations.Persist

class Model(clazz: Class[_]) {
  private val persistAnnotation = clazz.getAnnotation(classOf[Persist])

  if(persistAnnotation == null) {
    sys.error("Model ain't mapped with @Persist annotation")
  }

  val name = persistAnnotation.name()

  val fieldsInfo = clazz.getDeclaredFields.map(f => f.getName -> new Field(f)).toMap

  val indexes = persistAnnotation.customIndexes().map {
    index =>
      val orderField = if (index.orderField().isEmpty) None else Some(index.orderField())

      val fields: List[String] = index.fields().toList

      new Index(fields, orderField, this)
  }

  if(fieldsInfo.values.count(_.isId) == 0) {
    fieldsInfo.values.find(_.name == "id").map{field =>
      field.isId = true
    }
  }

  require(fieldsInfo.values.count(_.isId) == 1, "Type %s needs exactly one @id attribute".format(name))

  val idField = fieldsInfo.values.find(_.isId).get

  def getObjectKey(id: Any): String = List(name, id).mkString(":")

  def getIndexFor(fields: Set[String]): Index = indexes.find(i =>
    i.fields.forall(fields.contains) && fields.forall(i.fields.contains)
  ).getOrElse(
    sys.error("No suitable index for %s in entity %s".format(fields, name))
  )
}
