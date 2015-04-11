package me.eax.akka_examples

import akka.routing.ConsistentHashingRouter.ConsistentHashable

package object commons {

  trait RoutedMsgWithId {
    val id: Long
  }

  case class RoutedMsg[T](key: T, msg: Any) extends ConsistentHashable {
    val consistentHashKey = key
  }

}
