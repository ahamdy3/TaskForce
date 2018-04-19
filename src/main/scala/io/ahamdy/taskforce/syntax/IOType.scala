package io.ahamdy.taskforce.syntax

import cats.implicits._
import monix.eval.Task

trait IOSyntax {

  def sequenceUnit[A](input: List[Task[A]]): Task[Unit] =
    input.sequence.as(())
    //???
    //Task.traverse(input)(identity).map(_.toList).as(())

  def parallelSequenceUnit[A](input: List[Task[A]]): Task[Unit] =
    input.parSequence.as(())
    //???
    // Task.parallelTraverse(input)(identity).map(_.toList).as(())
}

object IOType extends IOSyntax

