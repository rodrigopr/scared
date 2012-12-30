package com.github.rodrigopr.scared.testmodel

import com.github.rodrigopr.scared.annotations.{Id, Index, Persist}
import scala.Array
import reflect.BeanInfo
import annotation.target.field

@BeanInfo
@Persist(
  name = "noid",
  customIndexes = Array(
    new Index(fields = Array("name"))
  )
)
case class NoIdModel(
  id: Long,
  name: String
) {
  def this() = this(0, null)
}
