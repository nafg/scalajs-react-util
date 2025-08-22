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
    githubWorkflowScalaVersions         := List("2.13.x", "3.x"),
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
