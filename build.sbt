import _root_.io.github.nafg.scalacoptions._


name := "scalajs-react-util"

ThisBuild / organization := "io.github.nafg.scalajs-react-util"

ThisBuild / crossScalaVersions := Seq("2.13.8", "3.1.1")
ThisBuild / scalaVersion := (ThisBuild / crossScalaVersions).value.last

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

def addScalajsReactModule(name: String) = libraryDependencies += "com.github.japgolly.scalajs-react" %%% name % "2.1.1"

publish / skip := true

lazy val core =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      moduleName := "core",
      sjsCrossTarget,
      addScalajsReactModule("core"),
      addScalajsReactModule("extra")
    )

lazy val editor =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(core)
    .settings(
      sjsCrossTarget,
      addScalajsReactModule("extra-ext-monocle3")
    )

lazy val `partial-renderer` =
  project
    .enablePlugins(ScalaJSBundlerPlugin)
    .dependsOn(core)
    .settings(
      sjsCrossTarget,
      libraryDependencies += "dev.optics" %%% "monocle-macro" % "3.1.0",
      libraryDependencies += "org.scalameta" %%% "munit" % "0.7.29" % Test,
      testFrameworks += new TestFramework("munit.Framework")
    )
