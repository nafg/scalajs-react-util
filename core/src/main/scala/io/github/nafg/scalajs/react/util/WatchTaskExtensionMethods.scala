package io.github.nafg.scalajs.react.util

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global
import japgolly.scalajs.react.vdom.html_<^.*
import japgolly.scalajs.react.{AsyncCallback, Callback}
import io.github.nafg.scalajs.react.util.confirm.ConfirmCanceled


class WatchTaskExtensionMethods(busyIndicator: GlobalBusyIndicator, messages: Messages) {
  private def notifyFailure[A](future: => Future[A]) =
    future.andThen {
      case Failure(exception) if exception != ConfirmCanceled =>
        messages.postErrorCB(messages.defaultErrorMessage(exception)).runNow()
    }
  private def notifyResult[A](future: => Future[A]) =
    notifyFailure(future.andThen { case Success(_) => messages.postSuccessCB("Done!").runNow() })

  trait ExtensionMethods[F[_], A] {
    protected def toCB(f: => F[Callback]): Callback
    protected def toACB: F[A] => AsyncCallback[A]

    def watch: F[A]
    /**
     * Wrap `fut` as a `Callback`, with a busy indicator and failure notification message
     */
    final def watchCB(implicit ev: A =:= Callback): Callback =
      toCB(ev.substituteCo(watch))

    def watchResult: F[A]
    /**
     * Wrap `fut` as a `Callback`, with a busy indicator and result notification message
     */
    final def watchResultCB(implicit ev: A =:= Callback): Callback =
      toCB(ev.substituteCo(watch))
    final def watchResultACB: AsyncCallback[A] = toACB(watchResult)
  }

  implicit class future[A](self: => Future[A]) extends ExtensionMethods[Future, A] {
    override protected def toCB(f: => Future[Callback]) = Callback.future(f)
    override protected def toACB = AsyncCallback.fromFuture(_)
    override def watch = notifyFailure(busyIndicator.showBusy(self))
    override def watchResult = notifyResult(busyIndicator.showBusy(self))
  }

  implicit class asyncCallback[A](self: AsyncCallback[A]) extends ExtensionMethods[AsyncCallback, A] {
    override protected def toCB(f: => AsyncCallback[Callback]): Callback = f.toCallback
    override protected def toACB: AsyncCallback[A] => AsyncCallback[A] = identity
    private def run(f: Future[A] => Future[A]) =
      AsyncCallback.delay {
          val promise = Promise[A]()
          f(promise.future)
          promise
        }
        .flatMap { promise =>
          self
            .attemptTry
            .tap(promise.complete)
            .flatMap(triedA => AsyncCallback.const(triedA))
        }
    override def watch: AsyncCallback[A] = run(future => notifyFailure(busyIndicator.showBusy(future)))
    override def watchResult: AsyncCallback[A] = run(future => notifyResult(busyIndicator.showBusy(future)))
  }
}
