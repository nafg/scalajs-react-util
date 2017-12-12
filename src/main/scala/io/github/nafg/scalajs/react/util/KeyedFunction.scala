package io.github.nafg.scalajs.react.util

/**
  * Function (wrapper) whose equality is delegated to another value.
  * Two KeyedFunctions whose key compares equal will themselves compare equal.
  * Useful to control when React component props should appear equal or not
  */
case class KeyedFunction[+R](key: Any = ())(function: () => R) extends (() => R) {
  override def apply() = function()
}
