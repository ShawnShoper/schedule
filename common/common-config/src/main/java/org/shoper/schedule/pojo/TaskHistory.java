package org.shoper.schedule.pojo;

/**
 * 任务完成历史表
 * 
 * @author ShawnShoper
 *
 */
public class TaskHistory
{
	// provider host:port
	private String provide;
	// task begin time
	private long beginTime;
	// task end time
	private long endTime;
	// task id
	private String taskID;
	// task running status
	private int status;
	private int updateCount;
	private int saveCount;

	public int getUpdateCount()
	{
		return updateCount;
	}
	public void setUpdateCount(int updateCount)
	{
		this.updateCount = updateCount;
	}
	public int getSaveCount()
	{
		return saveCount;
	}
	public void setSaveCount(int saveCount)
	{
		this.saveCount = saveCount;
	}
	public String getProvide()
	{
		return provide;
	}
	public void setProvide(String provide)
	{
		this.provide = provide;
	}
	public long getBeginTime()
	{
		return beginTime;
	}
	public void setBeginTime(long beginTime)
	{
		this.beginTime = beginTime;
	}
	public long getEndTime()
	{
		return endTime;
	}
	public void setEndTime(long endTime)
	{
		this.endTime = endTime;
	}
	public String getTaskID()
	{
		return taskID;
	}
	public void setTaskID(String taskID)
	{
		this.taskID = taskID;
	}
	public int getStatus()
	{
		return status;
	}
	public void setStatus(int status)
	{
		this.status = status;
	}

	public TaskHistory()
	{
	}
	public TaskHistory(String provide, long beginTime, long endTime,
			String taskID, int status)
	{
		this.provide = provide;
		this.beginTime = beginTime;
		this.endTime = endTime;
		this.taskID = taskID;
		this.status = status;
	}

}
