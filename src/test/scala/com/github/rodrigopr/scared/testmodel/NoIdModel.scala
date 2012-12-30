package com.github.rodrigopr.scared.testmodel

import com.github.rodrigopr.scared.annotations.{Index, Persist}
import scala.Array
import reflect.BeanInfo

@BeanInfo
@Persist(
  name = "noid",
  customIndexes = Array(
    new Index(fields = Array("name"))
  )
)
case class NoIdModel(
  field1: Long,
  name: String
)
