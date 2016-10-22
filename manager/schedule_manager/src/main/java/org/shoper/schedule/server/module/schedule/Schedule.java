package org.shoper.schedule.server.module.schedule;

import org.quartz.Scheduler;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.pojo.Task;

public interface Schedule
{
	public void schedule(Task task) throws SystemException;
	public void shutdownJob(Scheduler sched, String name, String group)
			throws SystemException;
}
