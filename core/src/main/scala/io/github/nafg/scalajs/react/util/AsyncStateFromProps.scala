package io.github.nafg.scalajs.react.util

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import japgolly.scalajs.react.component.builder.Builder.Step4
import japgolly.scalajs.react.component.builder.Lifecycle.StateW
import japgolly.scalajs.react.{Callback, Children, UpdateSnapshot}


/**
 * Reuse logic for componentDidMount and componentWillReceiveProps to asynchronously compute the state from the props
 * Can accept a predicate to use in componentWillReceiveProps, to compare currentProps with nextProps
 * Methods with "always" in the name don't take a predicate
 * Methods with "const" in the name expect the computation to produce a Future[S] for setState;
 * the other methods expect it to produce a Future[S => S] for modState.
 */
object AsyncStateFromProps {
  private def fnConst[S]: S => S => S = s => _ => s

  def always[P, C <: Children, S, B <: IsUnmounted, US <: UpdateSnapshot](compute: (P, S, B) => Future[S => S]) =
    apply[P, C, S, B, US](_ != _, compute)
  def const[P, C <: Children, S, B <: IsUnmounted, US <: UpdateSnapshot](predicate: (P, P) => Boolean,
                                                                         compute: (P, S, B) => Future[S]) =
    apply[P, C, S, B, US](predicate, compute(_, _, _).map(fnConst))
  def constAlways[P, C <: Children, S, B <: IsUnmounted, US <: UpdateSnapshot](compute: (P, S, B) => Future[S]) =
    apply[P, C, S, B, US](_ != _, compute(_, _, _).map(fnConst))
  def apply[P, C <: Children, S, B <: IsUnmounted, US <: UpdateSnapshot](predicate: (P, P) => Boolean,
                                                                         compute: (P, S, B) => Future[S => S]) =
    (self: Step4[P, C, S, B, US]) => ext(self).asyncStateFromProps(predicate, compute)

  implicit class ext[P, C <: Children, S, B <: IsUnmounted, US <: UpdateSnapshot](self: Step4[P, C, S, B, US]) {
    object asyncStateFromProps {
      def apply(predicate: (P, P) => Boolean, compute: (P, S, B) => Future[S => S]) = {
        val run: (P, S, StateW[P, S, B]) => Callback = { (props, state, self) =>
          Callback.future(compute(props, state, self.backend)
            .map(f => Callback.unless(self.backend.isUnmounted)(self.modState(f))))
        }
        self
          .componentDidMount(self => run(self.props, self.state, self))
          .componentDidUpdate { self =>
            Callback.when(predicate(self.prevProps, self.currentProps))(run(self.currentProps, self.currentState, self))
          }
          .configure(IsUnmounted.install)
      }

      def always(compute: (P, S, B) => Future[S => S]) = apply(_ != _, compute)

      def const(predicate: (P, P) => Boolean, compute: (P, S, B) => Future[S]) =
        apply(predicate, compute(_, _, _).map(fnConst))

      def constAlways(compute: (P, S, B) => Future[S]) =
        apply(_ != _, compute(_, _, _).map(fnConst))
    }
  }
}
