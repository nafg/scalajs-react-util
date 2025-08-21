package io.github.nafg.scalajs.react.util.partialrenderer

import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.{Callback, CustomHook, StateAccessor}
import io.github.nafg.scalajs.react.util.SnapshotUtils.Snapshot

import monocle.Lens

case class Settable[A](value: A)(modifyFunc: (A => A) => Callback) {
  def modify(f: A => A): Callback = modifyFunc(f)
  def set(a: A)                   = modify(_ => a)
  def toStateSnapshot             = Snapshot(value)(set)

  def zoom[B](lens: Lens[A, B]): Settable[B]                =
    Settable(lens.get(value))(f => modify(lens.modify(f)))
  def zoom[B](get: A => B)(set: B => A => A): Settable[B]   =
    Settable(get(value))(f => modify(a => set(f(get(a)))(a)))
  def xmap[B](get: A => B)(reverseGet: B => A): Settable[B] =
    Settable(get(value))(f => modify(a => reverseGet(f(get(a)))))
}
object Settable                                                    {
  def fromStateSnapshot[A](state: StateSnapshot[A]): Settable[A]                       =
    Settable(state.value)(state.modState)
  def of[I, S](i: I)(implicit t: StateAccessor.ReadImpureWritePure[I, S]): Settable[S] =
    Settable(t.state(i))(f => t(i).modState(f))

  def useSettable[A](initial: => A): CustomHook[Unit, Settable[A]] =
    CustomHook[Unit]
      .useReducerBy[A, A => A](reducer = _ => (state, modify) => modify(state), initialState = _ => initial)
      .buildReturning((_, reducer) => Settable(reducer.value)(reducer.dispatch))
}
