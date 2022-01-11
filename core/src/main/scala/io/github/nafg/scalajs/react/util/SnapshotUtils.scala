package io.github.nafg.scalajs.react.util

import scala.concurrent.{ExecutionContext, Future}

import org.scalajs.dom.html
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.vdom.html_<^.*
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

  trait HasToTagMod[A] extends Any {
    protected def snapshot: StateSnapshot[A]
    protected def setAttr: A => TagMod
    protected def property: html.Input => A

    def toTagMod =
      TagMod(
        setAttr(snapshot.value),
        ^.onChange ==> { (event: ReactEventFromInput) =>
          val value = property(event.target)
          snapshot.setState(value)
        }
      )
  }

  implicit class StringSnapshotExtensionMethods(override protected val snapshot: StateSnapshot[String])
    extends AnyVal with HasToTagMod[String] {
    override protected def setAttr = ^.value := _
    override protected def property = _.value
  }

  implicit class BooleanSnapshotExtensionMethods(override protected val snapshot: StateSnapshot[Boolean])
    extends AnyVal with HasToTagMod[Boolean] {
    override protected def setAttr = ^.checked := _
    override protected def property = _.checked
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
