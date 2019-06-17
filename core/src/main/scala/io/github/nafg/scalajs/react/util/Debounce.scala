package io.github.nafg.scalajs.react.util

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetTimeoutHandle

import japgolly.scalajs.react.Callback
import scala.concurrent.duration._


object Debounce {
  private val DefaultDuration = 250.milli

  def apply[A](duration: FiniteDuration = DefaultDuration)(f: A => Unit): A => Unit = {
    var timeout = Option.empty[SetTimeoutHandle]

    def impl(a: A): Unit = {
      def run(): Unit = {
        timeout = None
        f(a)
      }

      timeout.foreach(timers.clearTimeout)
      timeout = Some(timers.setTimeout(duration)(run()))
    }

    impl
  }

  def callback[A](duration: FiniteDuration = DefaultDuration)(f: A => Callback): A => Callback = {
    var timeout = Option.empty[SetTimeoutHandle]

    a =>
      Callback {
        timeout.foreach(timers.clearTimeout)
        timeout = Some(timers.setTimeout(duration) {
          timeout = None
          f(a).runNow()
        })
      }
  }

  def future[A, B](duration: FiniteDuration = DefaultDuration)(f: A => Future[B])
                  (implicit executionContext: ExecutionContext): A => Future[B] = {
    var timeout = Option.empty[SetTimeoutHandle]
    var invocationNum = 0

    def impl(a: A): Future[B] = {
      invocationNum += 1
      val promise = Promise[B]

      def run(): Unit = {
        timeout = None
        val curInvocationNum = invocationNum
        f(a).onComplete { t =>
          if (invocationNum == curInvocationNum)
            promise.complete(t)
        }
      }

      timeout.foreach(timers.clearTimeout)
      timeout = Some(timers.setTimeout(duration)(run()))

      promise.future
    }

    impl
  }
}
