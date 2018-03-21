package io.ahamdy.taskforce.common

import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock

import io.ahamdy.taskforce.syntax.zonedDateTime._
import cats.syntax.flatMap._
import cats.effect.IO

import scala.concurrent.duration.FiniteDuration

class CachedValue[A](source: IO[A], ttl: FiniteDuration, time: Time) {
  val currentValue = new AtomicReference[IO[A]]()
  val lastUpdated = new AtomicReference[ZonedDateTime](time.epoch)
  val lock = new ReentrantReadWriteLock()

  def value: IO[A] =
    time.now.flatMap{ now =>
      if (lastUpdated.get().isBefore(now.minus(ttl))){
        try{
          lock.writeLock().lock()
          source.unsafeRunSync() match {
            case Right(a: A) =>
              IO(lastUpdated.set(now)) >>
                IO {
                  currentValue.set(IO.pure(a))
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
