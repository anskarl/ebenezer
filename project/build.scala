//   Copyright 2014 Commonwealth Bank of Australia
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

import sbt._, Keys._

import com.twitter.scrooge.ScroogeSBT._

import sbtassembly.AssemblyPlugin.autoImport.assembly

import au.com.cba.omnia.uniform.core.standard.StandardProjectPlugin._
import au.com.cba.omnia.uniform.core.version.UniqueVersionPlugin._
import au.com.cba.omnia.uniform.dependency.UniformDependencyPlugin._
import au.com.cba.omnia.uniform.thrift.UniformThriftPlugin._
import au.com.cba.omnia.uniform.assembly.UniformAssemblyPlugin._

import au.com.cba.omnia.humbug.HumbugSBT._

object build extends Build {
  val thermometerVersion   = "1.5.10-20170501023059-67accaf"
  val omnitoolVersion      = "1.14.9-20170710003444-8f25fcd"
  val humbugVersion        = "0.7.8-20170501020334-64f8587"
  val beeswaxVersion       = "0.1.15-20170710010323-278c3d1"

  lazy val standardSettings =
    Defaults.coreDefaultSettings ++
    uniformDependencySettings ++
    strictDependencySettings ++
    uniform.docSettings("https://github.com/CommBank/ebenezer") ++ Seq(
      logLevel in assembly := Level.Error,
      updateOptions := updateOptions.value.withCachedResolution(true),
      fork in Test := true
    )

  lazy val all = Project(
    id = "all",
    base = file("."),
    settings =
      standardSettings
        ++ uniform.project("ebenezer", "au.com.cba.omnia.ebenezer.all")
        ++ uniform.ghsettings
        ++ Seq(
          publishArtifact := false
        ),
    aggregate = Seq(core, testProject)
  )

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings =
      standardSettings
        ++ uniform.project("ebenezer", "au.com.cba.omnia.ebenezer")
        ++ uniformThriftSettings
        ++ humbugSettings
        ++ Seq(
          libraryDependencies ++=
            depend.hadoopClasspath ++ depend.hadoop() ++ depend.scalding() ++ depend.scalaz() ++
            depend.parquet() ++ depend.omnia("humbug-core", humbugVersion) ++ depend.omnia("beeswax", beeswaxVersion) ++
            depend.omnia("thermometer", thermometerVersion, "test"),
          scroogeThriftSourceFolder in Test <<= (sourceDirectory) { _ / "test" / "thrift" / "scrooge" },
          humbugThriftSourceFolder  in Test <<= (sourceDirectory) { _ / "test" / "thrift" / "humbug" },
          parallelExecution in Test := false
        )
  )

  lazy val testProject = Project(
    id = "test",
    base = file("test"),
    settings =
      standardSettings
        ++ uniform.project("ebenezer-test", "au.com.cba.omnia.ebenezer.test")
        ++ uniformThriftSettings
        ++ Seq(
          libraryDependencies ++=
            depend.hadoopClasspath ++ depend.hadoop() ++ depend.parquet() ++ depend.omnia("thermometer-hive", thermometerVersion),
          parallelExecution in Test := false
        )
  ).dependsOn(core)

  lazy val tools = Project(
    id = "tools",
    base = file("tools"),
    settings =
      standardSettings
        ++ uniform.project("ebenezer-tools", "au.com.cba.omnia.ebenezer.cli")
        ++ uniformThriftSettings
        ++ uniformAssemblySettings
        ++ Seq(
          libraryDependencies ++=
            depend.hadoopClasspath ++ depend.hadoop() ++ depend.scalaz() ++ depend.testing() ++ depend.parquet() ++ Seq(
              noHadoop("com.twitter" % "parquet-tools" % depend.versions.parquet)
                exclude("org.apache.hadoop", "hadoop-client"),
              noHadoop("io.argonaut" %% "argonaut"     % "6.1")
            )
        )
  ).dependsOn(core % "test->test")
  
  lazy val example = Project(
    id = "example",
    base = file("example"),
    settings =
      standardSettings
        ++ uniform.project("ebenezer-example", "au.com.cba.omnia.ebenezer.example")
        ++ uniformThriftSettings
        ++ uniformAssemblySettings
        ++ Seq(
          parallelExecution in Test := false,
          libraryDependencies ++=
            depend.hadoopClasspath ++ depend.hadoop() ++ depend.scalding() ++ depend.parquet() ++
            depend.omnia("thermometer-hive", thermometerVersion, "test")
        )
  ).dependsOn(core)
   .dependsOn(testProject % "test")

  // lazy val compat = Project(
  //   id = "compat",
  //   base = file("compat"),
  //   settings =
  //     standardSettings
  //       ++ uniform.project("ebenezer-compat", "au.com.cba.omnia.ebenezer.compat")
  //       ++ uniformThriftSettings
  //       ++ uniformAssemblySettings
  //       ++ Seq(
  //         parallelExecution in Test := false,
  //         libraryDependencies ++=
  //           depend.hadoopClasspath ++ depend.hadoop() ++ depend.scalding() ++ depend.parquet() ++
  //           depend.omnia("thermometer-hive", thermometerVersion)
  //       )
  // ).dependsOn(hive)
  //  .dependsOn(testProject % "test")
}
