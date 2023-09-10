package io.github.nafg.scalajs.react

import scala.concurrent.Future
import scala.scalajs.js

import org.scalajs.dom
import org.scalajs.dom.BeforeUnloadEvent
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global
import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.{AsyncCallback, React}

package object util {
  def addOnBeforeUnload(preventLeaveCondition: => Boolean) =
    Callback {
      dom.window.addEventListener[BeforeUnloadEvent](
        "beforeunload",
        { (evt: BeforeUnloadEvent) =>
          if (!preventLeaveCondition) false
          else {
            evt.preventDefault()
            evt.asInstanceOf[js.Dynamic].returnValue = ""
            true
          }
        }
      )
    }

  def suspendFuture[A](f: => Future[A])(render: A => VdomElement)(implicit
    loadingIndicator: LoadingIndicator = LoadingIndicator.spinner16
  ) =
    React.Suspense(loadingIndicator.render, AsyncCallback.fromFuture(f))(render, implicitly)
}
