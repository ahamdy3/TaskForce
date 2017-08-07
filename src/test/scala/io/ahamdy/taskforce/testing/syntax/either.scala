package io.ahamdy.taskforce.testing.syntax

trait EitherSyntax {
  implicit class EitherWrapper[E,A](either: Either[E,A]) {
    def getRight: A = either.toOption.get
  }
}

object either extends EitherSyntax
