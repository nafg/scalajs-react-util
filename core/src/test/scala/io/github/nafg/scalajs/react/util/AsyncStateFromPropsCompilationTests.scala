package io.github.nafg.scalajs.react.util

import scala.concurrent.Future

import japgolly.scalajs.react.{Children, UpdateSnapshot}


object AsyncStateFromPropsCompilationTests {
  def dummy[P, S, B <: IsUnmounted, C <: Children, US <: UpdateSnapshot] =
    AsyncStateFromProps[P, C, S, B, US]((_, _) => true, { (_: P, _: S, _: B) =>
      Future.successful(s => s)
    })
}
