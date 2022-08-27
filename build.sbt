name         := "zk"
organization := "me.ooon"
scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  "org.apache.zookeeper"        % "zookeeper"                % "3.7.1",
  "me.ooon"                    %% "orison"                   % "1.0.2"  % Test,
  "com.lihaoyi"                %% "os-lib"                   % "0.8.1"  % Test,
  "org.scalatest"              %% "scalatest-core"           % "3.2.13" % Test,
  "org.scalatest"               % "scalatest-compatible"     % "3.2.13" % Test,
  "org.scalatest"              %% "scalatest-diagrams"       % "3.2.13" % Test,
  "org.scalatest"              %% "scalatest-matchers-core"  % "3.2.13" % Test,
  "org.scalatest"              %% "scalatest-shouldmatchers" % "3.2.13" % Test,
  "org.scalatest"              %% "scalatest-freespec"       % "3.2.13" % Test,
  "org.slf4j"                   % "log4j-over-slf4j"         % "1.7.36" % Test,
  "com.typesafe.scala-logging" %% "scala-logging"            % "3.9.5"  % Test,
  "ch.qos.logback"              % "logback-classic"          % "1.2.11" % Test,
  // server 依赖
  "io.dropwizard.metrics" % "metrics-core" % "4.2.10"  % Test,
  "org.xerial.snappy"     % "snappy-java"  % "1.1.8.4" % Test
)

excludeDependencies ++= Seq(
  ExclusionRule("org.slf4j", "slf4j-log4j12"),
  ExclusionRule("log4j", "log4j")
)
