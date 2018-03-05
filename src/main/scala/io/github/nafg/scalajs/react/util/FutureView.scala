package io.github.nafg.scalajs.react.util

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._


class FutureView extends HasSpinnerImage {
  val defaultOnFailure: Throwable => VdomElement = { throwable =>
    throwable.printStackTrace()
    <.em(^.cls := "text-muted", "Loading failed")
  }

  case class Props(future: Future[VdomElement],
                   loading: () => VdomElement = () => busyIndicator,
                   onFailure: Throwable => VdomElement = defaultOnFailure)

  val component =
    ScalaComponent.builder[Props]("FutureView")
      .initialStateFromProps(_.future.value)
      .render { self =>
        self.state match {
          case None                     => self.props.loading()
          case Some(Failure(throwable)) => self.props.onFailure(throwable)
          case Some(Success(elem))      => elem
        }
      }
      .configure(AsyncStateFromProps.constAlways((_, props) => props.future.transform(attempt => Success(Some(attempt)))))
      .build

  def apply(fut: Future[VdomElement]) = component(Props(fut))

  def custom(loader: => VdomElement = busyIndicator, onFailure: Throwable => VdomElement = defaultOnFailure)
            (fut: Future[VdomElement]) =
    component(Props(fut, loading = () => loader))
}

object FutureView extends FutureView with HasSpinnerImage
