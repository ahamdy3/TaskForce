package io.ahamdy.taskforce.scheduling

import java.time.temporal.ChronoUnit
import java.time.{ZoneId, ZonedDateTime}

import com.cronutils.model.CronType
import io.ahamdy.taskforce.testing.syntax.either._
import io.ahamdy.taskforce.testing.StandardSpec

class CronLineTest extends StandardSpec {

  "CronLine" should {
    "nextExecutionTimeAfter should return" in {
      val now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES)
      val cronLine = CronLine.parse("0 * * * * ?", CronType.QUARTZ, ZoneId.of("UTC")).getRight
      cronLine.nextExecutionTimeAfter(now) mustEqual now.plusMinutes(1).withZoneSameInstant(cronLine.timeZone)
    }
  }
}
