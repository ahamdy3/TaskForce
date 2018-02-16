package io.ahamdy.taskforce.syntax

import cats.effect.IO



trait IOSyntax {

  def sequenceUnit[A](input: List[IO[A]]): IO[Unit] =
    ???
    //IO.traverse(input)(identity).map(_.toList).as(())

  def parallelSequenceUnit[A](input: List[IO[A]]): IO[Unit] =
    ???
    // IO.parallelTraverse(input)(identity).map(_.toList).as(())
}

object IO extends IOSyntax

