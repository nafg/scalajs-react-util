addSbtPlugin("org.scala-js"                             % "sbt-scalajs"                % "1.15.0")
addSbtPlugin("ch.epfl.scala"                            % s"sbt-scalajs-bundler"       % "0.21.1")
addSbtPlugin("com.github.sbt"                           % "sbt-ci-release"             % "1.5.12")
addSbtPlugin("io.github.nafg.mergify"                   % "sbt-mergify-github-actions" % "0.8.0")
libraryDependencies += "io.github.nafg.scalac-options" %% "scalac-options"             % "0.2.0"

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
