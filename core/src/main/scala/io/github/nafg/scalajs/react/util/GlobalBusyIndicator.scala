package io.github.nafg.scalajs.react.util

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Success

import japgolly.scalajs.react.extra.{Broadcaster, Listenable, OnUnmount}
import japgolly.scalajs.react.vdom.html_<^.*
import japgolly.scalajs.react.{Callback, ScalaComponent}


abstract class GlobalBusyIndicator extends HasBusyIndicator {
  private object broadcaster extends Broadcaster[Future[Any]] {
    override def broadcast(a: Future[Any]) = super.broadcast(a)
  }

  def showBusy[A](f: Future[A]): f.type = {
    broadcaster.broadcast(f).runNow()
    f
  }

  object Implicits {
    implicit class FutureBusyIndicatorExtensionMethods[A](self: Future[A]) {
      def showBusy: Future[A] = GlobalBusyIndicator.this.showBusy(self)
    }
  }

  def positionMods = TagMod(
    ^.position.fixed,
    ^.left := "50%", ^.top := "35px",
    ^.marginBottom := "0px", ^.marginLeft := "-16px",
    ^.zIndex := "2000"
  )

  def render(futures: Seq[Future[Any]]) =
    busyIndicator(positionMods)(
      ^.visibility.hidden when futures.forall(_.isCompleted)
    )

  val component =
    ScalaComponent.builder[Unit]("BusyIndicator")
      .initialState(Seq.empty[Future[Any]])
      .backend(_ => OnUnmount())
      .render_S(render)
      .configure(Listenable.listen(_ => broadcaster, { self =>
        (fut: Future[Any]) =>
          self.modState(fut +: _) >>
            Callback.future(fut.transform(_ => Success(self.modState(_.filter(_ ne fut))))).void
      }))
      .build
}

object GlobalBusyIndicator extends GlobalBusyIndicator with HasSpinnerImage
