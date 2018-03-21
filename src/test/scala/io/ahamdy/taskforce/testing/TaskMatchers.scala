package io.ahamdy.taskforce.testing

import cats.effect.IO
import org.specs2.matcher.MatchersImplicits._
import org.specs2.matcher._

trait TaskMatchers {

  def beFailingIO[A]: Matcher[IO[A]] = { (io: IO[A]) =>
    (io.attempt.unsafeRunSync().isLeft, "IO didn't fail")
  }

  def beFailingIO[A](t: Throwable): Matcher[IO[A]] = { (io: IO[A]) =>
    io.attempt.unsafeRunSync() match {
      case Left(err) => (err.getMessage == t.getMessage, s"IO is failing with $err not with expected: $t")
      case Right(a) => (false, s"IO is not failing and returning value $a")
    }
  }

  def beSucceedingIO[A](value: A): Matcher[IO[A]] = { (io: IO[A]) =>
    io.attempt.unsafeRunSync() match {
      case Left(err) => (false, s"IO is failing with $err")
      case Right(a) => (a == value, s"$a is not equal to $value")
    }
  }

  def beSucceedingIOLike[A](pattern: PartialFunction[A, MatchResult[_]]): Matcher[IO[A]] =
    new Matcher[IO[A]] {
      def apply[S <: IO[A]](t: Expectable[S]): MatchResult[S] =
        t.value.attempt.unsafeRunSync() match {
          case Left(err) => failure(s"IO is failing with ${err.getMessage}", t)
          case Right(a) => if (pattern.isDefinedAt(a)) result(pattern.apply(a), t) else failure("Pattern failed for IO result", t)
        }
    }

  def beSucceedingIO[A]: Matcher[IO[A]] = { (io: IO[A]) =>
    io.attempt.unsafeRunSync() match {
      case Left(err) => (false, s"IO is failing with $err")
      case Right(_) => (true, "")
    }
  }
}
