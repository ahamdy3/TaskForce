package io.ahamdy.taskforce.syntax

import java.util.concurrent.{ExecutorService, Executors}

import monix.eval.Task
import io.ahamdy.taskforce.syntax.IOType._
import io.ahamdy.taskforce.testing.StandardSpec
import org.specs2.specification.AfterAll

import scala.collection.mutable

class IOSyntaxTest extends StandardSpec with AfterAll {
  val executor: ExecutorService =  Executors.newFixedThreadPool(5)
  // implicit val strategy: Strategy = Strategy.fromExecutor(executor)

  "TaskSyntax" should {
    "sequenceUnit should traverse a list of Task and return Task[Unit]" in {
      val mutableStringSet = mutable.Set.empty[String]
      def createTask(msg: String): Task[Unit] = Task(mutableStringSet.add(msg))

      val taskList = List(
        createTask("task-1"),
        createTask("task-2"),
        createTask("task-3")
      )

      sequenceUnit(taskList) must beSucceedingIO(())

      mutableStringSet mustEqual Set("task-1", "task-2", "task-3")
    }

    "sequenceUnit should return Task[Unit] if given an empty List[Task[_]]" in {
      sequenceUnit(List.empty[Task[Unit]]) must beSucceedingIO(())
    }

    "parallelSequenceUnit should traverse a list of Task and return Task[Unit] in parallel" in {

      val mutableStringList = mutable.ListBuffer.empty[String]
      def createTask(msg: String, delayInMills: Int) = Task {
        Thread.sleep(delayInMills)
        mutableStringList.append(msg)
      }

      val taskList = List(
        createTask("task-1", 10),
        createTask("task-2", 1),
        createTask("task-3", 20)
      )

      parallelSequenceUnit(taskList) must beSucceedingIO(())

      mutableStringList mustEqual List("task-2", "task-1", "task-3")
    }

    "parallelSequenceUnit should return Task[Unit] if given an empty List[Task[_]]" in {
      parallelSequenceUnit(List.empty[Task[Unit]]) must beSucceedingIO(())
    }
  }



  override def afterAll(): Unit = executor.shutdown()
}
