package com.github.rodrigopr.scared.testmodel

import com.github.rodrigopr.scared.annotations._
import scala.Array
import java.util.Date
import annotation.target.field

@Persist(
  name = "occurrence",
  customIndexes = Array(
    new Index(fields = Array("name")),
    new Index(fields = Array("status"), orderField = "date"),
    new Index(fields = Array("status", "services"), orderField = "date"),
    new Index(fields = Array("status", "servers"), orderField = "date"),
    new Index(fields = Array("status", "related"), orderField = "date")
  )
)
case class Occurrence(
  @(Id @field) id: Long,
  name: String,
  date: Date,
  status: String,
  services: List[Long],
  servers: List[Long],
  related: List[Long]
)
