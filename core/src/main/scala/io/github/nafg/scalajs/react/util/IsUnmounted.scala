package io.github.nafg.scalajs.react.util

import japgolly.scalajs.react.ScalaComponent.Config
import japgolly.scalajs.react.{Callback, Children, UpdateSnapshot}


trait IsUnmounted {
  private var unmounting = false
  def setUnmounting = Callback {
    unmounting = true
  }
  def isUnmounted = unmounting
}

object IsUnmounted {
  class Backend extends IsUnmounted

  def install[P, C <: Children, S, B <: IsUnmounted, U <: UpdateSnapshot]: Config[P, C, S, B, U, U] =
    _.componentWillUnmount(_.backend.setUnmounting)
}
