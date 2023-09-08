package io.github.nafg.scalajs.react.util

import japgolly.scalajs.react.extra.Broadcaster

/** Allows publishing a message from outside the class.
  *
  * `Broadcaster#broacast` is `protected` so it can only be called from within.
  *
  * Due to https://github.com/scala/bug/issues/12809, we can't override `broadcast` so this trait adds a public
  * alternative called `publish` instead.
  *
  * @tparam A
  *   The type of messages that can be broadcasted.
  */
trait PublicBroadcaster[A] extends Broadcaster[A] {
  def publish(a: A) = broadcast(a)
}
