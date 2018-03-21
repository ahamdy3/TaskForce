package io.ahamdy.taskforce.syntax

import cats.effect.{IO => CatsIO}



trait IOSyntax {

  def sequenceUnit[A](input: List[CatsIO[A]]): CatsIO[Unit] =
    ???
    //IO.traverse(input)(identity).map(_.toList).as(())

  def parallelSequenceUnit[A](input: List[CatsIO[A]]): CatsIO[Unit] =
    ???
    // IO.parallelTraverse(input)(identity).map(_.toList).as(())
}

object IOType extends IOSyntax

