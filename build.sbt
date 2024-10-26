name         := "zk"
organization := "me.ooon"
scalaVersion := "2.13.15"
target       := studioTarget.value

libraryDependencies ++= Seq(
  "org.apache.zookeeper"        % "zookeeper"                % "3.9.2",
  "me.ooon"                    %% "orison"                   % "1.0.17"  % Test,
  "com.lihaoyi"                %% "os-lib"                   % "0.11.2"  % Test,
  "org.scalatest"              %% "scalatest-core"           % "3.2.19" % Test,
  "org.scalatest"               % "scalatest-compatible"     % "3.2.19" % Test,
  "org.scalatest"              %% "scalatest-diagrams"       % "3.2.19" % Test,
  "org.scalatest"              %% "scalatest-matchers-core"  % "3.2.19" % Test,
  "org.scalatest"              %% "scalatest-shouldmatchers" % "3.2.19" % Test,
  "org.scalatest"              %% "scalatest-freespec"       % "3.2.19" % Test,
  "org.slf4j"                   % "log4j-over-slf4j"         % "2.0.16"  % Test,
  "com.typesafe.scala-logging" %% "scala-logging"            % "3.9.5"  % Test,
  "ch.qos.logback"              % "logback-classic"          % "1.5.12"  % Test,
  // server 依赖
  "io.dropwizard.metrics" % "metrics-core" % "4.2.28"  % Test,
  "org.xerial.snappy"     % "snappy-java"  % "1.1.10.7" % Test
)

excludeDependencies ++= Seq(
  ExclusionRule("org.slf4j", "slf4j-log4j12"),
  ExclusionRule("log4j", "log4j")
)
