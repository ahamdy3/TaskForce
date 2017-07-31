package io.ahamdy.jobforce.leader.components

import java.time.{ZoneId, ZonedDateTime}

import scala.concurrent.duration._
import com.cronutils.model.CronType
import io.ahamdy.jobforce.common.DummyTime
import io.ahamdy.jobforce.domain.JobSchedule
import io.ahamdy.jobforce.scheduling.CronLine
import io.ahamdy.jobforce.testing.StandardSpec

class JobDueCheckerTest extends StandardSpec {
  val dummyTime = new DummyTime(ZonedDateTime.now())
  "JobDueChecker" should {
    "isDue should return true only if job next execution time is within startTimeWindow" in {
      JobDueChecker.isDue(
        JobSchedule(CronLine.parse("0 * * * * ?", CronType.QUARTZ, ZoneId.of("UTC")).get, 1.minute),
        now = dummyTime.unsafeNow(),
        lastRunTime = dummyTime.unsafeNow().minusSeconds(5)) must beTrue
    }
  }
}
