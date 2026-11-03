package io.github.nafg.scalajs.react

import scala.concurrent.Future
import scala.scalajs.js

import org.scalajs.dom
import org.scalajs.dom.BeforeUnloadEvent
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global
import japgolly.scalajs.react.callback.{Callback, CallbackTo}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.{AsyncCallback, React}

package object util {

  /** Adds a `beforeunload` listener that asks the browser to confirm leaving the page whenever `preventLeaveCondition`
    * is true at the moment the page is being left.
    *
    * @return
    *   a callback that adds the listener and yields a callback that removes it
    */
  def addOnBeforeUnload(preventLeaveCondition: => Boolean): CallbackTo[Callback] =
    CallbackTo {
      val listener: js.Function1[BeforeUnloadEvent, Boolean] = { (evt: BeforeUnloadEvent) =>
        if (!preventLeaveCondition) false
        else {
          evt.preventDefault()
          evt.asInstanceOf[js.Dynamic].returnValue = ""
          true
        }
      }
      dom.window.addEventListener("beforeunload", listener)
      Callback(dom.window.removeEventListener("beforeunload", listener))
    }

  def suspendFuture[A](f: => Future[A])(render: A => VdomElement)(implicit
    loadingIndicator: LoadingIndicator = LoadingIndicator.spinner16
  ) =
    React.Suspense(loadingIndicator.render, AsyncCallback.fromFuture(f))(render, implicitly)
}
