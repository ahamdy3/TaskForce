package io.ahamdy.taskforce.syntax

import cats.syntax.functor._
import fs2.interop.cats._
import fs2.{Strategy, Task}

trait TaskSyntax {
  implicit class TaskObjectSyntax(val task: Task.type) {
    val unit: Task[Unit] = Task.now(())
  }

  def sequenceUnit[A](input: List[Task[A]]): Task[Unit] =
    Task.traverse(input)(identity).map(_.toList).as(())

  def parallelSequenceUnit[A](input: List[Task[A]])(implicit S: Strategy): Task[Unit] =
    Task.parallelTraverse(input)(identity).map(_.toList).as(())
}

object task extends TaskSyntax

