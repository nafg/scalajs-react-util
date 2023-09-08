package io.github.nafg.scalajs.react.util.editor

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.{TagMod, VdomElement}
import japgolly.scalajs.react.{Callback, ReactEventFromInput}

import sourcecode.FullName

trait FieldConstructor {
  def render(editorState: Editor.State[_], tagMod: TagMod)(onChange: String => Callback): VdomElement

  final def editorOf[A](implicit format: Format[A], name: FullName): Editor[A]                               =
    new Editor[A](this)(format, name)
  final def editorOf[A](format: Format[A])(implicit name: FullName, dummyImplicit: DummyImplicit): Editor[A] =
    new Editor[A](this)(format, name)
}

object FieldConstructor {
  class Base(renderElement: TagMod => VdomElement, invalidTagMod: TagMod => TagMod) extends FieldConstructor {
    override def render(editorState: Editor.State[_], tagMod: TagMod)(onChange: String => Callback) =
      renderElement(
        TagMod(
          tagMod,
          editorState.validated.swap.toOption.map(invalidTagMod).getOrElse(TagMod.empty),
          ^.value := editorState.text,
          ^.onChange ==> ((event: ReactEventFromInput) => onChange(event.target.value))
        )
      )
  }
}
