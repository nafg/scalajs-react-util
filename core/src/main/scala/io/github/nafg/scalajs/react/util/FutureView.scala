package io.github.nafg.scalajs.react.util

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success, Try}

import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^.*
import japgolly.scalajs.react.{Callback, CallbackTo, CtorType, ScalaComponent}
import io.github.nafg.scalajs.react.util.AsyncStateFromProps.ext


object FutureValueViewImpl {
  case class Settings(renderLoading: () => VdomNode, onFailure: Throwable => CallbackTo[VdomNode])
  case class Props[A](content: A, settings: Settings)

  val component =
    ScalaComponent.builder[Props[Option[Try[VdomNode]]]]
      .stateless
      .noBackend
      .render_P {
        case Props(None, settings)                     => settings.renderLoading()
        case Props(Some(Success(elem)), _)             => elem
        case Props(Some(Failure(throwable)), settings) => settings.onFailure(throwable).runNow()
      }
      .build
}

abstract class FutureValueViewLike extends HasBusyIndicator {
  type F[_]

  protected def map[A, B](f: A => B): F[A] => F[B]

  val defaultSettings = FutureValueViewImpl.Settings(
    renderLoading = () => busyIndicator,
    onFailure = { throwable =>
      Callback(throwable.printStackTrace())
        .map(_ => <.em(^.color.gray, "Loading failed"))
    }
  )

  protected type State
  protected type Backend

  def component: Component[FutureValueViewImpl.Props[F[VdomNode]], State, Backend, CtorType.Props]

  def apply[A](content: F[A])(implicit f: A => VdomNode) =
    component(FutureValueViewImpl.Props(map(f)(content), defaultSettings))

  def custom[A](renderLoading: => VdomNode = defaultSettings.renderLoading(),
                onFailure: Throwable => CallbackTo[VdomNode] = defaultSettings.onFailure)
               (content: F[A])
               (implicit f: A => VdomNode) =
    component(FutureValueViewImpl.Props(map(f)(content), FutureValueViewImpl.Settings(() => renderLoading, onFailure)))
}

abstract class FutureValueView extends FutureValueViewLike {
  override type F[A] = Option[Try[A]]
  override protected def map[A, B](f: A => B) = _.map(_.map(f))
  override protected type State = Unit
  override protected type Backend = Unit
  override def component = FutureValueViewImpl.component
}

object FutureValueView extends FutureValueView with HasSpinnerImage

abstract class FutureView extends FutureValueViewLike {
  override type F[A] = Future[A]
  override protected def map[A, B](f: A => B) = _.map(f)
  override protected type State = Option[Try[VdomNode]]
  override protected type Backend = IsUnmounted.Backend
  val component =
    ScalaComponent.builder[FutureValueViewImpl.Props[Future[VdomNode]]]
      .initialStateFromProps(_.content.value)
      .backend(_ => new IsUnmounted.Backend)
      .render(self => FutureValueViewImpl.component(FutureValueViewImpl.Props(self.state, self.props.settings)))
      .asyncStateFromProps.constAlways((p, _, _) => p.content.transform(attempt => Success(Some(attempt))))
      .build
}

object FutureView extends FutureView with HasSpinnerImage
