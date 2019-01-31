package io.github.nafg.scalajs.react.util.editor

import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Lifecycle.RenderScope
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.html_<^._
import io.github.nafg.scalajs.react.util.Debounce

import cats.data.Validated
import sourcecode.FullName


object Editor {
  case class State[A](text: String, validated: Validated[TagMod, A])
}

class Editor[A](fieldConstructor: FieldConstructor)(implicit format: Format[A], name: FullName) {
  def this(fieldConstructor: FieldConstructor, format: Format[A])
          (implicit name: FullName, dummyImplicit: DummyImplicit) =
    this(fieldConstructor)(format, name)

  case class Props(snapshot: StateSnapshot[A], tagMod: TagMod)

  private def mkState(value: A): Editor.State[A] = Editor.State(format.format(value), Validated.valid(value))
  private def canonicalize(state: Editor.State[A]): Editor.State[A] = state.validated.fold(_ => state, mkState)

  val handleValidChange: ((RenderScope[Props, Editor.State[A], Unit], A)) => Callback =
    Debounce.callback() { case (self, v) => self.props.snapshot.setState(v) }

  val component =
    ScalaComponent.builder[Props](name.value)
      .initialStateFromProps(props => mkState(props.snapshot.value))
      .render { self =>
        val tagMods = TagMod(self.props.tagMod, ^.onBlur --> self.modState(canonicalize _))
        fieldConstructor.render(self.state, tagMods) { text =>
          val newState = Editor.State(text, format.parse(text))
          self.setState(newState) >>
            Callback.traverseOption(newState.validated.toOption)(v => handleValidChange((self, v)))
        }
      }
      .componentWillReceiveProps { self =>
        val nextValue = self.nextProps.snapshot.value
        Callback.when(nextValue != self.currentProps.snapshot.value) {
          self.modState { state =>
            if (state.validated.exists(_ == nextValue)) state else mkState(nextValue)
          }
        }
      }
      .build

  def apply(snapshot: StateSnapshot[A])(tagMods: TagMod*) = component(Props(snapshot, tagMods.toTagMod))
}
