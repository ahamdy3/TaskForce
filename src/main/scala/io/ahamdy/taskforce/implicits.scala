package io.ahamdy.taskforce

import io.ahamdy.taskforce.syntax.{IOSyntax, ZonedDateTimeSyntax}

object implicits
  extends IOSyntax
    with ZonedDateTimeSyntax
