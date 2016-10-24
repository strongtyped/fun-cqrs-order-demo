package io.strongtyped

object Maps {
  /**
   * Updates a Map value by key
   */
  def updateByKey[K, V](map: Map[K, V], key: K)(func: V => V): Map[K, V]  = {
    map.get(key).map { value =>
      map + (key -> func(value))
    } getOrElse {
      map
    }
  }

  def addOrUpdateByKey[K, V](map: Map[K, V], key: K)(func: Option[V] => V): Map[K, V] = {
    val value = func(map.get(key))
    map + (key -> value)
  }
}
