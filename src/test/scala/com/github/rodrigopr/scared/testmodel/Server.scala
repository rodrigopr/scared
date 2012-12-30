package com.github.rodrigopr.scared.testmodel

import com.github.rodrigopr.scared.annotations.{ Index, Persist}
import scala.Array

@Persist(
  name = "server",
  customIndexes = Array(
    new Index(fields = Array("name")),
    new Index(fields = Array("group")),
    new Index(fields = Array("enviroment")),
    new Index(fields = Array("enviroment", "group")),
    new Index(fields = Array("roles"))
  )
)
case class Server(
  id: Long,

  name: String,
  ip: String,
  fqdn: String,

  group: String,
  enviroment: String,
  roles: List[String]
)
