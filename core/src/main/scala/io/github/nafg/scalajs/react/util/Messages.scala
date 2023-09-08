package io.github.nafg.scalajs.react.util

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.vdom.html_<^.*
import japgolly.scalajs.react.{Callback, ScalaComponent}
import io.github.nafg.scalajs.react.util.ReactImplicits.step4_listen

class Messages {
  case class Message(timeout: Double, content: TagMod)

  private object broadcaster extends PublicBroadcaster[Message]

  def postCB(timeout: Double = 1000)(content: TagMod*): Callback =
    broadcaster.publish(Message(timeout, content.toTagMod))

  @deprecated("Use postCB(...)(...).runNow()", "0.12.1")
  def post(timeout: Double = 1000)(content: TagMod*): Unit = postCB(timeout)(content*).runNow()

  def postSuccessCB(message: TagMod) = postCB(500)(^.cls := "alert alert-success", message)
  @deprecated("Use postSuccessCB(...).runNow()", "0.12.1")
  def postSuccess(message: TagMod)   = postSuccessCB(message).runNow()

  def postErrorCB(message: TagMod) = postCB(5000)(^.cls := "alert alert-danger", message)
  @deprecated("Use postErrorCB(...).runNow()", "0.12.1")
  def postError(message: TagMod)   = postErrorCB(message).runNow()

  def defaultErrorMessage: Throwable => TagMod = _ => "An error occurred"

  def notifyResult[A](
    future: Future[A]
  )(success: A => TagMod = (_: A) => "Done!", failure: Throwable => TagMod = defaultErrorMessage)(implicit
    executionContext: ExecutionContext
  ): Future[A] =
    notifyFailure(future andThen { case Success(value) => postSuccessCB(success(value)).runNow() })(failure)

  def notifyFailure[A](future: Future[A])(failure: Throwable => TagMod = defaultErrorMessage)(implicit
    executionContext: ExecutionContext
  ): Future[A] =
    future andThen { case Failure(exception) => postErrorCB(failure(exception)).runNow() }

  object Implicits {
    implicit class FutureMessagesExtensionMethods[A](self: Future[A]) {
      def notifyResult(success: A => TagMod = _ => "Done!", failure: Throwable => TagMod = defaultErrorMessage)(implicit
        executionContext: ExecutionContext
      ): Future[A] =
        Messages.this.notifyResult(self)(success, failure)

      def notifyFailure(failure: Throwable => TagMod = defaultErrorMessage)(implicit
        executionContext: ExecutionContext
      ): Future[A] =
        Messages.this.notifyFailure(self)(failure)
    }
  }

  def render(messages: Seq[Message]) =
    <.div(
      ^.position.fixed,
      ^.top     := "0",
      ^.left    := "0",
      ^.right   := "0",
      ^.width   := "500px",
      ^.marginLeft.auto,
      ^.marginRight.auto,
      ^.opacity := "0.9",
      ^.zIndex  := "1000",
      ^.textAlign.center,
      messages.toVdomArray { msg =>
        <.div(^.key := System.identityHashCode(msg), msg.content)
      }
    )

  val component =
    ScalaComponent
      .builder[Unit]("Messages")
      .initialState(Seq.empty[Message])
      .backend(_ => OnUnmount())
      .render_S(render)
      .listen(_ => broadcaster) { msg => self =>
        self.modStateAsync(msg +: _) >>
          self.modStateAsync(_.filter(_ ne msg)).delayMs(msg.timeout)
      }
      .build
}

object Messages extends Messages
