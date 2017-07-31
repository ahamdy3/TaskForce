package io.ahamdy.jobforce.common

import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock

import io.ahamdy.jobforce.syntax.zonedDateTime._
import cats.syntax.flatMap._
import fs2.interop.cats._

import fs2.Task

import scala.concurrent.duration.FiniteDuration

class CachedValue[A](source: Task[A], ttl: FiniteDuration, time: Time) {
  val currentValue = new AtomicReference[Task[A]]()
  val lastUpdated = new AtomicReference[ZonedDateTime](time.epoch)
  val lock = new ReentrantReadWriteLock()

  def value: Task[A] =
    time.now.flatMap{ now =>
      if (lastUpdated.get().isBefore(now.minus(ttl))){
        try{
          lock.writeLock().lock()
          source.unsafeRunSync() match {
            case Right(a) =>
              Task.delay(lastUpdated.set(now)) >>
                Task.delay {
                  currentValue.set(Task.now(a))
                  a
                }
            case Left(_) => source
          }
        }finally {
          lock.writeLock().unlock()
        }
      }else{
        try{
          lock.readLock().lock()
          currentValue.get()
        }finally {
          lock.readLock().unlock()
        }
      }
    }
}
