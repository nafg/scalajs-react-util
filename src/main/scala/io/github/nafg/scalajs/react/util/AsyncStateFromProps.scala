package io.github.nafg.scalajs.react.util

import scala.concurrent.{ExecutionContext, Future}

import japgolly.scalajs.react.component.builder.Builder.Config
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
}
