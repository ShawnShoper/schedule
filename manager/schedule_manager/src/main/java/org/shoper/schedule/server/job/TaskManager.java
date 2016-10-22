package org.shoper.schedule.server.job;

import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.shoper.concurrent.future.AsynCallable;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.schedule.conf.ApplicationInfo;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.pojo.Task;
import org.shoper.schedule.server.module.LogModule;
import org.shoper.schedule.server.module.schedule.TimingSchedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskManager
{
	@Autowired
	LogModule logModule;
	@Autowired
	TaskGenerator taskGenerator;
	@Autowired
	ApplicationInfo application;
	private final String FUTURENAMENORMALTASK = "generatorNormalTask";
	final TaskQueue taskQueue = new TaskQueue();

	/**
	 * task generator monitor<br>
	 * 0 running<br>
	 * 1 pending<br>
	 */
	private final AtomicInteger pm = new AtomicInteger(0);
	private final AtomicInteger cm = new AtomicInteger(0);
	public void notifyTaskConsumer()
	{
		synchronized (cm)
		{
			// cm.decrementAndGet();
			cm.notify();
		}
	}
	// public void waitTaskConsumer() throws InterruptedException
	// {
	// waitTaskConsumer(0, TimeUnit.MILLISECONDS);
	// }
	// public void waitTaskConsumer(int time, TimeUnit unit)
	// throws InterruptedException
	// {
	// synchronized (cm)
	// {
	// // cm.incrementAndGet();
	// cm.wait(unit.toMillis(time));
	// }
	// }
	public void notifyTaskGenerate()
	{
		synchronized (pm)
		{
			// pm.decrementAndGet();
			pm.notify();
		}
	}
	public void waitTaskGenerate()
	{
		waitTaskGenerate(0, TimeUnit.MILLISECONDS);
	}
	public void waitTaskGenerate(int time, TimeUnit unit)
	{
		synchronized (pm)
		{
			try
			{
				// pm.incrementAndGet();
				logModule.info(TaskManager.class,
						"任务生成器开始等待" + unit.toSeconds(time) + "秒");
				pm.wait(unit.toMillis(time));
			} catch (InterruptedException e)
			{
				;
			}
		}
	}
	/**
	 * Start generate task.
	 */
	public void fire()
	{
		logModule.info(TaskManager.class, "启动任务管理器...");
		FutureManager.pushFuture(application.getName(), FUTURENAMENORMALTASK,
				new AsynCallable<Boolean, Object>() {
					@Override
					public Boolean run(Object param) throws Exception
					{
						for (;;)
						{
							long st = System.currentTimeMillis();
							logModule.info(TaskManager.class, "开始生成任务");
							try
							{
								List<Task> tasks = taskGenerator
										.generateNotTimingTask();
								pushTask(tasks);
								logModule.info(TaskManager.class, "状态:当前生成任务"
										+ (tasks == null ? 0 : tasks.size())
										+ "条,耗时"
										+ (System.currentTimeMillis() - st)
										+ "耗秒");
								waitTaskGenerate(
										application.getTaskGenerateInterval(),
										application.getGenerateIntervalUnit());

							} catch (Exception e)
							{
								e.printStackTrace();
							}
						}
					}
				});
	}

	/**
	 * Get a task from task pool<br>
	 * return null if task pool is empty,and notify task generator..
	 * 
	 * @return task
	 */
	public Task getTask(int timeout, TimeUnit unit)
	{
		Task task = null;
		try
		{
			task = taskQueue.pendingTask.poll(timeout, unit);
			if (task == null)
			{
				notifyTaskGenerate();
			}
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		return task;
	}
	/**
	 * 往任务池里推送任务.
	 * 
	 * @param tasks
	 * @throws InterruptedException
	 * @throws SystemException
	 */
	public void pushTask(List<Task> tasks)
			throws InterruptedException, SystemException
	{
		for (Task task : tasks)
		{
			pushTask(task);
		}
		notifyTaskConsumer();
	}
	/**
	 * 用于添加新任务..转发定时任务以及非定时任务
	 * 
	 * @param task
	 * @throws SystemException
	 * @throws InterruptedException
	 */
	public void addTask(Task task) throws SystemException, InterruptedException
	{
		if (task.isTiming())
		{
			try
			{
				timingSchedule.schedule(task);
			} catch (SystemException e)
			{
				throw new SystemException(e);
			}
		} else
			pushTask(task);
	}
	/**
	 * 用于生成的任务进行推送到任务等待队列...
	 * 
	 * @param task
	 * @throws InterruptedException
	 */
	public void pushTask(Task task) throws InterruptedException
	{

		if (!taskQueue.pendingTask.contains(task))
			while (!taskQueue.pendingTask.offer(task, 1, TimeUnit.SECONDS));

	}
	private class TaskQueue
	{
		private final BlockingDeque<Task> pendingTask = new LinkedBlockingDeque<Task>();
	}
	@PostConstruct
	public void init()
	{
		// # TODO nothing to do now
	}
	@PreDestroy
	public void destroy()
	{
		taskQueue.pendingTask.clear();
	}
	public List<Task> getAllTimingTask()
	{

		return taskGenerator.generateTimingTask();
	}
	/**
	 * Calling for task has be done
	 * 
	 * @param provider
	 * @param task
	 */
	public void taskDone(String provider, String task)
	{

	}
	/**
	 * Calling for task has failed
	 * 
	 * @param task
	 * @throws InterruptedException
	 */
	public void returnTask(Task task) throws InterruptedException
	{
		taskQueue.pendingTask.offer(task, 20, TimeUnit.SECONDS);
	}
	@Autowired
	TimingSchedule timingSchedule;
	/**
	 * 删除任务...
	 * 
	 * @param task
	 * @return
	 * @throws SystemException
	 */
	public boolean removeTask(Task task) throws SystemException
	{
		logModule.info(TaskManager.class, "删除任务,ID=[" + task.getId() + "]");
		// check is timing task
		if (task.isTiming())
		{
			try
			{
				timingSchedule.remove(task);
			} catch (SystemException e)
			{
				throw e;
			}
		}
		return taskQueue.pendingTask.remove(task);
	}

	/**
	 * 反转 task 状态..true 添加任务,fasle 删除任务
	 * 
	 * @param task
	 * @throws InterruptedException
	 * @throws SystemException
	 */
	public void inverseStatus(Task task, boolean value)
			throws SystemException, InterruptedException
	{
		if (value)
			addTask(task);
		else
			removeTask(task);
	}
}
