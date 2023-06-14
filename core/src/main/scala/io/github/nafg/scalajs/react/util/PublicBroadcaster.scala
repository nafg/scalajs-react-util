package io.github.nafg.scalajs.react.util

import japgolly.scalajs.react.extra.Broadcaster


trait PublicBroadcaster[A] extends Broadcaster[A] {
  // overriding to make it public
  override def broadcast(a: A) = super.broadcast(a)
}
