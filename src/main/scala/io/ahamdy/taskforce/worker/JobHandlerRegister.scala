package io.ahamdy.taskforce.worker

import java.util.concurrent.ConcurrentHashMap

import io.ahamdy.taskforce.domain.{JobType, RegisterError}

trait JobHandlerRegister {
  def jobHandlers: ConcurrentHashMap[JobType, JobHandler]
  def registerHandler(jobHandler: JobHandler): Either[RegisterError, Unit]
  def getJobHandler(jobType: JobType): Option[JobHandler]
}

class JobHandlerRegisterImpl extends JobHandlerRegister {
  override val jobHandlers = new ConcurrentHashMap[JobType, JobHandler]

  override def registerHandler(jobHandler: JobHandler): Either[RegisterError, Unit] =
    Option(jobHandlers.putIfAbsent(jobHandler.jobType, jobHandler)) match {
      case None => Right(())
      case Some(_) =>
        Left(RegisterError(jobHandler.jobType, s"${jobHandler.jobType} already has a registered job handler"))
    }

  override def getJobHandler(jobType: JobType): Option[JobHandler] = Option(jobHandlers.get(jobType))
}
