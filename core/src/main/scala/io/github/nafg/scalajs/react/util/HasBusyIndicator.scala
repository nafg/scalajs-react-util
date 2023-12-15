package io.github.nafg.scalajs.react.util

import japgolly.scalajs.react.vdom.html_<^.*
import japgolly.scalajs.react.vdom.{HtmlTopNode, TagOf}

trait HasBusyIndicator {
  def busyIndicator: TagOf[HtmlTopNode]
}

trait HasSpinnerImage extends HasBusyIndicator {
  override def busyIndicator: TagOf[HtmlTopNode] = <.img(^.src := SpinnerImage.uri)
}
