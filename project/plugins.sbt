addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.7.0")
addSbtPlugin("ch.epfl.scala" % s"sbt-scalajs-bundler" % "0.20.0")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.7")
addSbtPlugin("com.codecommit" % "sbt-github-actions" % "0.13.0")
libraryDependencies += "io.github.nafg.mergify" %% "mergify-writer" % "0.2.1"
