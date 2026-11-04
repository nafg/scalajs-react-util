inThisBuild(
  List(
    homepage                            := Some(url("https://github.com/nafg/scalajs-react-util")),
    licenses                            := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    developers                          := List(
      Developer("nafg", "Naftoli Gugenheim", "98384+nafg@users.noreply.github.com", url("https://github.com/nafg"))
    ),
    dynverGitDescribeOutput             :=
      dynverGitDescribeOutput.value.map { o =>
        if (o.isSnapshot()) {
          // Trick to get "-SNAPSHOT" without the other bits
          o.copy(commitSuffix = sbtdynver.GitCommitSuffix(0, ""), dirtySuffix = sbtdynver.GitDirtySuffix("+"))
        } else
          o
      },
    dynverSonatypeSnapshots             := true,
    githubWorkflowJavaVersions          := Seq(JavaSpec.temurin("17")),
    githubWorkflowScalaVersions         := List("2.13.x", "3.x"),
    // In sbt 2, test only runs tests it considers affected by changes, and it can skip some even on a clean build
    githubWorkflowBuild                 := Seq(WorkflowStep.Sbt(List("testFull"), name = Some("Build project"))),
    githubWorkflowTargetTags ++= Seq("v*"),
    githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v"))),
    githubWorkflowPublish               := Seq(
      WorkflowStep.Sbt(
        List("ci-release"),
        env = Map(
          "PGP_PASSPHRASE"    -> "${{ secrets.PGP_PASSPHRASE }}",
          "PGP_SECRET"        -> "${{ secrets.PGP_SECRET }}",
          "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
          "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
        )
      )
    )
  )
)
