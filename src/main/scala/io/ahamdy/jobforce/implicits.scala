package io.ahamdy.jobforce

import io.ahamdy.jobforce.syntax.{TaskSyntax, ZonedDateTimeSyntax}

object implicits
  extends TaskSyntax
    with ZonedDateTimeSyntax
