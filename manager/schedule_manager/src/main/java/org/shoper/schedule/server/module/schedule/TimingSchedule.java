package org.shoper.schedule.server.module.schedule;


import javax.annotation.PreDestroy;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.shoper.schedule.conf.ApplicationInfo;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.pojo.Task;
import org.shoper.schedule.server.module.LogModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Quartz调度实现
 */
@Component
public class TimingSchedule implements Schedule
{
	@Autowired
	ApplicationInfo applicationInfo;
	@Autowired
	LogModule logModule;
	@Autowired
	QuartzManager quartzManager;
	/**
	 * 单个任务调度...
	 */
	@Override
	public void schedule(Task task) throws SystemException
	{
		logModule.info(TimingSchedule.class,
				"Scheduling task [id=" + task.getId() + "]");
		String taskID = task.getId();
		String taskParentID = task.getTemplateID();
		// 定义一个job, 设置身份为task相关
		JobDetail jobDetail = JobBuilder.newJob(BaseJob.class)
				.withIdentity(taskID, taskParentID).build();
		// 把我们关注的参数传入到任务里
		jobDetail.getJobDataMap().put("task", task);
		// 定义一个触发器, 设置身份为task相关
		Trigger trigger = TriggerBuilder.newTrigger()
				.withIdentity(taskID, taskParentID)
				.withSchedule(
						CronScheduleBuilder.cronSchedule(task.getCronExpress()))
				.startNow()
				// .withSchedule(SimpleScheduleBuilder.simpleSchedule()
				// .withIntervalInSeconds(10) //时间间隔
				// .withRepeatCount(5)) //重复次数(将执行6次)
				.build();

		try
		{
			Scheduler scheduler = quartzManager
					.getScheduler(applicationInfo.getName());
			// #TODO 检查 trigger 是否存在...如果存在恢复即可.
			// scheduler.getTriggerState(trigger.getKey()).;
			scheduler.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException e)
		{
			throw new SystemException(e);
		}
		logModule.info(TimingSchedule.class,
				"Started a task ,id " + task.getId());
	}

	@Override
	public void shutdownJob(Scheduler sched, String name, String group)
			throws SystemException

	{
		try
		{
			JobKey jobKey = JobKey.jobKey(name, group);
			TriggerKey tk = new TriggerKey(name, group);
			sched.pauseTrigger(tk);// 停止触发器
			sched.unscheduleJob(tk);
			sched.deleteJob(jobKey);// 删除任务
		} catch (SchedulerException e)
		{
			throw new SystemException(e);
		}
	}

	@PreDestroy
	public void destroy()
	{
		try
		{
			QuartzManager.shutdownAll();
		} catch (SchedulerException e)
		{
			e.printStackTrace();
		}
	}
	/**
	 * 调度一批任务
	 * 
	 * @param tasks
	 * @throws SystemException
	 */
	public void schedule(List<Task> tasks) throws SystemException
	{
		for (Task task : tasks)
		{
			Assert.notNull(task);
			Assert.notNull(task);
			Assert.notNull(task.getCronExpress());
			Assert.hasText(task.getCronExpress());
			if (task.isTiming())
				schedule(task);
		}
	}
	/**
	 * 移除一个定时任务.
	 * 
	 * @param task
	 * @throws SystemException
	 */
	public void remove(Task task) throws SystemException
	{
		try
		{
			Scheduler scheduler = quartzManager
					.getScheduler(applicationInfo.getName());
			shutdownJob(scheduler, task.getId(), task.getTemplateID());

		} catch (SchedulerException e)
		{
			throw new SystemException(e);
		}

	}
}
