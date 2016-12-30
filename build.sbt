lazy val baseName  = "Sheet"
lazy val baseNameL = baseName.toLowerCase

lazy val projectVersion = "0.1.2"
lazy val mimaVersion    = "0.1.0"

lazy val buildSettings = Seq(
  name               := baseName,          // was: "poi-scala"
  organization       := "de.sciss",        // was: "info.folone"
  version            := projectVersion,    // independent from folone's project
  scalaVersion       := "2.11.8",
  crossScalaVersions := Seq("2.12.1", "2.11.8", "2.10.6"),
  scalacOptions     ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture", "-Xlint"),
  licenses           := Seq("Apache License" -> url("http://opensource.org/licenses/Apache-2.0")),
  homepage           := Some(url(s"https://github.com/Sciss/${name.value}"))
)

// ---- main dependencies ----

lazy val apachePOIVersion = "3.15"

// ---- test dependencies ----

lazy val specs2Version      = "3.8.6" // "2.4.1"  // later versions lack scalaz-stream 2.11 dependency
lazy val scalaCheckVersion  = "1.13.4"

lazy val standardSettings = buildSettings ++ Seq(
  libraryDependencies ++= Seq(
    "org.apache.poi" %  "poi"                       % apachePOIVersion,
    "org.apache.poi" %  "poi-ooxml"                 % apachePOIVersion,
    "org.specs2"     %% "specs2-core"               % specs2Version     % "test",
    "org.specs2"     %% "specs2-scalacheck"         % specs2Version     % "test",
    "org.scalacheck" %% "scalacheck"                % scalaCheckVersion % "test"
  ),
  initialCommands in console := """import de.sciss.sheet._""",
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

lazy val root = project.in(file("."))
  .settings(standardSettings)
  .settings(
    mimaPreviousArtifacts := Set("de.sciss" %% baseNameL % mimaVersion)
  )
