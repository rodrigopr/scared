package com.github.rodrigopr.scared.testmodel

import com.github.rodrigopr.scared.annotations.{Id, Index, Persist}
import scala.Array
import annotation.target.field

@Persist(
  name = "simple",
  customIndexes = Array(
    new Index(fields = Array("name"))
  )
)
case class Simple(
  @(Id @field) id: Long,

  name: String
)
