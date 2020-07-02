package io.github.nafg.scalajs.react.util

import scala.concurrent.{ExecutionContext, Future}

import japgolly.scalajs.react.component.builder.Builder.{Config, Step4}
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

  def always[P, C <: Children, S, B, US <: UpdateSnapshot](compute: (P, S, B) => Future[S => S])
                                                          (implicit executionContext: ExecutionContext) =
    apply[P, C, S, B, US](_ != _, compute)
  def const[P, C <: Children, S, B, US <: UpdateSnapshot](predicate: (P, P) => Boolean, compute: (P, S, B) => Future[S])
                                                         (implicit executionContext: ExecutionContext) =
    apply[P, C, S, B, US](predicate, compute(_, _, _).map(fnConst))
  def constAlways[P, C <: Children, S, B, US <: UpdateSnapshot](compute: (P, S, B) => Future[S])
                                                               (implicit executionContext: ExecutionContext) =
    apply[P, C, S, B, US](_ != _, compute(_, _, _).map(fnConst))
  def apply[P, C <: Children, S, B, US <: UpdateSnapshot](predicate: (P, P) => Boolean,
                                                          compute: (P, S, B) => Future[S => S])
                                                         (implicit ec: ExecutionContext
                                                         ): Config[P, C, S, B, US, UpdateSnapshot.Some[US#Value]] =
    _.asyncStateFromProps(predicate, compute)

  implicit class ext[P, C <: Children, S, B, US <: UpdateSnapshot](self: Step4[P, C, S, B, US]) {
    object asyncStateFromProps {
      def apply(predicate: (P, P) => Boolean, compute: (P, S, B) => Future[S => S])
               (implicit executionContext: ExecutionContext): Step4[P, C, S, B, UpdateSnapshot.Some[US#Value]] = {
        val run: (P, S, StateW[P, S, B]) => Callback =
          (props, state, self) => Callback.future(compute(props, state, self.backend).map(self.modState))
        self
          .componentDidMount(self => run(self.props, self.state, self))
          .componentDidUpdate { self =>
            Callback.when(predicate(self.prevProps, self.currentProps))(run(self.currentProps, self.currentState, self))
          }
      }

      def always(compute: (P, S, B) => Future[S => S])
                (implicit executionContext: ExecutionContext) = apply(_ != _, compute)

      def const(predicate: (P, P) => Boolean, compute: (P, S, B) => Future[S])
               (implicit executionContext: ExecutionContext) =
        apply(predicate, compute(_, _, _).map(fnConst))

      def constAlways(compute: (P, S, B) => Future[S])
                     (implicit executionContext: ExecutionContext) =
        apply(_ != _, compute(_, _, _).map(fnConst))
    }
  }
}
