package io.github.nafg.scalajs.react.util.partialrenderer

import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.{Callback, Reusability, Reusable}

import monocle.{Focus, Lens}

class SettableReusabilityTests extends munit.FunSuite {
  private case class Pair(left: Int, right: Int)
  private implicit val reusabilityPair: Reusability[Pair] = Reusability.by_==

  private val leftLens: Lens[Pair, Int]  = Focus[Pair](_.left)
  private val rightLens: Lens[Pair, Int] = Focus[Pair](_.right)

  /** A root whose modifier is created once and applies to the latest value, as React's updater-form calls do */
  private class Root(initial: Pair) {
    var value: Pair = initial

    private val modifier = Reusable.byRef((f: Pair => Pair) => Callback { value = f(value) })

    def settable: Settable[Pair] = Settable(value)(modifier)
  }

  private def reusable[A](x: A, y: A)(implicit r: Reusability[A]) = r.test(x, y)

  test("equal values with different modifiers are not reusable") {
    val f1: (Int => Int) => Callback = _ => Callback.empty
    val f2: (Int => Int) => Callback = _ => Callback.empty
    assert(reusable(Settable(1)(Reusable.byRef(f1)), Settable(1)(Reusable.byRef(f1))))
    assert(!reusable(Settable(1)(Reusable.byRef(f1)), Settable(1)(Reusable.byRef(f2))))
    assert(!reusable(Settable(1)(Reusable.byRef(f1)), Settable(2)(Reusable.byRef(f1))))
  }

  test("an old zoomed modifier applies to the latest state") {
    val root = new Root(Pair(1, 2))
    val old  = root.settable

    old.zoom(leftLens).set(10).runNow()
    old.zoom(rightLens).set(20).runNow()
    assertEquals(root.value, Pair(10, 20))

    root.settable.zoom(leftLens).modify(_ + 1).runNow()
    old.zoom(rightLens).modify(_ + 1).runNow()
    assertEquals(root.value, Pair(11, 21))
  }

  test("composed zooms") {
    case class Outer(pair: Pair, other: String)
    val pairLens = Focus[Outer](_.pair)
    var value    = Outer(Pair(1, 2), "x")
    val modifier = Reusable.byRef((f: Outer => Outer) => Callback { value = f(value) })
    def settable = Settable(value)(modifier)

    val old = settable.zoom(pairLens).zoom(rightLens)
    assert(reusable(old, settable.zoom(pairLens).zoom(rightLens)))

    settable.zoom(pairLens).zoom(leftLens).set(5).runNow()
    old.set(6).runNow()
    assertEquals(value, Outer(Pair(5, 6), "x"))
    assert(!reusable(old, settable.zoom(pairLens).zoom(rightLens)), "the value changed")
    assert(reusable(old.modifier, settable.zoom(pairLens).zoom(rightLens).modifier), "the modifier is still the same")

    val otherModifier = Reusable.byRef((f: Outer => Outer) => Callback { value = f(value) })
    assert(
      !reusable(settable.zoom(pairLens).zoom(rightLens), Settable(value)(otherModifier).zoom(pairLens).zoom(rightLens)),
      "the root modifier differs"
    )
  }

  test("stable versus inline lenses") {
    val root = new Root(Pair(1, 2))
    assert(reusable(root.settable.zoom(leftLens), root.settable.zoom(leftLens)))
    assert(!reusable(root.settable.zoom(Focus[Pair](_.left)), root.settable.zoom(Focus[Pair](_.left))))
    assert(!reusable(root.settable.zoom(leftLens), root.settable.zoom(rightLens)))
  }

  test("stable versus inline mappings") {
    val root                            = new Root(Pair(1, 2))
    val get: Pair => Int                = _.left
    val set: Int => Pair => Pair        = l => _.copy(left = l)
    val toTuple: Pair => (Int, Int)     = p => (p.left, p.right)
    val fromTuple: ((Int, Int)) => Pair = { case (l, r) => Pair(l, r) }

    assert(reusable(root.settable.zoom(get)(set), root.settable.zoom(get)(set)))
    assert(!reusable(root.settable.zoom(get)(set), root.settable.zoom(get)(l => _.copy(left = l))))
    assert(reusable(root.settable.xmap(toTuple)(fromTuple), root.settable.xmap(toTuple)(fromTuple)))
    assert(!reusable(root.settable.xmap(toTuple)(fromTuple), root.settable.xmap(toTuple)(t => Pair(t._1, t._2))))

    root.settable.zoom(get)(set).set(7).runNow()
    root.settable.xmap(toTuple)(fromTuple).modify { case (l, r) => (l, r + l) }.runNow()
    assertEquals(root.value, Pair(7, 9))
  }

  test("a Settable from a StateSnapshot is never reusable and modifies the snapshot's captured value") {
    var value    = Pair(1, 2)
    val snapshot =
      StateSnapshot(value)((next: Option[Pair], cb: Callback) => Callback { next.foreach(value = _) } >> cb)

    val settable = Settable.fromStateSnapshot(snapshot)
    assert(!reusable(settable, settable))

    settable.zoom(leftLens).set(10).runNow()
    settable.zoom(rightLens).set(20).runNow()
    assertEquals(value, Pair(1, 20), "the second change rebuilt the pair from the captured value")
  }
}
