import org.scalajs.linker.interface.ModuleSplitStyle

val CCTT = "compile->compile;test->test;compile-internal->compile-internal"

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion      := "3.3.5"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

lazy val `pekmez` = project
  .in(file("."))
  .aggregate(
    `web-core`,
    `mortgage-web`
  )

lazy val `web-core` = project
  .in(file("modules/web-core"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("com.deliganli.pekmez.web.core")))
    }
  )
  .settings(
    libraryDependencies ++= Seq(
      "com.armanbilge"                        %%% "calico"                % versions.calico,
      "com.armanbilge"                        %%% "calico-router"         % versions.calico,
      "dev.optics"                            %%% "monocle-core"          % versions.monocle,
      "dev.optics"                            %%% "monocle-macro"         % versions.monocle,
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-core"   % versions.jsoniter,
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-macros" % versions.jsoniter % "compile-internal",
      "org.typelevel"                         %%% "munit-cats-effect"     % versions.munit    % Test
    )
  )

lazy val `mortgage-web` = project
  .in(file("modules/mortgage-web"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("com.deliganli.pekmez.mortgage.web")))
    }
  )
  .dependsOn(
    `web-core` % CCTT
  )

lazy val versions = new {
  val jsoniter = "2.33.3"
  val monocle  = "3.3.0"
  val calico   = "0.2.3"
  val munit    = "2.0.0"
}
