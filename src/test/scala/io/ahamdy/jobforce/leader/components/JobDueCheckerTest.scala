package io.ahamdy.jobforce.leader.components

import java.time.temporal.ChronoUnit
import java.time.{ZoneId, ZonedDateTime}

import scala.concurrent.duration._
import com.cronutils.model.CronType
import io.ahamdy.jobforce.domain.JobSchedule
import io.ahamdy.jobforce.scheduling.CronLine
import io.ahamdy.jobforce.testing.StandardSpec
import io.ahamdy.jobforce.syntax.zonedDateTime._
import io.ahamdy.jobforce.testing.syntax.either._


class JobDueCheckerTest extends StandardSpec {
  val now: ZonedDateTime = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS).plusMinutes(30)
  "JobDueChecker" should {
    "isDue should return true only if job next execution time is within startTimeWindow" in {
      JobDueChecker.isDue(
        JobSchedule(CronLine.parse("0 * * * * ?", CronType.QUARTZ, ZoneId.of("UTC")).getRight, 2.minute),
        now = now,
        actualLastTimeRun = now.minus(2.minutes)) must beTrue

      JobDueChecker.isDue(
        JobSchedule(CronLine.parse("*/2 * * * *", CronType.UNIX, ZoneId.of("UTC")).getRight, 3.minute),
        now = now,
        actualLastTimeRun = now.minus(2.minutes)) must beTrue

      JobDueChecker.isDue(
        JobSchedule(CronLine.parse("* 0 0/1 ? * * *", CronType.QUARTZ, ZoneId.of("UTC")).getRight, 2.minute),
        now = now,
        actualLastTimeRun = now.minus(2.minutes)) must beFalse

      JobDueChecker.isDue(
        JobSchedule(CronLine.parse("* 20 0 ? * * *", CronType.QUARTZ, ZoneId.of("UTC")).getRight, 11.minute),
        now = now,
        actualLastTimeRun = now.minus(1.day)) must beTrue

      JobDueChecker.isDue(
        JobSchedule(CronLine.parse("* 20 0 ? * * *", CronType.QUARTZ, ZoneId.of("UTC")).getRight, 11.minute),
        now = now,
        actualLastTimeRun = now.minus(10.day)) must beTrue

      JobDueChecker.isDue(
        JobSchedule(CronLine.parse("* 20 1 ? * * *", CronType.QUARTZ, ZoneId.of("UTC")).getRight, 11.minute),
        now = now,
        actualLastTimeRun = now.minus(10.day)) must beFalse

      JobDueChecker.isDue(
        JobSchedule(CronLine.parse("* 20 0 ? * * *", CronType.QUARTZ, ZoneId.of("UTC")).getRight, 11.minute),
        now = now,
        actualLastTimeRun = now.minus(10.minute)) must beTrue

      JobDueChecker.isDue(
        JobSchedule(CronLine.parse("* 20 0 ? * * *", CronType.QUARTZ, ZoneId.of("UTC")).getRight, 9.minute),
        now = now,
        actualLastTimeRun = now.minus(10.minute)) must beFalse
    }
  }
}
