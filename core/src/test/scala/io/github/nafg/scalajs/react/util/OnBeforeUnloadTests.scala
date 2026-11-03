package io.github.nafg.scalajs.react.util

import org.scalajs.dom

class OnBeforeUnloadTests extends munit.FunSuite {
  private def leavingIsPrevented(): Boolean = {
    val event = new dom.Event("beforeunload", new dom.EventInit { cancelable = true })
    dom.window.dispatchEvent(event)
    event.defaultPrevented
  }

  test("addOnBeforeUnload prevents leaving according to the condition when the page is left") {
    var preventLeave = false
    val remove       = addOnBeforeUnload(preventLeave).runNow()

    try {
      assert(!leavingIsPrevented(), "condition false")

      preventLeave = true
      assert(leavingIsPrevented(), "condition became true after the listener was added")
    } finally remove.runNow()
  }

  test("the callback addOnBeforeUnload yields removes its listener") {
    val remove = addOnBeforeUnload(true).runNow()

    remove.runNow()
    assert(!leavingIsPrevented())
  }
}
