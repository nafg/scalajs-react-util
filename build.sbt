import _root_.io.github.nafg.scalacoptions.*

name := "scalajs-react-util"

ThisBuild / organization := "io.github.nafg.scalajs-react-util"

ThisBuild / crossScalaVersions := Seq("2.13.18", "3.9.0")
ThisBuild / scalaVersion       := (ThisBuild / crossScalaVersions).value.last

def myScalacOptions(version: String) =
  ScalacOptions.all(version)(
    (opts: options.Common) =>
      opts.deprecation ++
        opts.feature,
    (opts: options.V2) =>
      opts.unchecked ++
        opts.explaintypes,
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

val scalajsReactVersion = "4.1.0"

def addScalajsReactModule(name: String) =
  libraryDependencies += "com.github.japgolly.scalajs-react" %%% name % scalajsReactVersion

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
      libraryDependencies ++= Seq(
        "org.scalameta"                     %%% "munit" % "1.3.6" % Test,
        "com.github.japgolly.scalajs-react" %%% "test"  % scalajsReactVersion % Test
      ),
      testFrameworks += new TestFramework("munit.Framework"),
      Test / requireJsDomEnv := true,
      // scalajs-bundler's default webpack (5.24.3) hashes with md4, which Node 17+ rejects
      webpack / version := "5.98.0",
      Test / npmDependencies ++= Seq("react" -> "18.3.1", "react-dom" -> "18.3.1")
    )
