package io.ahamdy.jobforce.scheduling

import java.util.concurrent.atomic.AtomicInteger

import fs2.Task
import io.ahamdy.jobforce.testing.StandardSpec
import org.specs2.specification.AfterAll

import scala.concurrent.duration._

class SchedulerImplTest extends StandardSpec with AfterAll{

  val scheduler = new SchedulerImpl(SchedulerConfig(threadPoolSize = 1))

  "SchedulerImpl" should {
    "run task in repeatedly in provided period" in {
      val counter = new AtomicInteger()

      val task = Task.delay(counter.incrementAndGet()).map(_ => ())

      scheduler.unsafeSchedule(1.millis, task, _ => ())

      Thread.sleep(10)

      counter.get() must beGreaterThan(5)
    }

  }

  override def afterAll(): Unit = scheduler.shutdown
}
