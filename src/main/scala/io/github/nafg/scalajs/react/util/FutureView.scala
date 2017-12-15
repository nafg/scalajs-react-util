package io.github.nafg.scalajs.react.util

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success, Try}

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._


object FutureView {
  val component =
    ScalaComponent.builder[Future[VdomElement]]("FutureView")
      .initialState(Option.empty[Try[VdomElement]])
      .render_S {
        case None                     => <.div("Loading...")
        case Some(Success(elem))      => elem
        case Some(Failure(throwable)) =>
          throwable.printStackTrace()
          <.em(^.cls := "text-muted", "Loading failed")
      }
      .configure(AsyncStateFromProps.constAlways((_, props) => props.transform(attempt => Success(Some(attempt)))))
      .build

  def apply(fut: Future[VdomElement]) = component(fut)
}
