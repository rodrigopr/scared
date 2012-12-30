package com.github.rodrigopr.scared.testmodel

import com.github.rodrigopr.scared.annotations.{Id, Index, Persist}
import scala.Array
import reflect.BeanInfo
import annotation.target.field

case class InvalidModel(
  id: Long,
  name: String
) {
  def this() = this(0, null)
}
