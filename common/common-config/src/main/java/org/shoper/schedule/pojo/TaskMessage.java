package org.shoper.schedule.pojo;

import com.alibaba.fastjson.JSONObject;

/**
 * TaskMessage server to provider...
 * 
 * @author ShawnShoper
 *
 */
public class TaskMessage
{
	private Task task;
	private TaskTemplate taskTemplate;

	public static TaskMessage parseObject(String json)
	{
		return JSONObject.parseObject(json, TaskMessage.class);
	}
	public Task getTask()
	{
		return task;
	}
	public void setTask(Task task)
	{
		this.task = task;
	}
	public TaskTemplate getTaskTemplate()
	{
		return taskTemplate;
	}
	public void setTaskTemplate(TaskTemplate taskTemplate)
	{
		this.taskTemplate = taskTemplate;
	}
	public TaskMessage(Task task, TaskTemplate taskTemplate)
	{
		this.task = task;
		this.taskTemplate = taskTemplate;
	}
	public TaskMessage()
	{
	}
	public String toJson()
	{
		String json = JSONObject.toJSONString(this);
		return json;
	}
	@Override
	public String toString()
	{
		return "TaskMessage [task=" + task + ", taskTemplate=" + taskTemplate
				+ "]";
	}

}
