import scalapb.compiler.Version.{grpcJavaVersion, scalapbVersion}

name := "trading-bot"
version := "0.1"
scalaVersion := "2.12.10"

PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)

libraryDependencies ++= Seq(
  "io.grpc" % "grpc-netty" % grpcJavaVersion,
  "io.netty" % "netty-tcnative-boringssl-static" % "2.0.25.Final",
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion % "protobuf",
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapbVersion
)

assemblyMergeStrategy in assembly := {
  case x if Assembly.isConfigFile(x) => MergeStrategy.concat
  case PathList(ps @ _*)
      if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
    MergeStrategy.rename
  case PathList("META-INF", xs @ _*) =>
    (xs map { _.toLowerCase }) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) |
          ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: xs)
          if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      // with the default merge strategy, the plugin was complaining with this error:
      // "deduplicate: different file contents found", all files being "io.netty.versions.properties"
      // but from different netty modules, looks likes the issue is related to having different
      // netty versions in the dependencies
      case "io.netty.versions.properties" :: Nil => MergeStrategy.discard
      case _                                     => MergeStrategy.deduplicate
    }
  case _ => MergeStrategy.deduplicate
}
