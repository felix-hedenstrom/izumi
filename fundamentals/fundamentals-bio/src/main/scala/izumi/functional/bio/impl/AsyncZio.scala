package izumi.functional.bio.impl

import izumi.functional.bio.Exit.ZIOExit
import izumi.functional.bio.data.{Morphism3, RestoreInterruption3}
import izumi.functional.bio.{Async3, Exit, Fiber2, Fiber3, __PlatformSpecific}
import izumi.fundamentals.platform.language.Quirks.Discarder
import zio._izumicompat_.{__ZIORaceCompat, __ZIOWithFiberRuntime}
import zio.internal.stacktracer.{InteropTracer, Tracer}
import zio.stacktracer.TracingImplicits.disableAutoTrace
import zio.{ZEnvironment, ZIO}

import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object AsyncZio extends AsyncZio

open class AsyncZio extends Async3[ZIO] /*with Local3[ZIO]*/ {
  @inline override final def InnerF: this.type = this

  @inline override final def unit: ZIO[Any, Nothing, Unit] = ZIO.unit
  @inline override final def pure[A](a: A): ZIO[Any, Nothing, A] = ZIO.succeed(a)(Tracer.instance.empty)
  @inline override final def sync[A](effect: => A): ZIO[Any, Nothing, A] = {
    val byName: () => A = () => effect
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    ZIO.succeed(effect)
  }
  @inline override final def syncThrowable[A](effect: => A): ZIO[Any, Throwable, A] = {
    val byName: () => A = () => effect
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    ZIO.attempt(effect)
  }
  @inline override final def suspend[R, A](effect: => ZIO[R, Throwable, A]): ZIO[R, Throwable, A] = {
    val byName: () => ZIO[R, Throwable, A] = () => effect
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    ZIO.suspend(effect)
  }

  @inline override final def fail[E](v: => E): ZIO[Any, E, Nothing] = {
    val byName: () => E = () => v
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    ZIO.fail(v)
  }
  @inline override final def terminate(v: => Throwable): ZIO[Any, Nothing, Nothing] = {
    val byName: () => Throwable = () => v
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    ZIO.die(v)
  }

  @inline override final def fromEither[E, A](effect: => Either[E, A]): ZIO[Any, E, A] = {
    val byName: () => Either[E, A] = () => effect
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    ZIO.fromEither(effect)
  }
  @inline override final def fromOption[E, A](errorOnNone: => E)(effect: => Option[A]): ZIO[Any, E, A] = {
    val byName: () => Option[A] = () => effect
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    ZIO.fromEither(effect.toRight(errorOnNone))
  }
  @inline override final def fromTry[A](effect: => Try[A]): ZIO[Any, Throwable, A] = {
    val byName: () => Try[A] = () => effect
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    ZIO.fromTry(effect)
  }

  @inline override final def void[R, E, A](r: ZIO[R, E, A]): ZIO[R, E, Unit] = r.unit(Tracer.instance.empty)
  @inline override final def map[R, E, A, B](r: ZIO[R, E, A])(f: A => B): ZIO[R, E, B] = r.map(f)(InteropTracer.newTrace(f))
  @inline override final def as[R, E, A, B](r: ZIO[R, E, A])(v: => B): ZIO[R, E, B] = {
    val byName: () => B = () => v
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    r.as(v)
  }

  @inline override final def tapError[R, E, A, E1 >: E](r: ZIO[R, E, A])(f: E => ZIO[R, E1, Unit]): ZIO[R, E1, A] = r.tapError(f)(implicitly, InteropTracer.newTrace(f))
  @inline override final def leftMap[R, E, A, E2](r: ZIO[R, E, A])(f: E => E2): ZIO[R, E2, A] = r.mapError(f)(implicitly, InteropTracer.newTrace(f))
  @inline override final def leftFlatMap[R, E, A, E2](r: ZIO[R, E, A])(f: E => ZIO[R, Nothing, E2]): ZIO[R, E2, A] =
    r.flatMapError(f)(implicitly, InteropTracer.newTrace(f))
  @inline override final def flip[R, E, A](r: ZIO[R, E, A]): ZIO[R, A, E] = r.flip(Tracer.instance.empty)
  @inline override final def bimap[R, E, A, E2, B](r: ZIO[R, E, A])(f: E => E2, g: A => B): ZIO[R, E2, B] = r.mapBoth(f, g)(implicitly, InteropTracer.newTrace(f))

  @inline override final def flatMap[R, E, A, B](r: ZIO[R, E, A])(f0: A => ZIO[R, E, B]): ZIO[R, E, B] = r.flatMap(f0)(InteropTracer.newTrace(f0))
  @inline override final def tap[R, E, A](r: ZIO[R, E, A], f: A => ZIO[R, E, Unit]): ZIO[R, E, A] = r.tap(f)(InteropTracer.newTrace(f))
  @inline override final def tapBoth[R, E, A, E1 >: E](r: ZIO[R, E, A])(err: E => ZIO[R, E1, Unit], succ: A => ZIO[R, E1, Unit]): ZIO[R, E1, A] =
    r.tapBoth(err, succ)(implicitly, InteropTracer.newTrace(err))
  @inline override final def flatten[R, E, A](r: ZIO[R, E, ZIO[R, E, A]]): ZIO[R, E, A] = ZIO.flatten(r)(Tracer.instance.empty)
  @inline override final def *>[R, E, A, B](f: ZIO[R, E, A], next: => ZIO[R, E, B]): ZIO[R, E, B] = {
    val byName: () => ZIO[R, E, B] = () => next
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    f *> next
  }
  @inline override final def <*[R, E, A, B](f: ZIO[R, E, A], next: => ZIO[R, E, B]): ZIO[R, E, A] = {
    val byName: () => ZIO[R, E, B] = () => next
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    f <* next
  }
  @inline override final def map2[R, E, A, B, C](r1: ZIO[R, E, A], r2: => ZIO[R, E, B])(f: (A, B) => C): ZIO[R, E, C] = {
    val byName: () => ZIO[R, E, B] = () => r2
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    r1.zipWith(r2)(f)
  }

  @inline override final def iterateWhile[R, E, A](r: ZIO[R, E, A])(p: A => Boolean): ZIO[R, E, A] = r.repeatWhile(p)(InteropTracer.newTrace(p))
  @inline override final def iterateUntil[R, E, A](r: ZIO[R, E, A])(p: A => Boolean): ZIO[R, E, A] = r.repeatUntil(p)(InteropTracer.newTrace(p))

  @inline override final def leftMap2[R, E, A, E2, E3](firstOp: ZIO[R, E, A], secondOp: => ZIO[R, E2, A])(f: (E, E2) => E3): ZIO[R, E3, A] = {
    val byName: () => ZIO[R, E2, A] = () => secondOp
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    firstOp.catchAll(e => secondOp.mapError(f(e, _)))
  }
  @inline override final def orElse[R, E, A, E2](r: ZIO[R, E, A], f: => ZIO[R, E2, A]): ZIO[R, E2, A] = {
    val byName: () => ZIO[R, E2, A] = () => f
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    r.orElse(f)
  }

  @inline override final def redeem[R, E, A, E2, B](r: ZIO[R, E, A])(err: E => ZIO[R, E2, B], succ: A => ZIO[R, E2, B]): ZIO[R, E2, B] =
    r.foldZIO(err, succ)(implicitly, InteropTracer.newTrace(err))
  @inline override final def catchAll[R, E, A, E2](r: ZIO[R, E, A])(f: E => ZIO[R, E2, A]): ZIO[R, E2, A] = r.catchAll(f)(implicitly, InteropTracer.newTrace(f))
  @inline override final def catchSome[R, E, A, E1 >: E](r: ZIO[R, E, A])(f: PartialFunction[E, ZIO[R, E1, A]]): ZIO[R, E1, A] =
    r.catchSome(f)(implicitly, InteropTracer.newTrace(f))

  @inline override final def guarantee[R, E, A](f: ZIO[R, E, A], cleanup: ZIO[R, Nothing, Unit]): ZIO[R, E, A] = f.ensuring(cleanup)(Tracer.instance.empty)
  @inline override final def attempt[R, E, A](r: ZIO[R, E, A]): ZIO[R, Nothing, Either[E, A]] = r.either(implicitly, Tracer.instance.empty)
  @inline override final def redeemPure[R, E, A, B](r: ZIO[R, E, A])(err: E => B, succ: A => B): ZIO[R, Nothing, B] =
    r.fold(err, succ)(implicitly, InteropTracer.newTrace(err))

  @inline override final def retryWhile[R, E, A](r: ZIO[R, E, A])(f: E => Boolean): ZIO[R, E, A] = r.retryWhile(f)(implicitly, InteropTracer.newTrace(f))
  @inline override final def retryWhileF[R, R1 <: R, E, A](r: ZIO[R, E, A])(f: E => ZIO[R1, Nothing, Boolean]): ZIO[R1, E, A] =
    r.retryWhileZIO(f)(implicitly, InteropTracer.newTrace(f))

  @inline override final def retryUntil[R, E, A](r: ZIO[R, E, A])(f: E => Boolean): ZIO[R, E, A] = r.retryUntil(f)(implicitly, InteropTracer.newTrace(f))
  @inline override final def retryUntilF[R, R1 <: R, E, A](r: ZIO[R, E, A])(f: E => ZIO[R1, Nothing, Boolean]): ZIO[R1, E, A] =
    r.retryUntilZIO(f)(implicitly, InteropTracer.newTrace(f))

  @inline override final def fromOptionOr[R, E, A](valueOnNone: => A, r: ZIO[R, E, Option[A]]): ZIO[R, E, A] = {
    val byName: () => A = () => valueOnNone
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    r.someOrElse(valueOnNone)
  }

  @inline override final def fromOptionF[R, E, A](fallbackOnNone: => ZIO[R, E, A], r: ZIO[R, E, Option[A]]): ZIO[R, E, A] = {
    val byName: () => ZIO[R, E, A] = () => fallbackOnNone
    implicit val trace: zio.Trace = InteropTracer.newTrace(byName)

    r.someOrElseZIO(fallbackOnNone)
  }

  @inline override final def bracket[R, E, A, B](acquire: ZIO[R, E, A])(release: A => ZIO[R, Nothing, Unit])(use: A => ZIO[R, E, B]): ZIO[R, E, B] = {
    ZIO.acquireReleaseWith(acquire)(release)(use)(InteropTracer.newTrace(release))
  }
  @inline override final def bracketCase[R, E, A, B](
    acquire: ZIO[R, E, A]
  )(release: (A, Exit[E, B]) => ZIO[R, Nothing, Unit]
  )(use: A => ZIO[R, E, B]
  ): ZIO[R, E, B] = {
    implicit val trace: zio.Trace = InteropTracer.newTrace(release)

    ZIO.acquireReleaseExitWith[R, E, A](acquire)((a, exit: zio.Exit[E, B]) => ZIOExit.withIsInterruptedF(i => release(a, ZIOExit.toExit(exit)(i))))(use)
  }
  @inline override final def guaranteeCase[R, E, A](f: ZIO[R, E, A], cleanup: Exit[E, A] => ZIO[R, Nothing, Unit]): ZIO[R, E, A] = {
    implicit val trace: zio.Trace = InteropTracer.newTrace(cleanup)

    f.onExit(exit => ZIOExit.withIsInterruptedF(cleanup apply ZIOExit.toExit(exit)(_)))
  }

  @inline override final def traverse[R, E, A, B](l: Iterable[A])(f: A => ZIO[R, E, B]): ZIO[R, E, List[B]] =
    ZIO.foreach(l.toList)(f)(implicitly, InteropTracer.newTrace(f))
  @inline override final def sequence[R, E, A](l: Iterable[ZIO[R, E, A]]): ZIO[R, E, List[A]] = ZIO.collectAll(l.toList)(implicitly, Tracer.instance.empty)
  @inline override final def traverse_[R, E, A](l: Iterable[A])(f: A => ZIO[R, E, Unit]): ZIO[R, E, Unit] = ZIO.foreachDiscard(l)(f)(InteropTracer.newTrace(f))
  @inline override final def sequence_[R, E](l: Iterable[ZIO[R, E, Unit]]): ZIO[R, E, Unit] = ZIO.foreachDiscard(l)(identity)(Tracer.instance.empty)

  @inline override final def sandbox[R, E, A](r: ZIO[R, E, A]): ZIO[R, Exit.Failure[E], A] = {
    implicit val trace: zio.Trace = Tracer.instance.empty

    r.sandbox.flatMapError(ZIOExit withIsInterrupted ZIOExit.toExit(_))
  }

  @inline override final def yieldNow: ZIO[Any, Nothing, Unit] = ZIO.yieldNow(Tracer.instance.empty)
  @inline override final def never: ZIO[Any, Nothing, Nothing] = ZIO.never(Tracer.instance.empty)

  @inline override final def async[E, A](register: (Either[E, A] => Unit) => Unit): ZIO[Any, E, A] = {
    implicit val trace: zio.Trace = InteropTracer.newTrace(register)

    ZIO.async(cb => register(cb apply _.fold(ZIO.fail(_), ZIO.succeed(_))))
  }
  @inline override final def asyncF[R, E, A](register: (Either[E, A] => Unit) => ZIO[R, E, Unit]): ZIO[R, E, A] = {
    implicit val trace: zio.Trace = InteropTracer.newTrace(register)

    ZIO.asyncZIO(cb => register(cb apply _.fold(ZIO.fail(_), ZIO.succeed(_))))
  }
  @inline override final def asyncCancelable[E, A](register: (Either[E, A] => Unit) => Canceler): ZIO[Any, E, A] = {
    implicit val trace: zio.Trace = InteropTracer.newTrace(register)

    ZIO.asyncInterrupt[Any, E, A] {
      cb =>
        val canceler = register(cb apply _.fold(ZIO.fail(_), ZIO.succeed(_)))
        Left(canceler)
    }
  }

  @inline override final def fromFuture[A](mkFuture: ExecutionContext => Future[A]): ZIO[Any, Throwable, A] = {
    ZIO.fromFuture(mkFuture)(InteropTracer.newTrace(mkFuture))
  }
  @inline override final def fromFutureJava[A](javaFuture: => CompletionStage[A]): ZIO[Any, Throwable, A] = {
    __PlatformSpecific.fromFutureJava(javaFuture)
  }

  @inline override final def uninterruptible[R, E, A](r: ZIO[R, E, A]): ZIO[R, E, A] = r.uninterruptible(Tracer.instance.empty)

  @inline override final def race[R, E, A](r1: ZIO[R, E, A], r2: ZIO[R, E, A]): ZIO[R, E, A] = {
    implicit val trace: zio.Trace = Tracer.instance.empty

    __ZIORaceCompat.raceFirst(r1.interruptible, r2.interruptible)
  }

  @inline override final def racePairUnsafe[R, E, A, B](
    r1: ZIO[R, E, A],
    r2: ZIO[R, E, B],
  ): ZIO[R, E, Either[(Exit[E, A], Fiber3[ZIO, E, B]), (Fiber3[ZIO, E, A], Exit[E, B])]] = {
    implicit val trace: zio.Trace = Tracer.instance.empty

    val interrupted1 = new AtomicBoolean(true)
    val interrupted2 = new AtomicBoolean(true)
    __ZIORaceCompat.raceWith(
      ZIOExit.ZIOSignalOnNoExternalInterruptFailure(r1.interruptible)(sync(interrupted1.set(false))),
      ZIOExit.ZIOSignalOnNoExternalInterruptFailure(r2.interruptible)(sync(interrupted2.set(false))),
    )(
      { case (l, f) => ZIO.succeed(Left((ZIOExit.toExit(l)(interrupted1.get()), Fiber2.fromZIO(sync(interrupted2.get()))(f)))) },
      { case (r, f) => ZIO.succeed(Right((Fiber2.fromZIO(sync(interrupted1.get()))(f), ZIOExit.toExit(r)(interrupted2.get())))) },
    )
  }

  @inline override final def parTraverseN[R, E, A, B](maxConcurrent: Int)(l: Iterable[A])(f: A => ZIO[R, E, B]): ZIO[R, E, List[B]] = {
    implicit val trace: zio.Trace = InteropTracer.newTrace(f)

    ZIO
      .foreachPar(l.toList)(f(_).interruptible)
      .withParallelism(maxConcurrent)
  }
  @inline override final def parTraverseN_[R, E, A, B](maxConcurrent: Int)(l: Iterable[A])(f: A => ZIO[R, E, B]): ZIO[R, E, Unit] = {
    implicit val trace: zio.Trace = InteropTracer.newTrace(f)

    ZIO
      .foreachParDiscard(l)(f(_).interruptible)
      .withParallelism(maxConcurrent)
  }
  @inline override final def parTraverseNCore[R, E, A, B](l: Iterable[A])(f: A => ZIO[R, E, B]): ZIO[R, E, List[B]] = {
    ZIO.suspendSucceed(parTraverseN(java.lang.Runtime.getRuntime.availableProcessors() max 2)(l)(f))(InteropTracer.newTrace(f))
  }
  @inline override final def parTraverseNCore_[R, E, A, B](l: Iterable[A])(f: A => ZIO[R, E, B]): ZIO[R, E, Unit] = {
    ZIO.suspendSucceed(parTraverseN_(java.lang.Runtime.getRuntime.availableProcessors() max 2)(l)(f))(InteropTracer.newTrace(f))
  }
  @inline override final def parTraverse[R, E, A, B](l: Iterable[A])(f: A => ZIO[R, E, B]): ZIO[R, E, List[B]] = {
    implicit val trace: zio.Trace = InteropTracer.newTrace(f)

    // do not force unlimited parallelism here, obey 'regional parallelism' (unlimited by default)
    ZIO.foreachPar(l.toList)(f(_).interruptible)
  }
  @inline override final def parTraverse_[R, E, A, B](l: Iterable[A])(f: A => ZIO[R, E, B]): ZIO[R, E, Unit] = {
    implicit val trace: zio.Trace = InteropTracer.newTrace(f)

    // do not force unlimited parallelism here, obey 'regional parallelism' (unlimited by default)
    ZIO.foreachParDiscard(l)(f(_).interruptible)
  }

  @inline override final def zipWithPar[R, E, A, B, C](fa: ZIO[R, E, A], fb: ZIO[R, E, B])(f: (A, B) => C): ZIO[R, E, C] = {
    fa.zipWithPar(fb)(f)(InteropTracer.newTrace(f))
  }
  @inline override final def zipPar[R, E, A, B](fa: ZIO[R, E, A], fb: ZIO[R, E, B]): ZIO[R, E, (A, B)] = {
    fa.<&>(fb)(zio.Zippable.Zippable2[A, B], Tracer.instance.empty)
  }
  @inline override final def zipParLeft[R, E, A, B](fa: ZIO[R, E, A], fb: ZIO[R, E, B]): ZIO[R, E, A] = {
    fa.<&(fb)(Tracer.instance.empty)
  }
  @inline override final def zipParRight[R, E, A, B](fa: ZIO[R, E, A], fb: ZIO[R, E, B]): ZIO[R, E, B] = {
    fa.&>(fb)(Tracer.instance.empty)
  }

  // Trifunctor implementations with Tag evidence
  // Probably unworkable at all: we end up floating a combinatoric explosion of evidences for every type we encounter
  def andThen[R, R1: zio.Tag, E, A](f: ZIO[R, E, R1], g: ZIO[R1, E, A]): ZIO[R, E, A] = {
    implicit val trace: zio.Trace = Tracer.instance.empty

    f.flatMap(r1 => g.provideEnvironment(ZEnvironment(r1)))
  }
  def choice[RL: zio.Tag, RR: zio.Tag, E, A](f: ZIO[RL, E, A], g: ZIO[RR, E, A])(implicit t: zio.Tag[Either[RL, RR]]): ZIO[Either[RL, RR], E, A] = {
    implicit val trace: zio.Trace = Tracer.instance.empty

    ZIO.serviceWithZIO[Either[RL, RR]] {
      case Left(l) => f.provideEnvironment(ZEnvironment(l))
      case Right(r) => g.provideEnvironment(ZEnvironment(r))
    }
  }
  //

  // Reader methods are impossible to implement as-is, only with a newtype (essentially a transformer, because a boundary will be required[?])
//  @inline override final def ask[R]: ZIO[R, Nothing, R] = ZIO.environment
//  @inline override final def askWith[R, A](f: R => A): ZIO[R, Nothing, A] = ZIO.environmentWith(f)
//
//  @inline override final def provide[R, E, A](fr: ZIO[R, E, A])(r: => R): ZIO[Any, E, A] = fr.provideEnvironment(ZEnvironment(r))
//  @inline override final def contramap[R, E, A, R0](fr: ZIO[R, E, A])(f: R0 => R): ZIO[R0, E, A] = fr.provideSome(f)
//
//  @inline override final def access[R, E, A](f: R => ZIO[R, E, A]): ZIO[R, E, A] = ZIO.environmentWithZIO(f)
//
//  @inline override final def dimap[R1, E, A1, R2, A2](fab: ZIO[R1, E, A1])(f: R2 => R1)(g: A1 => A2): ZIO[R2, E, A2] = fab.provideSome(f).map(g)
//
//  @inline override final def andThen[R, R1, E, A](f: ZIO[R, E, R1], g: ZIO[R1, E, A]): ZIO[R, E, A] = f >>> g
//
//  @inline override final def asking[R, E, A](f: ZIO[R, E, A]): ZIO[R, E, (A, R)] = f.onFirst
//
//  @inline override final def choice[RL, RR, E, A](f: ZIO[RL, E, A], g: ZIO[RR, E, A]): ZIO[Either[RL, RR], E, A] = (f +++ g).map(_.merge)
//  @inline override final def choose[RL, RR, E, AL, AR](f: ZIO[RL, E, AL], g: ZIO[RR, E, AR]): ZIO[Either[RL, RR], E, Either[AL, AR]] = f +++ g

  @inline override final def sendInterruptToSelf: ZIO[Any, Nothing, Unit] = {
    implicit val trace: zio.Trace = Tracer.instance.empty

    def loopUntilInterrupted: ZIO[Any, Nothing, Unit] =
      ZIO.descriptorWith(d => if (d.interrupters.isEmpty) ZIO.yieldNow *> loopUntilInterrupted else ZIO.unit)

    for {
      _ <- __ZIOWithFiberRuntime.ZIOWithFiberRuntime[Any, Nothing, Unit]((thisFiber, _) => thisFiber.interruptAsFork(thisFiber.id))
      _ <- loopUntilInterrupted
    } yield ()
  }

  @inline override final def currentEC: ZIO[Any, Nothing, ExecutionContext] = ZIO.executor(Tracer.instance.empty).map(_.asExecutionContext)(Tracer.instance.empty)
  @inline override final def onEC[R, E, A](ec: ExecutionContext)(f: ZIO[R, E, A]): ZIO[R, E, A] = f.onExecutionContext(ec)(Tracer.instance.empty)

  @inline override final def uninterruptibleExcept[R, E, A](r: Morphism3[ZIO, ZIO] => ZIO[R, E, A]): ZIO[R, E, A] = {
    implicit val trace: zio.Trace = InteropTracer.newTrace(r)

    ZIO.uninterruptibleMask {
      restore =>
        val restoreMorphism: Morphism3[ZIO, ZIO] = Morphism3(restore(_))
        r(restoreMorphism)
    }
  }

  @inline override final def bracketExcept[R, E, A, B](
    acquire: RestoreInterruption3[ZIO] => ZIO[R, E, A]
  )(release: (A, Exit[E, B]) => ZIO[R, Nothing, Unit]
  )(use: A => ZIO[R, E, B]
  ): ZIO[R, E, B] = {
    implicit val trace: zio.Trace = InteropTracer.newTrace(acquire)

    ZIO.uninterruptibleMask[R, E, B] {
      restore =>
        val restoreMorphism: Morphism3[ZIO, ZIO] = Morphism3(restore(_))
        acquire(restoreMorphism).flatMap {
          a =>
            ZIO
              .suspendSucceed(restore(use(a)))
              .exit
              .flatMap {
                e =>
                  ZIOExit
                    .withIsInterruptedF(i => release(a, Exit.ZIOExit.toExit(e)(i)))
                    .foldCauseZIO(
                      cause2 => ZIO.refailCause(e.foldExit(_ ++ cause2, _ => cause2)),
                      _ => ZIO.done(e),
                    )
              }
        }
    }
  }

  disableAutoTrace.discard()
}
