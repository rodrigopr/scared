package com.github.rodrigopr.scared.testmodel

import com.github.rodrigopr.scared.annotations.{Index, Persist}
import scala.Array

@Persist(name = "nullable")
case class ModelNullableId(id: java.lang.Long, name: String)
