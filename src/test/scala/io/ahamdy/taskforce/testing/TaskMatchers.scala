package io.ahamdy.taskforce.testing

import fs2._
import org.specs2.matcher.MatchersImplicits._
import org.specs2.matcher._

trait TaskMatchers {

  def beFailingIO[A]: Matcher[IO[A]] = { (task: IO[A]) =>
    (task.attempt.unsafeRun().isLeft, "Task didn't fail")
  }

  def beFailingIO[A](t: Throwable): Matcher[IO[A]] = { (task: IO[A]) =>
    task.attempt.unsafeRun() match {
      case Left(err) => (err.getMessage == t.getMessage, s"Task is failing with $err not with expected: $t")
      case Right(a) => (false, s"Task is not failing and returning value $a")
    }
  }

  def beSucceedingIO[A](value: A): Matcher[IO[A]] = { (task: IO[A]) =>
    task.attempt.unsafeRun() match {
      case Left(err) => (false, s"Task is failing with $err")
      case Right(a) => (a == value, s"$a is not equal to $value")
    }
  }

  def beSucceedingTaskLike[A](pattern: PartialFunction[A, MatchResult[_]]): Matcher[IO[A]] =
    new Matcher[IO[A]] {
      def apply[S <: IO[A]](t: Expectable[S]): MatchResult[S] =
        t.value.attempt.unsafeRun() match {
          case Left(err) => failure(s"Task is failing with ${err.getMessage}", t)
          case Right(a) => if (pattern.isDefinedAt(a)) result(pattern.apply(a), t) else failure("Pattern failed for task result", t)
        }
    }

  def beSucceedingIO[A]: Matcher[IO[A]] = { (task: IO[A]) =>
    task.attempt.unsafeRun() match {
      case Left(err) => (false, s"Task is failing with $err")
      case Right(_) => (true, "")
    }
  }
}
