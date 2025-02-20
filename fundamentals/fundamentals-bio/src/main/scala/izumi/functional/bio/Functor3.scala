package izumi.functional.bio

import scala.annotation.unused

trait Functor3[F[-_, +_, +_]] extends RootBifunctor[F] {
  def map[R, E, A, B](r: F[R, E, A])(f: A => B): F[R, E, B]

  def as[R, E, A, B](r: F[R, E, A])(v: => B): F[R, E, B] = map(r)(_ => v)
  def void[R, E, A](r: F[R, E, A]): F[R, E, Unit] = map(r)(_ => ())

  /** Extracts the optional value, or returns the given `valueOnNone` value */
  def fromOptionOr[R, E, A](valueOnNone: => A, r: F[R, E, Option[A]]): F[R, E, A] = map(r)(_.getOrElse(valueOnNone))

  @inline final def widen[R, E, A, A1](r: F[R, E, A])(implicit @unused ev: A <:< A1): F[R, E, A1] = r.asInstanceOf[F[R, E, A1]]
}
