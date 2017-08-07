package io.ahamdy.jobforce.scheduling

import java.time.temporal.ChronoUnit
import java.time.{ZoneId, ZonedDateTime}

import com.cronutils.model.CronType
import io.ahamdy.jobforce.testing.StandardSpec
import io.ahamdy.jobforce.testing.syntax.either._

class CronLineTest extends StandardSpec {

  "CronLine" should {
    "nextExecutionTimeAfter should return" in {
      val now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES)
      val cronLine = CronLine.parse("0 * * * * ?", CronType.QUARTZ, ZoneId.of("UTC")).getRight
      cronLine.nextExecutionTimeAfter(now) mustEqual now.plusMinutes(1).withZoneSameInstant(cronLine.timeZone)
    }
  }
}
