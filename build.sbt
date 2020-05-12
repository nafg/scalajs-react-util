name := "scalajs-react-util"

ThisBuild / organization := "io.github.nafg.scalajs-react-util"

ThisBuild / crossScalaVersions := Seq("2.12.11", "2.13.2")
ThisBuild / scalaVersion := (ThisBuild / crossScalaVersions).value.last

ThisBuild / scalacOptions += "-deprecation"

def sjsCrossTarget = crossTarget ~= (new File(_, "sjs" + scalaJSVersion))

def addScalajsReactModule(name: String) = libraryDependencies += "com.github.japgolly.scalajs-react" %%% name % "1.7.0"

publish / skip := true

lazy val core =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      moduleName := "core",
      sjsCrossTarget,
      addScalajsReactModule("extra")
    )

lazy val editor =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(core)
    .settings(
      sjsCrossTarget,
      addScalajsReactModule("ext-monocle-cats")
    )
