package io.github.nafg.scalajs.react.util.partialrenderer

import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.{Callback, CustomHook, Reusability, Reusable, StateAccessor}
import io.github.nafg.scalajs.react.util.SnapshotUtils.Snapshot

import monocle.Lens

/** A value together with a function that modifies it.
  *
  * @param modifier
  *   Applies a modification to the value. Its [[japgolly.scalajs.react.Reusable]] identity is what
  *   [[Settable.reusability]] compares, so it has to change whenever the modifier's behaviour does. Wrap a function
  *   created once (stored in a `val`, or made with `useCallback`) in `Reusable.byRef`; wrap one created on each render
  *   in `Reusable.never`.
  */
case class Settable[A](value: A)(val modifier: Reusable[(A => A) => Callback]) {
  def modify(f: A => A): Callback = modifier.value(f)
  def set(a: A)                   = modify(_ => a)
  def toStateSnapshot             = Snapshot(value)(set)

  /** A `Settable` whose modifier applies `lift(f)` through this one's.
    *
    * @param key
    *   The values `lift` closes over, such as a lens. The child's modifier is reusable while this modifier and `key`
    *   are.
    */
  private[partialrenderer] def derive[B](childValue: B, key: Reusable[Any])(lift: (B => B) => A => A): Settable[B] =
    Settable(childValue)((modifier *> key).withValue((f: B => B) => modify(lift(f))))

  /** A `Settable` for the part of the value that `lens` focuses on. Its modifier is reusable while this one is and
    * `lens` is the same instance, so a lens built inline makes it never reusable.
    */
  def zoom[B](lens: Lens[A, B]): Settable[B] =
    derive(lens.get(value), Reusable.byRef(lens))(lens.modify)

  /** A `Settable` for the part of the value that `get` reads and `set` writes. Its modifier is reusable while this one
    * is and `get` and `set` are the same instances, so lambdas written inline make it never reusable.
    */
  def zoom[B](get: A => B)(set: B => A => A): Settable[B] =
    derive(get(value), Reusable.byRef(get).tuple(Reusable.byRef(set)))(f => a => set(f(get(a)))(a))

  /** A `Settable` for the value converted by `get`, converted back by `reverseGet` when modified. Its modifier is
    * reusable while this one is and `get` and `reverseGet` are the same instances, so lambdas written inline make it
    * never reusable.
    */
  def xmap[B](get: A => B)(reverseGet: B => A): Settable[B] =
    derive(get(value), Reusable.byRef(get).tuple(Reusable.byRef(reverseGet)))(f => a => reverseGet(f(get(a))))
}
object Settable                                                                {

  /** Compares the values with `Reusability[A]` and the modifiers by their `Reusable` identity. */
  implicit def reusability[A](implicit r: Reusability[A]): Reusability[Settable[A]] = {
    val m = implicitly[Reusability[Reusable[(A => A) => Callback]]]
    Reusability((x, y) => r.test(x.value, y.value) && m.test(x.modifier, y.modifier))
  }

  /** The modifier is never reusable.
    *
    * WARNING: The modifier is `state.modState`, which applies the modification to the value `state` captured, not to
    * the latest state. Calling it after the state has changed, or calling it twice (for example through two zooms of
    * the result), discards the earlier changes.
    */
  def fromStateSnapshot[A](state: StateSnapshot[A]): Settable[A] =
    Settable(state.value)(Reusable.never(state.modState))

  /** The modifier is never reusable. */
  def of[I, S](i: I)(implicit t: StateAccessor.ReadImpureWritePure[I, S]): Settable[S] =
    Settable(t.state(i))(Reusable.never(f => t(i).modState(f)))

  /** Keeps the value in component state. The modifier is the same `Reusable` for as long as the component is mounted.
    */
  def useSettable[A](initial: => A): CustomHook[Unit, Settable[A]] =
    CustomHook[Unit]
      .useReducerBy[A, A => A](reducer = _ => (state, modify) => modify(state), initialState = _ => initial)
      .buildReturning((_, reducer) =>
        Settable(reducer.value)(reducer.originalDispatch.withValue((f: A => A) => reducer.dispatch(f).value))
      )
}
