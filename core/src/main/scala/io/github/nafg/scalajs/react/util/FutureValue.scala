package io.github.nafg.scalajs.react.util

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global
import japgolly.scalajs.react.hooks.CustomHook
import japgolly.scalajs.react.hooks.Hooks
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
  implicit def reuseFutureValue[A: Reusability]: Reusability[FutureValue[A]] = Reusability.by(_.value)

  private def make[A](state: Hooks.UseState[FutureValue[A]], future: => Future[A]): AsyncCallback[Unit] =
    AsyncCallback
      .fromFuture(future)
      .attemptTry
      .flatMapSync(result => state.modState(_.copy(value = Some(result))))

  def useQuery[D: Reusability, A](dep: D)(query: D => Future[A]) =
    CustomHook[Unit]
      .useState(FutureValue[A]())
      .useEffectWithDepsBy((_, _) => dep) { (_, state) => dep =>
        make(state, query(dep))
      }
      .buildReturning((_, state) => state.value)

  /** A custom hook that executes a query based on a dependency and manages the result as a `FutureValue`. The result of
    * the query is kept in the hook's state and updated whenever the dependency changes.
    *
    * Unlike [[useQuery]], whenever the dependency changes, the FutureValue is reset to None (pending) until the new
    * query execution completes.
    *
    * @param dep
    *   A dependency value of type `D` that is tracked for changes.
    * @param query
    *   A function that takes a dependency of type `D` and returns a `Future[A]`.
    */
  def useQueryForgetful[D: Reusability, A](dep: D)(query: D => Future[A]) =
    CustomHook[Unit]
      .useState(FutureValue[A](None))
      .useEffectWithDepsBy((_, _) => dep) { case ((), state) =>
        dep =>
          state.setState(FutureValue(None)).asAsyncCallback >>
            make(state, query(dep))
      }
      .buildReturning((_, state) => state.value)
}
