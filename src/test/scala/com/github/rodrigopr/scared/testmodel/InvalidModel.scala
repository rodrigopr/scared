package com.github.rodrigopr.scared.testmodel


case class InvalidModel(
  id: Long,
  name: String
) {
  def this() = this(0, null)
}
