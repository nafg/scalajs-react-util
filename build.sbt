name := "scalajs-react-util"

enablePlugins(ScalaJSBundlerPlugin)

scalaVersion := "2.12.5"
organization := "io.github.nafg.scalajs-react-util"
moduleName := "core"
libraryDependencies += "com.github.japgolly.scalajs-react" %%% "extra" % "1.1.1"
