package io.github.nafg.scalajs.react.util

import scala.concurrent.{ExecutionContext, Future}

import japgolly.scalajs.react.component.builder.Builder.{Config, Step4}
import japgolly.scalajs.react.component.builder.Lifecycle.StateRW
import japgolly.scalajs.react.{Callback, Children, UpdateSnapshot}


/**
  * Reuse logic for componentDidMount and componentWillReceiveProps to asynchronously compute the state from the props
  * Can accept a predicate to use in componentWillReceiveProps, to compare currentProps with nextProps
  * Methods with "always" in the name don't take a predicate
  * Methods with "const" in the name expect the computation to produce a Future[S] for setState;
  * the other methods expect it to produce a Future[S => S] for modState.
  */
object AsyncStateFromProps {
  def always[P, C <: Children, S, B, US <: UpdateSnapshot](compute: (StateRW[P, S, B], P) => Future[S => S])
                                                          (implicit executionContext: ExecutionContext): Config[P, C, S, B, US, US] =
    apply[P, C, S, B, US]((_, _) => true, compute)
  def const[P, C <: Children, S, B, US <: UpdateSnapshot](predicate: (P, P) => Boolean, compute: (StateRW[P, S, B], P) => Future[S])
                                                         (implicit executionContext: ExecutionContext): Config[P, C, S, B, US, US] =
    apply[P, C, S, B, US](predicate, compute(_, _).map(Function.const(_)))
  def constAlways[P, C <: Children, S, B, US <: UpdateSnapshot](compute: (StateRW[P, S, B], P) => Future[S])
                                                               (implicit executionContext: ExecutionContext): Config[P, C, S, B, US, US] =
    apply[P, C, S, B, US]((_, _) => true, compute(_, _).map(Function.const(_)))
  def apply[P, C <: Children, S, B, US <: UpdateSnapshot](predicate: (P, P) => Boolean, compute: (StateRW[P, S, B], P) => Future[S => S])
                                                         (implicit executionContext: ExecutionContext): Config[P, C, S, B, US, US] = { builder =>
    val run: (P, StateRW[P, S, B]) => Callback =
      (props, self) => Callback.future(compute(self, props).map(self.modState))
    builder
      .componentDidMount(self => run(self.props, self))
      .componentWillReceiveProps { self =>
        Callback.when(predicate(self.currentProps, self.nextProps))(run(self.nextProps, self))
      }
  }

  implicit class ext[P, C <: Children, S, B, US <: UpdateSnapshot](self: Step4[P, C, S, B, US]) {
    object asyncStateFromProps {
      def apply(predicate: (P, P) => Boolean, compute: (StateRW[P, S, B], P) => Future[S => S])
               (implicit executionContext: ExecutionContext): Step4[P, C, S, B, US] = {
        val run: (P, StateRW[P, S, B]) => Callback =
          (props, self) => Callback.future(compute(self, props).map(self.modState))
        self
          .componentDidMount(self => run(self.props, self))
          .componentWillReceiveProps { self =>
            Callback.when(predicate(self.currentProps, self.nextProps))(run(self.nextProps, self))
          }
      }

      def always(compute: (StateRW[P, S, B], P) => Future[S => S])
                (implicit executionContext: ExecutionContext): Step4[P, C, S, B, US] = apply((_, _) => true, compute)

      def const(predicate: (P, P) => Boolean, compute: (StateRW[P, S, B], P) => Future[S])
               (implicit executionContext: ExecutionContext): Step4[P, C, S, B, US] =
        apply(predicate, compute(_, _).map(Function.const(_)))

      def constAlways(compute: (StateRW[P, S, B], P) => Future[S])
                     (implicit executionContext: ExecutionContext): Step4[P, C, S, B, US] =
        apply((_, _) => true, compute(_, _).map(Function.const(_)))
    }
  }
}
