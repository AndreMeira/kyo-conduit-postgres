package playground.batching

case class Batched[+A](elements: List[Batched.Element[A]]) {
  def isEmpty: Boolean = elements.isEmpty

  def reverse: Batched[A] = Batched(elements.reverse)

  def indices: List[Int] = elements.map(_.index)

  def values: List[A] = elements.map(_.value)

  def map[B](fn: A => B): Batched[B] =
    Batched(elements.map(element => element.map(fn)))

  def sortedBy[B: Ordering](fn: A => B): Batched[A] =
    Batched(elements.sortBy(element => fn(element.value)))

  def distinctBy[B](fn: A => B): Batched[A] =
    Batched(elements.distinctBy(element => fn(element.value)))

  def filterByIndex(fn: Int => Boolean): Batched[A] =
    Batched(elements.filter(element => fn(element.index)))

  def zip[B](other: Batched[B]): Batched[(A, B)] = {
    val mapping = other.elements.map(elem => elem.index -> elem.value).toMap
    val zipped  = elements.map(elem => elem.map(value => mapping.get(elem.index).map(value -> _)))
    Batched(zipped).collect { case Some(a -> b) => a -> b }
  }

  def collect[B](fn: PartialFunction[A, B]): Batched[B] =
    Batched {
      for
        element <- elements
        value   <- fn.lift(element.value)
      yield element.copy(value = value)
    }

  def partitionMap[L, R](fn: A => Either[L, R]): (Batched[L], Batched[R]) = {
    val (left, right) = elements.partitionMap: element =>
      fn(element.value) match
        case Right(value) => Right(element.copy(value = value))
        case Left(value)  => Left(element.copy(value = value))
    Batched(left) -> Batched(right)
  }

  def distinctBySequence[B](fn: A => B): List[Batched[A]] =
    if isEmpty then Nil
    else {
      val distinct = distinctBy(fn)
      distinct :: (this -- distinct).distinctBySequence(fn)
    }

  def --(other: Batched[?]): Batched[A] = {
    val indices = other.indices.toSet
    filterByIndex(i => !indices.contains(i))
  }

  def <<[B >: A](other: Batched[B]): Batched[B] = {
    val diff = this -- other
    Batched(diff.elements ++ other.elements)
  }
}

object Batched:
  case class Element[+A](value: A, index: Int):
    def map[B](fn: A => B): Element[B] = Element(fn(value), index)

  def empty[A]: Batched[A] = Batched(List.empty)

  def fromSeq[A](values: Seq[A]): Batched[A] =
    Batched(values.toList.zipWithIndex.map(Element(_, _)))
