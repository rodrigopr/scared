package com.github.rodrigopr.scared.db

import com.github.rodrigopr.scared.mapping.{Field, Model}
import collection.mutable
import collection.mutable.ArrayBuffer
import com.redis.RedisClient
import com.redis.serialization.{Format, Parse}
import com.github.rodrigopr.scared.serializer.SerDe

class RedisContext(serde: SerDe = SerDe.defaultSerializer) {
  private[db] val modelsInfo = new mutable.HashMap[Class[_], Model]()

  // TODO: use a pool and load host:port from configuration
  private[db] val redis = new RedisClient()

  def nextIdFor[T](implicit m: Manifest[T]): Long = {
    val modelInfo = extractModelInfo(m.erasure)

    redis.incr("#" + modelInfo.name).get
  }

  /**
   * Returns the saved id, in case it was auto-generated.
   *
   * Save `model` and update his corresponding indexes. <br>
   * The model need be annotated with [[com.github.rodrigopr.scared.annotations.Persist]]
   */
  def save[T <: AnyRef](model: T)(implicit m: Manifest[T]): Any = {
    val modelInfo = extractModelInfo(model.getClass)

    val data = toMap(model)

    val idField: Field = modelInfo.idField

    val id = Option(data(idField.name)).getOrElse {
      if(modelInfo.idField.canAutoGenerate) {
        val newId = nextIdFor[T]

        // update the attribute in the main object
        idField.field.set(model, newId)

        newId
      } else {
        sys.error("Id attribute must not be null")
      }
    }

    val oldData = load[T](id).map(toMap)

    val indexData = modelInfo.indexes.map { i => (oldData.map(i.genIndexEntry), i.genIndexEntry(data))}

    val toRemove = ArrayBuffer[String]()
    val toAdd = ArrayBuffer[IndexEntry]()

    indexData.foreach { case(oldIndexesOpt, newIndexes) =>
      oldIndexesOpt map { oldIndexes =>
        toRemove ++= oldIndexes.filterNot(newIndexes contains).map(_.key)
        toAdd ++= newIndexes.filterNot(oldIndexes contains)
      } getOrElse {
        toAdd ++= newIndexes
      }
    }

    redis.pipeline {p =>
      p.set(modelInfo.getObjectKey(id), serde.serialize[T](model))

      toRemove.foreach(p.zrem(_, id))

      toAdd.foreach { entry =>
        p.zadd(entry.key, entry.score, id.toString)
      }
    }

    if(idField.canAutoGenerate) {
      redis.get("#" + modelInfo.name).map(_.toLong).map{ oldValue =>
        val idAsLong = id.asInstanceOf[Long]

        if(idAsLong > oldValue) {
          redis.set("#" + modelInfo.name, idAsLong)
        }
      }
    }

    id
  }

  /**
   * Load a model using `id` and update it applying `updateFunc`, returns the updated object.
   * Return None if the object don't exists.
   */
  def update[T <: AnyRef](id: Any, updateFunc: T => T)(implicit m: Manifest[T]): Option[T] = {
    load[T](id).map(updateFunc).map{ modified =>
      save[T](modified)
      modified
    }
  }

  /**
   * Deletes the `model` and all references to it. <br>
   * The model need be annotated with [[com.github.rodrigopr.scared.annotations.Persist]]
   */
  def deleteModel[T <: AnyRef](model: T)(implicit m: Manifest[T]) {
    val modelInfo = extractModelInfo(model.getClass)

    val data = toMap(model)
    val id = data(modelInfo.idField.name)

    delete[T](id)
  }

  /**
   * Deletes the model associated with the `id`. <br>
   * The model need be annotated with [[com.github.rodrigopr.scared.annotations.Persist]]
   */
  def delete[T <: AnyRef](id: Any)(implicit m: Manifest[T]) {
    val modelInfo = extractModelInfo(m.erasure)

    val oldData = load[T](id).map(toMap)

    val toRemove = oldData.map(data =>
      modelInfo.indexes.map { i => i.genIndexEntry(data) }.toList.flatten
    ).getOrElse(List())

    redis.pipeline {p =>
      p.del(modelInfo.getObjectKey(id))

      toRemove.foreach(e => p.zrem(e.key, id))
    }
  }

  /**
   * Returns the load model if founded or None if it don't exist
   */
  def load[T](id: Any)(implicit m: Manifest[T]): Option[T] = {
    val modelInfo = extractModelInfo(m.erasure)

    redis.get(modelInfo.getObjectKey(id))(format = Format.default, parse = Parse.Implicits.parseByteArray).map(d =>
      serde.deserialize[T](d)
    )
  }

  /**
   * Query one or more indexes
   *
   * @see com.github.rodrigopr.scared.db.Where
   * @see com.github.rodrigopr.scared.db.Queryable
   */
  def query[T](where: Where)(implicit m: Manifest[T]) = new Queryable(this, where, extractModelInfo(m.erasure))

  private def extractModelInfo(clazz: Class[_]) = modelsInfo.getOrElseUpdate(clazz, new Model(clazz))

  private def toMap(model: AnyRef) =
    (Map[String, Any]() /: model.getClass.getDeclaredFields) { (a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(model))
    }
}
