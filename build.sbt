lazy val buildSettings = Seq(
  name               := "poi",            // was: "poi-scala"
  organization       := "de.sciss",       // was: "info.folone"
  version            := "0.1.0",          // independent from folone's project
  scalaVersion       := "2.11.6",
  crossScalaVersions := Seq("2.10.5", "2.11.6"),
  scalacOptions     ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture"),
  licenses           := Seq("Apache License" -> url("http://opensource.org/licenses/Apache-2.0")),
  homepage           := Some(url(s"https://github.com/Sciss/${name.value}"))
)

// ---- main dependencies ----

lazy val apachePOIVersion = "3.12"

// ---- test dependencies ----

lazy val scalazVersion      = "7.1.2"
lazy val specs2Version      = "2.4.1"  // later versions lack scalaz-stream 2.11 dependency
lazy val scalaCheckVersion  = "1.12.3"

lazy val standardSettings = buildSettings ++ Seq(
  libraryDependencies ++= Seq(
    "org.apache.poi" %  "poi"                       % apachePOIVersion,
    "org.apache.poi" %  "poi-ooxml"                 % apachePOIVersion,
    "org.scalaz"     %% "scalaz-core"               % scalazVersion     % "test",
    // "org.scalaz"     %% "scalaz-effect"             % scalazVersion  % "test",
    "org.specs2"     %% "specs2"                    % specs2Version     % "test",
    "org.scalacheck" %% "scalacheck"                % scalaCheckVersion % "test",
    "org.scalaz"     %% "scalaz-scalacheck-binding" % scalazVersion     % "test"
  ),
  initialCommands in console := """import de.sciss.poi._""",
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo :=
    Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    ),
  pomExtra := {
    val n = name.value
    <scm>
      <url>git@github.com:Sciss/{n}.git</url>
      <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
    </scm>
    <developers>
      {
      Seq(
        ("folone",       "George Leontiev"  , "http://github.com/folone"  ),
        ("fedgehog",     "Maxim Fedorov"    , "http://github.com/fedgehog"),
        ("Michael Rans", "Michael Rans"     , "http://github.com/Michael Rans"),
        ("daneko",       "Kouichi Akatsuka" , "http://github.com/daneko"  ),
        ("rintcius",     "Rintcius Blok"    , "http://github.com/rintcius"),
        ("Sciss",        "Hanns Holger Rutz", "http://github.com/Sciss"   )
      ).map { case (id, name0, url) =>
        <developer>
          <id>{id}</id>
          <name>{name0}</name>
          <url>{url}</url>
        </developer>
      }
      }
    </developers>
  }
)

lazy val poi = project.in(file(".")).settings(standardSettings)
