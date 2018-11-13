package io.github.nafg.scalajs.react.util

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.extra.{Broadcaster, Listenable, OnUnmount}
import japgolly.scalajs.react.vdom.html_<^._


class Messages {
  case class Message(timeout: Double, content: TagMod)

  private object broadcaster extends Broadcaster[Message] {
    // overriding to make it public
    override def broadcast(a: Message) = super.broadcast(a)
  }

  def post(timeout: Double = 1000)(content: TagMod*): Unit =
    broadcaster.broadcast(Message(timeout, content.toTagMod)).runNow()

  def postSuccess(message: TagMod): Unit = post(500)(^.cls := "alert alert-success", message)
  def postError(message: TagMod): Unit = post(5000)(^.cls := "alert alert-danger", message)

  def defaultErrorMessage: Throwable => TagMod = _ => "An error occurred"

  def notifyResult[A](future: Future[A])
                     (success: A => TagMod = (_: A) => "Done!",
                      failure: Throwable => TagMod = defaultErrorMessage)
                     (implicit executionContext: ExecutionContext): Future[A] =
    notifyFailure(future andThen { case Success(value) => postSuccess(success(value)) })(failure)

  def notifyFailure[A](future: Future[A])
                      (failure: Throwable => TagMod = defaultErrorMessage)
                      (implicit executionContext: ExecutionContext): Future[A] =
    future andThen { case Failure(exception) => postError(failure(exception)) }

  object Implicits {
    implicit class FutureMessagesExtensionMethods[A](self: Future[A]) {
      def notifyResult(success: A => TagMod = _ => "Done!",
                       failure: Throwable => TagMod = defaultErrorMessage)
                      (implicit executionContext: ExecutionContext): Future[A] =
        Messages.this.notifyResult(self)(success, failure)

      def notifyFailure(failure: Throwable => TagMod = defaultErrorMessage)
                       (implicit executionContext: ExecutionContext): Future[A] =
        Messages.this.notifyFailure(self)(failure)
    }
  }

  def render(messages: Seq[Message]) =
    <.div(
      ^.position.fixed,
      ^.top := "0",
      ^.left := "0",
      ^.right := "0",
      ^.width := "500px",
      ^.marginLeft.auto,
      ^.marginRight.auto,
      ^.opacity := "0.9",
      ^.zIndex := "1000",
      ^.textAlign.center,
      messages.toVdomArray { msg =>
        <.div(
          ^.key := System.identityHashCode(msg),
          msg.content
        )
      }
    )

  val component =
    ScalaComponent.builder[Unit]("Messages")
      .initialState(Seq.empty[Message])
      .backend(_ => new OnUnmount.Backend)
      .render_S(render)
      .configure(Listenable.listen(_ => broadcaster, { self =>
        (msg: Message) =>
          self.modState(messages => msg +: messages) >>
            self.modState(messages => messages.filter(_ ne msg)).delayMs(msg.timeout).void
      }))
      .build
}

object Messages extends Messages
