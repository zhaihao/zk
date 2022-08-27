import sbt._
import sbtrelease.ReleaseStateTransformations._

Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / versionScheme     := Some("early-semver")

Global / publishTo := Some("GitHub Apache Maven Packages" at "https://maven.pkg.github.com/zhaihao/zk")
Global / credentials += Credentials(
  "GitHub Package Registry",
  "maven.pkg.github.com",
  "zhaihao",
  sys.env("GITHUB_TOKEN")
)
publishMavenStyle := true
licenses          := Seq("MPL2" -> url("https://www.mozilla.org/en-US/MPL/2.0/"))

releaseCrossBuild           := true
releaseIgnoreUntrackedFiles := true
releaseCommitMessage        := s"release: v${(ThisBuild / version).value}"
releaseNextCommitMessage    := s"prepare: v${(ThisBuild / version).value}"

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
