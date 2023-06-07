package io.github.nafg.scalajs.react.util.partialrenderer

import japgolly.scalajs.react.Callback
import io.github.nafg.scalajs.react.util.SnapshotUtils.Snapshot

import monocle.Lens


case class Settable[A](value: A)(val modify: (A => A) => Callback) {
  def set(a: A) = modify(_ => a)
  def toStateSnapshot = Snapshot(value)(set)

  def zoom[B](lens: Lens[A, B]): Settable[B] =
    Settable(lens.get(value))(f => modify(lens.modify(f)))
  def zoom[B](get: A => B)(set: B => A => A): Settable[B] =
    Settable(get(value))(f => modify(a => set(f(get(a)))(a)))
  def xmap[B](get: A => B)(reverseGet: B => A): Settable[B] =
    Settable(get(value))(f => modify(a => reverseGet(f(get(a)))))
}
