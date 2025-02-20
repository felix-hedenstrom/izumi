// DO NOT EDIT THIS FILE
// IT IS AUTOGENERATED BY `sbtgen.sc` SCRIPT
// ALL CHANGES WILL BE LOST
// https://www.scala-js.org/
addSbtPlugin("org.scala-js" % "sbt-scalajs" % PV.scala_js_version)

// https://github.com/portable-scala/sbt-crossproject
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.1")

// https://scalacenter.github.io/scalajs-bundler/
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1")

// https://github.com/scala-js/jsdependencies
addSbtPlugin("org.scala-js" % "sbt-jsdependencies" % "1.0.2")

////////////////////////////////////////////////////////////////////////////////

addSbtPlugin("io.7mind.izumi.sbt" % "sbt-izumi" % "0.0.99")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % PV.sbt_assembly)

addSbtPlugin("com.jsuereth" % "sbt-pgp" % PV.sbt_pgp)

addSbtPlugin("org.scoverage" % "sbt-scoverage" % PV.sbt_scoverage)

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % PV.sbt_unidoc)

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % PV.sbt_site)

addSbtPlugin("com.github.sbt" % "sbt-ghpages" % PV.sbt_ghpages)

addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % PV.sbt_paradox)

addSbtPlugin("io.github.jonas" % "sbt-paradox-material-theme" % PV.sbt_paradox_material_theme)

addSbtPlugin("org.scalameta" % "sbt-mdoc" % PV.sbt_mdoc)

// Ignore scala-xml version conflict between scoverage where `coursier` requires scala-xml v2
// and scoverage requires scala-xml v1 on Scala 2.12,
// introduced when updating scoverage from 1.9.3 to 2.0.5
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
