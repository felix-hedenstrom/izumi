package izumi.distage.model.definition.errors

import izumi.distage.model.definition.errors.DIError.{ConflictResolutionFailed, LoopResolutionError}
import izumi.distage.model.exceptions.planning.InjectorFailed

class DIFailureInterpreter() {
  // TODO: we need to completely get rid of exceptions, this is just some transitional stuff
  def throwOnError(issues: List[DIError]): Nothing = {
    throw asError(issues)
  }

  def asError(issues: List[DIError]): InjectorFailed = {
    import izumi.fundamentals.platform.strings.IzString.*
    lazy val conflicts = issues.collect { case c: ConflictResolutionFailed => c }
    lazy val loops = issues.collect { case e: LoopResolutionError => DIError.formatError(e) }.niceList()
    lazy val inconsistencies = issues.collect { case e: DIError.VerificationError => DIError.formatError(e) }.niceList()

    if (conflicts.nonEmpty) {
      conflictError(conflicts)
    } else if (loops.nonEmpty) {
      new InjectorFailed(s"Injector failed unexpectedly. List of issues: $loops", issues)
    } else if (inconsistencies.nonEmpty) {
      new InjectorFailed(s"Injector failed unexpectedly. List of issues: $loops", issues)
    } else {
      new InjectorFailed("BUG: Injector failed and is unable to provide any diagnostics", List.empty)
    }
  }

  protected[this] def conflictError(issues: List[ConflictResolutionFailed]): InjectorFailed = {
    val rawIssues = issues.map(_.error)
    val issueRepr = rawIssues.map(DIError.formatConflict).mkString("\n", "\n", "")

    new InjectorFailed(
      s"""Found multiple instances for a key. There must be exactly one binding for each DIKey. List of issues:$issueRepr
         |
         |You can use named instances: `make[X].named("id")` syntax and `distage.Id` annotation to disambiguate between multiple instances of the same type.
       """.stripMargin,
      rawIssues.map(DIError.ConflictResolutionFailed.apply),
    )
  }
}
