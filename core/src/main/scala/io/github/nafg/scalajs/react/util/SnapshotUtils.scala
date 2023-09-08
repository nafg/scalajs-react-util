package io.github.nafg.scalajs.react.util

import scala.concurrent.{ExecutionContext, Future}

import org.scalajs.dom.html
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.vdom.html_<^.*
import japgolly.scalajs.react.{Callback, CallbackOption, CallbackTo, ReactEventFromInput}

import monocle.Optional

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
      extends AnyVal
      with HasToTagMod[String] {
    override protected def setAttr  = ^.value := _
    override protected def property = _.value
  }

  implicit class BooleanSnapshotExtensionMethods(override protected val snapshot: StateSnapshot[Boolean])
      extends AnyVal
      with HasToTagMod[Boolean] {
    override protected def setAttr  = ^.checked := _
    override protected def property = _.checked
  }

  implicit class OptionSnapshotExtensionMethods[A](self: StateSnapshot[Option[A]]) {
    def zoomDefinedFut(create: => Future[A])(
      delete: A => CallbackOption[Future[Unit]] = _ => CallbackTo(Future.unit).toCBO
    )(implicit executionContext: ExecutionContext): StateSnapshot[Boolean] =
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

  implicit class Snapshot_sequenceEither[A, B](self: StateSnapshot[Either[A, B]]) {
    def sequenceEither: Either[StateSnapshot[A], StateSnapshot[B]] =
      self.value match {
        case Left(value)  => Left(Snapshot(value)(value => self.setState(Left(value))))
        case Right(value) => Right(Snapshot(value)(value => self.setState(Right(value))))
      }
  }

  implicit class Snapshot_split[A, B](self: StateSnapshot[(A, B)]) {
    def split: (StateSnapshot[A], StateSnapshot[B]) =
      Snapshot(self.value._1)(a => self.modState(_.copy(_1 = a))) ->
        Snapshot(self.value._2)(b => self.modState(_.copy(_2 = b)))
  }

  implicit class Snapshot_traverseOptional[A](self: StateSnapshot[A]) {
    def traverseOptional[B](optional: Optional[A, B]): Option[StateSnapshot[B]] =
      optional
        .getOption(self.value)
        .map(Snapshot(_)(b => self.modState(optional.replace(b))))
  }

  case class StateSnapshotElem[A](key: Int, state: StateSnapshot[A], delete: Callback)

  implicit class SeqSnapshotExtensionMethods[A](self: StateSnapshot[Seq[A]]) {
    def stateSnapshotElements: Seq[StateSnapshotElem[A]] =
      self.value.zipWithIndex.map { case (a, n) =>
        StateSnapshotElem(
          n,
          Snapshot(a)(a2 => self.modState(_.patch(n, List(a2), 1))),
          self.modState(_.patch(n, Nil, 1))
        )
      }

    def insertAt(index: Int, value: A) =
      self.modState(_.patch(index, Some(value), 0))
  }
}
