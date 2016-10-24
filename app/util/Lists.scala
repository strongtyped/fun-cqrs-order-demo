package util

object Lists {
  def removeFirst[A](list: List[A])(pred: A => Boolean): List[A] = {
    val index = list.indexWhere(pred)
    if (index >= 0) {
      val (l1, l2) = list.splitAt(index)
      l1 ::: l2.tail
    } else {
      list
    }
  }

}
