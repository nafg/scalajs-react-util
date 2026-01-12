package io.github.nafg.scalajs.react.util

import scala.annotation.unchecked.uncheckedVariance
import scala.concurrent.Future
import scala.scalajs.js

import org.scalajs.dom
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global
import japgolly.scalajs.react.*
import japgolly.scalajs.react.component.builder.{ComponentBuilder, Lifecycle}
import japgolly.scalajs.react.extra.{Listenable, OnUnmount, StateSnapshot}
import japgolly.scalajs.react.facade.SyntheticEvent
import japgolly.scalajs.react.util.Effect.Dispatch
import japgolly.scalajs.react.vdom.{Attr, VdomNode}
import io.github.nafg.scalajs.react.util.SnapshotUtils.Snapshot

import cats.Applicative
import sourcecode.FullName

object ReactImplicits {
  implicit class SjsReactEventOps[E[+x <: dom.Node] <: SyntheticEvent[x]](private val self: Attr.Event[E])
      extends AnyVal {
    def -->!(cb: => Callback)  = self ==> (cb.asEventDefault(_).void)
    def -->!!(cb: => Callback) = self ==>
      ((event: self.Event) => event.stopPropagationCB >> cb.asEventDefault(event).void)
  }

  implicit class step1_asyncState[P](private val self: ComponentBuilder.Step1[P]) {
    private def spinUntilState[A](
      maybeState: StateSnapshot[Option[A]]
    )(render: StateSnapshot[A] => VdomNode)(implicit loadingIndicator: LoadingIndicator): VdomNode =
      maybeState.value match {
        case None    => loadingIndicator.render
        case Some(a) => render(Snapshot(a)(a => maybeState.setState(Some(a))))
      }

    def asyncStateRefreshable[S, B <: IsUnmounted](f: P => Future[S], backend: => B = new IsUnmounted.Backend)(
      render: (P, StateSnapshot[S], Callback) => VdomNode
    )(implicit loadingIndicator: LoadingIndicator = LoadingIndicator.spinner10) = {
      def load(props: P, setState: Option[S] => Callback, backend: IsUnmounted) =
        Callback.future(f(props).map(s => Callback.unless(backend.isUnmounted)(setState(Some(s)))))
      self
        .initialState(Option.empty[S])
        .backend(_ => backend)
        .render { self =>
          spinUntilState(StateSnapshot.of(self)(StateAccessor.scalaLifecycleStateRW))(
            render(self.props, _, load(self.props, self.setState, self.backend))
          )
        }
        .componentDidMount(self => load(self.props, self.setState, self.backend))
    }

    def asyncState[S, B <: IsUnmounted](f: P => Future[S], backend: => B = new IsUnmounted.Backend)(
      render: (P, StateSnapshot[S]) => VdomNode
    ) =
      asyncStateRefreshable(f, backend)((p, s, _) => render(p, s))
  }

  implicit class step4_listen[P, C <: Children, S, B, U <: UpdateSnapshot](
    private val self: ComponentBuilder.LastStep[P, C, S, B, U]
  ) extends AnyVal {

    def listen[F[_]: Dispatch, A](listenable: P => Listenable[A])(
      makeListener: A => Lifecycle.ComponentDidMount[P, S, B] => F[Unit]
    )(implicit ev: B <:< OnUnmount): ComponentBuilder.LastStep[P, C, S, B, U] = {
      type Step4Out[-BB] = ComponentBuilder.LastStep[P, C, S, BB @uncheckedVariance, U]
      type Step4In[+BB]  = ComponentBuilder.LastStep[P, C, S, BB @uncheckedVariance, U]
      type $[-BB]        = Lifecycle.ComponentDidMount[P, S, BB @uncheckedVariance]

      val config =
        Listenable.listen[F, P, C, S, OnUnmount, U, A](
          listenable = listenable,
          makeListener = $ => makeListener(_)(ev.substituteContra[$]($))
        )

      (ev.substituteContra[Step4Out] _)
        .compose(config)
        .compose(ev.substituteCo[Step4In] _)
        .apply(self)
    }
  }

  implicit class withDisplayName[P, CT[-p, +u] <: CtorType[p, u]](self: ScalaFnComponent[P, CT]) {
    def withDisplayName(name: String)                                     = {
      self.raw.asInstanceOf[js.Dynamic].displayName = name
      self
    }
    def withDisplayName(implicit name: FullName): ScalaFnComponent[P, CT] =
      withDisplayName(name.value.stripSuffix(".component"))
  }

  implicit val asyncCallbackApplicative: Applicative[AsyncCallback] =
    new Applicative[AsyncCallback] {
      override def pure[A](x: A): AsyncCallback[A]                                             = AsyncCallback.pure(x)
      override def ap[A, B](ff: AsyncCallback[A => B])(fa: AsyncCallback[A]): AsyncCallback[B] =
        ff.zipWith(fa)((f, a) => f(a))
    }
}
