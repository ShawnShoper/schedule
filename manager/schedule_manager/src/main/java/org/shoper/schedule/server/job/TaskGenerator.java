package org.shoper.schedule.server.job;


import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.shoper.schedule.conf.ApplicationInfo;
import org.shoper.schedule.pojo.Task;
import org.shoper.schedule.server.module.LogModule;
import org.shoper.schedule.server.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskGenerator
{
	@Autowired
	LogModule logModule;
	@Autowired
	TaskService taskService;
	@Autowired
	ApplicationInfo application;
	// private final String FUTURENAMENORMALTASK = "generatorNormalTask";

	@PostConstruct
	public void init()
	{
		logModule.info(TaskGenerator.class, "初始化 Task generator...");
	}

	@PreDestroy
	public void destroy()
	{
		// FutureManager.futureDone(application.getName(),
		// FUTURENAMENORMALTASK);
		logModule.info(TaskGenerator.class, "销毁 Task generator...");
	}

	/**
	 * 启动任务生成器
	 */
	public void fire()
	{
		// 启动任务生成器..

	}

	public List<Task> generateTimingTask()
	{
		return taskService.getTimingTask();
	}
	/**
	 * 生成任务...
	 */
	public List<Task> generateNotTimingTask()
	{
		return taskService.getTask(application.getTaskGenerateInterval(),
				application.getGenerateIntervalUnit(), false);
	}
}
