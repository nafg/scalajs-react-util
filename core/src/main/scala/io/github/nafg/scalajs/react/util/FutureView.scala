package io.github.nafg.scalajs.react.util

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._


abstract class FutureView extends HasBusyIndicator {
  val defaultOnFailure: Throwable => VdomNode = { throwable =>
    throwable.printStackTrace()
    <.em(^.cls := "text-muted", "Loading failed")
  }

  case class Props(future: Future[VdomNode],
                   loading: () => VdomNode = () => busyIndicator,
                   onFailure: Throwable => VdomNode = defaultOnFailure)

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

  def apply(fut: Future[VdomNode]) = component(Props(fut))

  def custom(loader: => VdomNode = busyIndicator, onFailure: Throwable => VdomNode = defaultOnFailure)
            (fut: Future[VdomNode]) =
    component(Props(fut, loading = () => loader, onFailure = onFailure))
}

object FutureView extends FutureView with HasSpinnerImage
