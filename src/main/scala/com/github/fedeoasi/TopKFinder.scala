package com.github.fedeoasi

class TopKFinder[T](seq: Seq[T]) {
  def top(k: Int)(implicit ordering: Ordering[T]): Seq[T] = {
    val minHeap = collection.mutable.PriorityQueue[T](seq.take(k): _*)(ordering.reverse)
    seq.drop(k).foreach { v =>
      minHeap += v
      minHeap.dequeue()
    }
    minHeap.dequeueAll.reverse
  }
}
