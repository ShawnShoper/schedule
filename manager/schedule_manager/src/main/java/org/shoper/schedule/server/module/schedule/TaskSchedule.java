package org.shoper.schedule.server.module.schedule;

import org.shoper.common.rpc.connector.Connector;
import org.shoper.common.rpc.manager.NodeManager;
import org.shoper.concurrent.future.AsynCallable;
import org.shoper.concurrent.future.AsynRunnable;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.concurrent.future.RunnableCallBack;
import org.shoper.schedule.conf.ApplicationInfo;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.pojo.Task;
import org.shoper.schedule.pojo.TaskMessage;
import org.shoper.schedule.server.job.TaskManager;
import org.shoper.schedule.server.job.TaskTrack;
import org.shoper.schedule.server.module.LogModule;
import org.shoper.schedule.server.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TaskSchedule
{
	@Autowired
	LogModule logModule;
	@Autowired
	ApplicationInfo applicationInfo;
	@Autowired
	TaskService taskService;
	@Autowired
	TaskManager taskManager;
	@Autowired
	NodeManager providerManager;
	@Autowired
	TimingSchedule timingSchedule;
	final String normalGenerator = "normalGenerator";
	final String timingGenerator = "timingGenerator";

	/**
	 * 0 running<br>
	 * 1 pending<br>
	 * 2 terminate<br>
	 */
	public void fire()
	{

		// 启动定时任务...
		FutureManager.pushFuture(applicationInfo.getName(), timingGenerator,
				new AsynCallable<Boolean, Object>() {
					@Override
					public Boolean run(Object param) throws Exception
					{
						try
						{
							timingSchedule
									.schedule(taskManager.getAllTimingTask());
						} catch (SystemException e)
						{
							logModule.error(TaskSchedule.class,
									"schedule failed...", e);
						}
						return true;
					}
				});

		// 启动执行任务...
		FutureManager.pushFuture(applicationInfo.getName(), normalGenerator,
				new AsynCallable<Boolean, Object>() {
					@Override
					public Boolean run(Object param) throws Exception
					{
						boolean flag = true;
						try
						{
							doTask();
						} catch (InterruptedException e)
						{
							flag = false;
						}
						return flag;
					}
				});
	}

	/**
	 * Generate task
	 * 
	 * @throws InterruptedException
	 */
	private void doTask() throws InterruptedException
	{
		// 0正常,1 无task,2 无 provider
		for (;;)
		{
			logModule.info(TaskSchedule.class,
					"Request get a avaliable task and provider...");
			try
			{
				TaskAndConnector taskAndConnector = getTaskAndConnector();
				if (taskAndConnector.status == 0x01)
				{
					logModule.info(TaskSchedule.class, "No task");
					// taskManager.waitTaskConsumer();
				} else if (taskAndConnector.status == 0x02)
				{
					logModule.info(TaskSchedule.class, "No provider");
					// providerManager.waitProvider();
				} else
				{
					logModule.info(TaskSchedule.class, "Send task to ["
							+ taskAndConnector.thriftConnector
									.getUrl().getID()
							+ "],task name ["
							+ taskAndConnector.taskMessage.getTask().getName()
							+ "].");
					// 增加超时处理,避免由于 thrift 连接 IP 绑定错误时，无法返回的错误
					try
					{
						FutureManager.pushFuture(applicationInfo.getName(),
								"send-task", new AsynRunnable() {

									@Override
									public void call() throws Exception
									{
										taskAndConnector.thriftConnector
												.getThriftHandler().sendTask(
														taskAndConnector.taskMessage);
									}
								}.setCallback(new RunnableCallBack() {
									@Override
									protected void fail(Exception e)
									{
										pushTaskFaild(taskAndConnector, e);
									}

									@Override
									protected void success()
									{
										TaskTrack.pushTask(
												taskAndConnector.thriftConnector
														.getUrl()
														.getToken(),
												taskAndConnector.taskMessage
														.getTask());

									}

								})).get(applicationInfo.getTimeout(),
										TimeUnit.SECONDS);
					} catch (Exception e)
					{
						pushTaskFaild(taskAndConnector, e);
					}
				}
			} catch (SystemException e1)
			{
				e1.printStackTrace();
			}
		}
	}

	void pushTaskFaild(TaskAndConnector taskAndConnector, Exception e)
	{
		try
		{
			logModule
					.error(TaskSchedule.class,
							"[TaskSchedule]:Send task error.return task ["
									+ taskAndConnector.taskMessage.getTask()
											.getName()
									+ "] and provider ["
									+ taskAndConnector.thriftConnector
											.getUrl().getID()
									+ "]...",

							e);
			TaskTrack.takeFaildTaskAndRemove(
					taskAndConnector.thriftConnector.getUrl()
							.getToken(),
					taskAndConnector.taskMessage.getTask().getId());
			taskManager.returnTask(taskAndConnector.taskMessage.getTask());
			providerManager.putBack(
					taskAndConnector.thriftConnector.getUrl()
							.getGroup(),
					taskAndConnector.thriftConnector.getUrl()
							.getID());
		} catch (InterruptedException e1)
		{
			;
		}
	}

	class TaskAndConnector
	{
		TaskMessage taskMessage;
		Connector thriftConnector;
		int status;
	}

	public TaskAndConnector getTaskAndConnector()
			throws InterruptedException, SystemException
	{
		TaskAndConnector taskAndConnector = new TaskAndConnector();
		for (;;)
		{
			Task task = taskManager.getTask(3, TimeUnit.MINUTES);
			if (task == null)
			{
				taskAndConnector.status = 1;
				return taskAndConnector;
			}
			String group = taskService.getTaskGroup(task.getTemplateID());
			// 未获取到数据,休眠...等待 task pool 获取到新的 task
			Connector connector = providerManager
					.getAvailableProvider(group, 3, TimeUnit.MINUTES, false);
			if (connector == null)
			{
				taskAndConnector.status = 2;
				taskManager.pushTask(task);
				return taskAndConnector;
			}
			try
			{
				taskAndConnector.taskMessage = taskService.getTaskMessage(task);
			} catch (Exception e)
			{
				taskService.inverseStatus(task.getId(), "false", "1");
				e.printStackTrace();
			}
			taskAndConnector.thriftConnector = connector;
			break;
		}
		return taskAndConnector;
	}
}
