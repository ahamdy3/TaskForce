package io.ahamdy.taskforce.testing

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.specs2.matcher.MatchersImplicits._
import org.specs2.matcher._

import scala.concurrent.duration._

trait TaskMatchers {

  def beFailingIO[A]: Matcher[Task[A]] = { (io: Task[A]) =>
    (io.attempt.runSyncUnsafe(1.minute).isLeft, "Task didn't fail")
  }

  def beFailingIO[A](t: Throwable): Matcher[Task[A]] = { (io: Task[A]) =>
    io.attempt.runSyncUnsafe(1.minute) match {
      case Left(err) => (err.getMessage == t.getMessage, s"Task is failing with $err not with expected: $t")
      case Right(a) => (false, s"Task is not failing and returning value $a")
    }
  }

  def beSucceedingIO[A](value: A): Matcher[Task[A]] = { (io: Task[A]) =>
    io.attempt.runSyncUnsafe(1.minute) match {
      case Left(err) => (false, s"Task is failing with $err")
      case Right(a) => (a == value, s"$a is not equal to $value")
    }
  }

  def beSucceedingIOLike[A](pattern: PartialFunction[A, MatchResult[_]]): Matcher[Task[A]] =
    new Matcher[Task[A]] {
      def apply[S <: Task[A]](t: Expectable[S]): MatchResult[S] =
        t.value.attempt.runSyncUnsafe(1.minute) match {
          case Left(err) => failure(s"Task is failing with ${err.getMessage}", t)
          case Right(a) => if (pattern.isDefinedAt(a)) result(pattern.apply(a), t) else failure("Pattern failed for Task result", t)
        }
    }

  def beSucceedingIO[A]: Matcher[Task[A]] = { (io: Task[A]) =>
    io.attempt.runSyncUnsafe(1.minute) match {
      case Left(err) => (false, s"Task is failing with $err")
      case Right(_) => (true, "")
    }
  }
}
