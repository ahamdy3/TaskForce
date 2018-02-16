package io.ahamdy.taskforce.syntax

import java.util.concurrent.{ExecutorService, Executors}

import fs2.{Strategy, Task}
import io.ahamdy.taskforce.syntax.IO._
import io.ahamdy.taskforce.testing.StandardSpec
import org.specs2.specification.AfterAll

import scala.collection.mutable

class IOSyntaxTest extends StandardSpec with AfterAll {
  val executor: ExecutorService =  Executors.newFixedThreadPool(5)
  implicit val strategy: Strategy = Strategy.fromExecutor(executor)

  "TaskSyntax" should {
    "sequenceUnit should traverse a list of Task and return IO[Unit]" in {
      val mutableStringSet = mutable.Set.empty[String]
      def createTask(msg: String) = IO(mutableStringSet.add(msg))

      val taskList = List(
        createTask("task-1"),
        createTask("task-2"),
        createTask("task-3")
      )

      sequenceUnit(taskList) must beSucceedingTask(())

      mutableStringSet mustEqual Set("task-1", "task-2", "task-3")
    }

    "sequenceUnit should return IO[Unit] if given an empty List[IO[_]]" in {
      sequenceUnit(List.empty[IO[Unit]]) must beSucceedingTask(())
    }

    "parallelSequenceUnit should traverse a list of Task and return IO[Unit] in parallel" in {

      val mutableStringList = mutable.ListBuffer.empty[String]
      def createTask(msg: String, delayInMills: Int) = IO {
        Thread.sleep(delayInMills)
        mutableStringList.append(msg)
      }

      val taskList = List(
        createTask("task-1", 10),
        createTask("task-2", 1),
        createTask("task-3", 20)
      )

      parallelSequenceUnit(taskList) must beSucceedingTask(())

      mutableStringList mustEqual List("task-2", "task-1", "task-3")
    }

    "parallelSequenceUnit should return IO[Unit] if given an empty List[IO[_]]" in {
      parallelSequenceUnit(List.empty[IO[Unit]]) must beSucceedingTask(())
    }
  }



  override def afterAll(): Unit = executor.shutdown()
}
