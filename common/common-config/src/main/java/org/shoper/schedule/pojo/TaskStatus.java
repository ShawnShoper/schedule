package org.shoper.schedule.pojo;

import org.springframework.data.annotation.Id;

/**
 * Task progress
 * 
 * @author ShawnShoper
 *
 */
public class TaskStatus
{
	@Id
	private String token;
	private String taskId;
	private int status;
	private String taskName;
	private String provider;

	public String getToken()
	{
		return token;
	}
	public void setToken(String token)
	{
		this.token = token;
	}
	public int getStatus()
	{
		return status;
	}
	public void setStatus(int status)
	{
		this.status = status;
	}
	public String getTaskName()
	{
		return taskName;
	}
	public void setTaskName(String taskName)
	{
		this.taskName = taskName;
	}
	public String getTaskId()
	{
		return taskId;
	}
	public void setTaskId(String taskId)
	{
		this.taskId = taskId;
	}
	public String getProvider()
	{
		return provider;
	}
	public void setProvider(String provider)
	{
		this.provider = provider;
	}

}
