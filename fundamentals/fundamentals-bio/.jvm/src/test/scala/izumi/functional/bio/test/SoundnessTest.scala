package izumi.functional.bio.test

import izumi.functional.bio._
import org.scalatest.exceptions.TestFailedException
import org.scalatest.wordspec.AnyWordSpec

class SoundnessTest extends AnyWordSpec {
  /*
    There's a `<: RootBifunctor[f]` constraint now in Convert3To2
    to prevent trifunctorial classes from being converted to bifunctorial,
    but it's still circumventable by inheriting from Functor + Trifunctor
    hierarchies in one implicit instance, like `Async3[F] with Local3[F]`.

    A solution may be to add a marker type member inside instances:

    trait FunctorialityHelper {
      type Functoriality
    }
    object FunctorialityHelper {
      type Monofunctorial
      type Bifunctorial <: Monofunctorial
      type Trifunctorial <: Bifunctorial
    }

    And then add a LUB guard to check that type member is _no more specific_
    than `Bifunctorial`: `(implicit guard: Lub[F#Functoriality, Trifunctorial, Bifunctorial])`
   */

  type BIOArrow2[F[+_, +_]] = Arrow3[λ[(`-R`, `+E`, `+A`) => F[E, A]]]
  type FR[F[-_, +_, +_], R] = { type l[+E, +A] = F[R, E, A] }

  "Cannot convert polymorphic BIOArrow into a bifunctor typeclass (normally)" in {
    val res = intercept[TestFailedException](assertCompiles("""
      def valueF[F[-_, +_, +_]: Arrow3: MonadAsk3: IO3] = {
        val FA: BIOArrow2[FR[F, Int]#l] = implicitly[BIOArrow2[FR[F, Int]#l]]
        FA.andThen(F.unit, F.access((i: Int) => F.sync(println(i))))
      }
      implicit val u: zio.Unsafe = zio.Unsafe.unsafe(identity)
      zio.Runtime.default.unsafe.run(valueF[zio.ZIO].provideEnvironment(zio.ZEnvironment(1)))
      """))
    assert(!res.getMessage.contains("not found type"))
    assert(res.getMessage.contains("implicit error") || res.getMessage.contains("No given instance") || res.getMessage.contains("implicit value"))
    assert(res.getMessage contains "BIOArrow2")
  }

  "Cannot convert ZIO BIOArrow instance into a bifunctor typeclass (normally)" in {
    val res = intercept[TestFailedException](assertCompiles("""
    def valueZIO = {
      val F: BIOArrow2[FR[zio.ZIO, Int]#l] = implicitly[BIOArrow2[FR[zio.ZIO, Int]#l]]
      F.andThen(zio.ZIO.unit, zio.ZIO.environmentWithZIO[Int](i => zio.ZIO.attempt(println(i))))
    }
    implicit val u: zio.Unsafe = zio.Unsafe.unsafe(identity)
    zio.Runtime.default.unsafe.run(valueZIO.provideEnvironment(zio.ZEnvironment(1)))
    """))
    assert(!res.getMessage.contains("not found type"))
    assert(res.getMessage.contains("implicit error") || res.getMessage.contains("No given instance") || res.getMessage.contains("implicit value"))
    assert(res.getMessage contains "BIOArrow2")
  }
}
