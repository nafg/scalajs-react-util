package io.github.nafg.scalajs.react.util

import japgolly.scalajs.react.vdom.VdomNode

trait LoadingIndicator  {
  def render: VdomNode
}
object LoadingIndicator {
  def apply(node: => VdomNode) = new LoadingIndicator {
    override def render = node
  }

  val spinner10 = LoadingIndicator(Spinner.inline(10))
  val spinner16 = LoadingIndicator(Spinner.inline(16))

  object Implicits {
    implicit def spinner16: LoadingIndicator = LoadingIndicator.spinner16
  }
}
