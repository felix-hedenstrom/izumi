package izumi.distage.modules

import izumi.distage.model.definition.{Module, ModuleDef}
import izumi.functional.quasi.{QuasiApplicative, QuasiAsync, QuasiFunctor, QuasiIO, QuasiIORunner, QuasiPrimitives, QuasiTemporal}
import izumi.distage.modules.support.*
import izumi.distage.modules.typeclass.ZIOCatsEffectInstancesModule
import izumi.functional.bio.retry.{Scheduler2, Scheduler3}
import izumi.functional.bio.{Async2, Async3, Fork2, Fork3, Local3, Primitives2, Primitives3, Temporal2, Temporal3, UnsafeRun2, UnsafeRun3}
import izumi.fundamentals.orphans.*
import izumi.fundamentals.platform.functional.Identity

import scala.annotation.unused
import izumi.reflect.{Tag, TagK, TagK3, TagKK}

/**
  * Implicitly available effect type support for `distage` resources, effects, roles & tests.
  *
  * Automatically provides default runtime environments & typeclasses instances for effect types.
  * All the defaults are overrideable via [[izumi.distage.model.definition.ModuleDef]]
  *
  *  - Adds [[izumi.functional.quasi.QuasiIO]] instances to support using effects in `Injector`, `distage-framework` & `distage-testkit-scalatest`
  *  - Adds `cats-effect` typeclass instances for effect types that have `cats-effect` instances
  *  - Adds [[izumi.functional.bio]] typeclass instances for bifunctor effect types
  *
  * Currently provides instances for
  *   - `zio`
  *   - `monix-bio`
  *   - `monix`
  *   - `cats-effect` IO
  *   - `Identity`
  *   - Any `F[_]` with `cats-effect` instances
  *   - Any `F[+_, +_]` with [[izumi.functional.bio]] instances
  *   - Any `F[-_, +_, +_]` with [[izumi.functional.bio]] instances
  *   - Any `F[_]` with [[izumi.functional.quasi.QuasiIO]] instances
  */
final case class DefaultModule[F[_]](module: Module) extends AnyVal {
  @inline def to[G[_]]: DefaultModule[G] = new DefaultModule[G](module)
}

object DefaultModule extends LowPriorityDefaultModulesInstances1 {
  @inline def apply[F[_]](implicit modules: DefaultModule[F], d: DummyImplicit): Module = modules.module

  def empty[F[_]]: DefaultModule[F] = DefaultModule(Module.empty)

  /** Empty since [[izumi.distage.modules.support.IdentitySupportModule]] is always available, even for non-Identity effects */
  implicit final def forIdentity: DefaultModule[Identity] = {
    DefaultModule.empty
  }
}

sealed trait LowPriorityDefaultModulesInstances1 extends LowPriorityDefaultModulesInstances2 {
  /**
    * This instance uses 'no more orphans' trick to provide an Optional instance
    * only IFF you have cats-effect & zio as a dependency without REQUIRING a cats-effect/zio dependency.
    *
    * Optional instance via https://blog.7mind.io/no-more-orphans.html
    *
    * This adds cats typeclass instances to the default effect module if you have `cats-effect` and `zio-interop-cats` on classpath,
    * otherwise the default effect module for ZIO will be [[forZIO]], containing BIO & QuasiIO instances, but no `cats-effect` instances.
    */
  implicit def forZIOPlusCats[K[_[_], _], A[_[_]], ZIO[_, _, _], R](
    implicit
    @unused ensureInteropCatsOnClasspath: `zio.interop.CatsIOResourceSyntax`[K],
    @unused ensureCatsEffectOnClasspath: `cats.effect.kernel.Async`[A],
    @unused isZIO: `zio.ZIO`[ZIO],
    tagR: Tag[R],
  ): DefaultModule2[ZIO[R, _, _]] = {
    DefaultModule(ZIOSupportModule[R] ++ ZIOCatsEffectInstancesModule[R])
  }
}

sealed trait LowPriorityDefaultModulesInstances2 extends LowPriorityDefaultModulesInstances3 {
  /**
    * This instance uses 'no more orphans' trick to provide an Optional instance
    * only IFF you have zio as a dependency without REQUIRING a zio dependency.
    *
    * Optional instance via https://blog.7mind.io/no-more-orphans.html
    *
    * @see [[izumi.distage.modules.support.ZIOSupportModule]]
    */
  implicit final def forZIO[ZIO[_, _, _]: `zio.ZIO`, R: Tag]: DefaultModule2[ZIO[R, _, _]] = {
    DefaultModule(ZIOSupportModule[R])
  }

//  /**
//    * This instance uses 'no more orphans' trick to provide an Optional instance
//    * only IFF you have monix-bio as a dependency without REQUIRING a monix-bio dependency.
//    *
//    * Optional instance via https://blog.7mind.io/no-more-orphans.html
//    *
//    * @see [[izumi.distage.modules.support.MonixBIOSupportModule]]
//    */
//  implicit final def forMonixBIO[BIO[_, _]: `monix.bio.IO`]: DefaultModule2[BIO] = {
//    DefaultModule(MonixBIOSupportModule)
//  }
//
//  /**
//    * This instance uses 'no more orphans' trick to provide an Optional instance
//    * only IFF you have monix as a dependency without REQUIRING a monix dependency.
//    *
//    * Optional instance via https://blog.7mind.io/no-more-orphans.html
//    *
//    * @see [[izumi.distage.modules.support.MonixSupportModule]]
//    */
//  implicit final def forMonix[Task[_]: `monix.eval.Task`]: DefaultModule[Task] = {
//    DefaultModule(MonixSupportModule)
//  }

  /**
    * This instance uses 'no more orphans' trick to provide an Optional instance
    * only IFF you have cats-effect as a dependency without REQUIRING a cats-effect dependency.
    *
    * Optional instance via https://blog.7mind.io/no-more-orphans.html
    *
    * @see [[izumi.distage.modules.support.CatsIOSupportModule]]
    */
  implicit final def forCatsIO[IO[_]: `cats.effect.IO`]: DefaultModule[IO] = {
    DefaultModule(CatsIOSupportModule)
  }
}

sealed trait LowPriorityDefaultModulesInstances3 extends LowPriorityDefaultModulesInstances4 {
  /** @see [[izumi.distage.modules.support.AnyBIO2SupportModule]] */
  implicit final def fromBIO2[F[+_, +_]: TagKK: Async2: Temporal2: UnsafeRun2: Fork2: Primitives2: Scheduler2]: DefaultModule2[F] = {
    DefaultModule(AnyBIO2SupportModule.withImplicits[F])
  }
}

sealed trait LowPriorityDefaultModulesInstances4 extends LowPriorityDefaultModulesInstances5 {
  /** @see [[izumi.distage.modules.support.AnyBIO3SupportModule]] */
  implicit final def fromBIO3[F[-_, +_, +_]: TagK3: Async3: Temporal3: Local3: UnsafeRun3: Fork3: Primitives3: Scheduler3]: DefaultModule3[F] = {
    DefaultModule(AnyBIO3SupportModule.withImplicits[F, Any])
  }
}

sealed trait LowPriorityDefaultModulesInstances5 extends LowPriorityDefaultModulesInstances6 {
  /**
    * This instance uses 'no more orphans' trick to provide an Optional instance
    * only IFF you have cats-effect as a dependency without REQUIRING a cats-effect dependency.
    *
    * Optional instance via https://blog.7mind.io/no-more-orphans.html
    */
  implicit final def fromCats[F[_], Async[_[_]]: `cats.effect.kernel.Async`, Parallel[_[_]]: `cats.Parallel`, Dispatcher[_[_]]: `cats.effect.std.Dispatcher`](
    implicit
    F0: Async[F],
    P0: Parallel[F],
    D0: Dispatcher[F],
    tagK: TagK[F],
  ): DefaultModule[F] = {
    val F = F0.asInstanceOf[cats.effect.kernel.Async[F]]
    val P = P0.asInstanceOf[cats.Parallel[F]]
    val D = D0.asInstanceOf[cats.effect.std.Dispatcher[F]]
    DefaultModule(AnyCatsEffectSupportModule.withImplicits[F](tagK, F, P, D))
  }
}

sealed trait LowPriorityDefaultModulesInstances6 {
  implicit final def fromQuasiIO[F[_]: TagK: QuasiIO: QuasiAsync: QuasiTemporal: QuasiIORunner]: DefaultModule[F] = {
    DefaultModule(new ModuleDef {
      addImplicit[QuasiFunctor[F]]
      addImplicit[QuasiApplicative[F]]
      addImplicit[QuasiPrimitives[F]]
      addImplicit[QuasiIO[F]]
      addImplicit[QuasiAsync[F]]
      addImplicit[QuasiTemporal[F]]
      addImplicit[QuasiIORunner[F]]
    })
  }
}
