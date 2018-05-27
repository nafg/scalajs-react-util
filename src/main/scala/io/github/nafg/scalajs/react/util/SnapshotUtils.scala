package io.github.nafg.scalajs.react.util

import scala.concurrent.{ExecutionContext, Future}

import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CallbackOption, CallbackTo, ReactEventFromInput}


object SnapshotUtils {
  def Snapshot[A](initialValue: A)(f: A => Callback) =
    StateSnapshot(initialValue)((option, cb) => option.fold(cb)(f(_) >> cb))

  implicit class SnapshotExtensionMethods[A](self: StateSnapshot[A]) {
    def andThen(f: A => Callback): StateSnapshot[A] =
      Snapshot(self.value) { a =>
        self.setState(a) >> f(a)
      }

    def withHandler(f: (A, A) => Callback) =
      Snapshot(self.value) { value =>
        self.setState(value) >>
          f(self.value, value)
      }
  }

  implicit class BooleanSnapshotExtensionMethods(self: StateSnapshot[Boolean]) {
    def toTagMod =
      TagMod(
        ^.checked := self.value,
        ^.onChange ==> { event: ReactEventFromInput =>
          val checked = event.target.checked
          self.setState(checked)
        }
      )
  }

  implicit class StringSnapshotExtensionMethods(self: StateSnapshot[String]) {
    def toTagMod =
      TagMod(
        ^.value := self.value,
        ^.onChange ==> { event: ReactEventFromInput =>
          val value = event.target.value
          self.setState(value)
        }
      )
  }

  implicit class OptionSnapshotExtensionMethods[A](self: StateSnapshot[Option[A]]) {
    def zoomDefinedFut(create: => Future[A])
                      (delete: A => CallbackOption[Future[Unit]] = _ => CallbackTo(Future.unit).toCBO)
                      (implicit executionContext: ExecutionContext): StateSnapshot[Boolean] =
      Snapshot(self.value.isDefined) { checked =>
        self.value match {
          case None if checked     =>
            Callback.future(create.map(a => self.setState(Some(a)))).void
          case Some(a) if !checked =>
            delete(a).flatMap(fut => Callback.future(fut.map(_ => self.setState(None))))
          case _                   => Callback.empty
        }
      }

    def zoomDefined(create: => A)(implicit executionContext: ExecutionContext): StateSnapshot[Boolean] =
      zoomDefinedFut {
        val value = create
        Future.successful(value)
      }()

    def sequenceOpt: Option[StateSnapshot[A]] =
      self.value.map { a =>
        Snapshot(a)(a => self.setState(Some(a)))
      }
  }
}
