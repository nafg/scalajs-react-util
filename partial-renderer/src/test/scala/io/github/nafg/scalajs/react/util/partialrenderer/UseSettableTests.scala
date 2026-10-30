package io.github.nafg.scalajs.react.util.partialrenderer

import scala.collection.mutable

import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.vdom.html_<^.*
import japgolly.scalajs.react.{Reusability, ScalaFnComponent}

import monocle.Focus

class UseSettableTests extends munit.FunSuite {
  private case class Pair(left: Int, right: Int)
  private implicit val reusabilityPair: Reusability[Pair] = Reusability.by_==

  private val leftLens  = Focus[Pair](_.left)
  private val rightLens = Focus[Pair](_.right)

  private def reusable[A](x: A, y: A)(implicit r: Reusability[A]) = r.test(x, y)

  test("useSettable's modifier keeps its identity across renders and applies to the latest state") {
    val rendered = mutable.Buffer.empty[Settable[Pair]]

    val component =
      ScalaFnComponent
        .withHooks[Int]
        .custom(Settable.useSettable(Pair(1, 2)))
        .render { (props, settable) =>
          rendered += settable
          <.div(props, settable.value.toString)
        }

    ReactTestUtils.withReactRootSync { root =>
      root.renderSync(component(1))
      root.renderSync(component(2))
      assertEquals(rendered.size, 2)
      val first = rendered.head
      assert(reusable(first, rendered(1)), "a re-render with the same state")
      assert(reusable(first.zoom(leftLens), rendered(1).zoom(leftLens)))

      root.actSync(first.zoom(leftLens).set(10).runNow())
      root.actSync(first.zoom(rightLens).set(20).runNow())
      assertEquals(rendered.last.value, Pair(10, 20))
      assert(!reusable(first, rendered.last), "the value changed")
      assert(reusable(first.modifier, rendered.last.modifier), "the modifier is the same")
    }
  }
}
