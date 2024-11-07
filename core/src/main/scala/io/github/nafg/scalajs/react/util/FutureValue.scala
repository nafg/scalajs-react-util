package io.github.nafg.scalajs.react.util

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.vdom.html_<^.*
import japgolly.scalajs.react.{AsyncCallback, Reusability}

case class FutureValue[A](value: Option[Try[A]] = None) {
  def fold[B](
    ifSuccess: A => B,
    ifFailure: Throwable => B = (throwable: Throwable) => <.div(throwable.getMessage),
    ifPending: => B = LoadingIndicator.spinner16.render
  ): B =
    value match {
      case None                     => ifPending
      case Some(Success(a))         => ifSuccess(a)
      case Some(Failure(throwable)) => ifFailure(throwable)
    }

  def getOrElse[B >: A](default: => B): B =
    fold(identity, _ => default, default)
}

object FutureValue {
  private implicit val reuseThrowable: Reusability[Throwable]                = Reusability.by_==
  private implicit def reuseTry[A: Reusability]: Reusability[Try[A]]         = Reusability.by(_.toEither)
  implicit def reuseFutureValue[A: Reusability]: Reusability[FutureValue[A]] =
    Reusability.by(_.value)

  def useQuery[D: Reusability, A](dep: D)(query: D => Future[A]) =
    CustomHook[Unit]
      .useState(FutureValue[A]())
      .useEffectWithDepsBy((_, _) => dep) { (_, state) => dep =>
        AsyncCallback
          .fromFuture(query(dep))
          .attemptTry
          .flatMapSync(result => state.modState(_.copy(value = Some(result))))
      }
      .buildReturning((_, state) => state.value)
}
