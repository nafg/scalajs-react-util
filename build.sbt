import _root_.io.github.nafg.scalacoptions.*

LocalRootProject / name := "scalajs-react-util"

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

// A bare setting, which sbt 2 applies to every project, so that each one evaluates it against its own Scala version
scalacOptions ++= myScalacOptions(scalaVersion.value)

ThisBuild / versionScheme := Some("early-semver")

def addScalajsReactModule(name: String) = libraryDependencies += "com.github.japgolly.scalajs-react" %% name % "4.1.0"

LocalRootProject / publish / skip := true

lazy val core =
  project
    .enablePlugins(ScalaJSPlugin)
    .settings(
      moduleName := "core",
      addScalajsReactModule("core"),
      addScalajsReactModule("extra"),
      libraryDependencies ++= Seq(
        "org.scala-js" %% "scala-js-macrotask-executor" % "1.1.1",
        "dev.optics"   %% "monocle-macro"               % "3.3.0"
      )
    )

lazy val editor =
  project
    .enablePlugins(ScalaJSPlugin)
    .dependsOn(core)
    .settings(addScalajsReactModule("extra-ext-monocle3"))

lazy val `partial-renderer` =
  project
    .enablePlugins(ScalaJSPlugin)
    .dependsOn(core)
    .settings(
      addScalajsReactModule("extra-ext-monocle3"),
      libraryDependencies += "org.scalameta" %% "munit" % "1.3.6" % Test,
      testFrameworks += new TestFramework("munit.Framework")
    )
