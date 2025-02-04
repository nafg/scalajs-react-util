import _root_.io.github.nafg.scalacoptions.*

name := "scalajs-react-util"

ThisBuild / organization := "io.github.nafg.scalajs-react-util"

ThisBuild / crossScalaVersions := Seq("2.13.16", "3.3.5")
ThisBuild / scalaVersion       := (ThisBuild / crossScalaVersions).value.last

def myScalacOptions(version: String) =
  ScalacOptions.all(version)(
    (opts: options.Common) =>
      opts.deprecation ++
        opts.unchecked ++
        opts.feature,
    (_: options.V2).explaintypes,
    (_: options.V2_13).Xlint("_"),
    (opts: options.V2_13_6_+) =>
      opts.WdeadCode ++
        opts.WextraImplicit ++
        opts.WnumericWiden ++
        opts.XlintUnused ++
        opts.WvalueDiscard ++
        opts.Xsource("3")
  )

ThisBuild / scalacOptions ++= myScalacOptions(scalaVersion.value)

ThisBuild / versionScheme := Some("early-semver")

def sjsCrossTarget = crossTarget ~= (new File(_, "sjs" + scalaJSVersion))

def addScalajsReactModule(name: String) = libraryDependencies += "com.github.japgolly.scalajs-react" %%% name % "2.1.2"

publish / skip := true

lazy val core =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      moduleName := "core",
      sjsCrossTarget,
      addScalajsReactModule("core"),
      addScalajsReactModule("extra"),
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scala-js-macrotask-executor" % "1.1.1",
        "dev.optics"   %%% "monocle-macro"               % "3.3.0"
      )
    )

lazy val editor =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(core)
    .settings(sjsCrossTarget, addScalajsReactModule("extra-ext-monocle3"))

lazy val `partial-renderer` =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(core)
    .settings(
      sjsCrossTarget,
      addScalajsReactModule("extra-ext-monocle3"),
      libraryDependencies += "org.scalameta" %%% "munit" % "1.1.0" % Test,
      testFrameworks += new TestFramework("munit.Framework")
    )
