package io.ahamdy.taskforce

import io.ahamdy.taskforce.syntax.{TaskSyntax, ZonedDateTimeSyntax}

object implicits
  extends TaskSyntax
    with ZonedDateTimeSyntax
