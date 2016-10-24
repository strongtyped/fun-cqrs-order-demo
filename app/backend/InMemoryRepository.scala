package backend

trait InMemoryRepository {

  type Model
  type Identifier

  private var store: Map[Identifier, Model] = Map()

  def find(id: Identifier): Option[Model] =
    store.get(id)

  def save(model: Model): Unit = {
    store = store + ($id(model) -> model)
  }

  def deleteById(id: Identifier): Unit =
    store = store.filterKeys(_ != id)

  def updateById(id: Identifier)(updateFunc: (Model) => Model): Option[Model] =
    find(id).map { model =>
      val updated = updateFunc(model)
      save(updated)
      updated
    }

  def findAll(predicate: Model => Boolean): List[Model] =
    store.values.filter(predicate).toList

  def fetchAll: Seq[Model] =
    store.values.toSeq

  /** Extract id from Model */
  protected def $id(model: Model): Identifier

}
