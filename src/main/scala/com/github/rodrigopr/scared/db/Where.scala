package com.github.rodrigopr.scared.db

import collection.mutable

/**
 * Where clause for the index lookup, the proper index will be identified from the fields usage.
 *
 * ==List attribute==
 * In case a attribute is a List, each element in it will be mapped to a index entry,
 * So comparing a attribute to List will need to create a join in N indexEntries, avoid if possible. <br>
 *
 * eg:
 * <pre>
 *  new Where { 'mList === List(1,2) }
 * </pre>
 *  This will get elements that have both 1 and 2 on the mList, but they may contain other elements on it. <br>
 *  This happen cause the default join method uses INTERSECTION,
 *  if changed in the query to UNION it would return items that have any of those elements. <br>
 *
 * <br>
 * In most cast you actually want to use:
 * <pre>
 *  new Where { 'mList === 2 }
 * </pre>
 *
 *  This will get elements that have 2 on the attribute mList
 */
trait Where { self =>
  protected[db] implicit def convToCondition(field: String): Condition = new Condition(field)(self)
  protected[db] implicit def convToCondition(field: Symbol): Condition = new Condition(field.name)(self)

  protected class Condition(field: String)(implicit where: Where) {
    def ===(value: Any): Boolean = {
      where.addEquals(field, value)
      true
    }
  }

  private[db] val clauses = mutable.HashMap[String, Any]()

  def addEquals(field: String, value: Any): Where = {
    clauses.put(field, value)

    this
  }

  protected[db] def getClauses = clauses.toMap
}

object Where {
  def apply(field: String, value: Any): Where = new Where{}.addEquals(field, value)
}
