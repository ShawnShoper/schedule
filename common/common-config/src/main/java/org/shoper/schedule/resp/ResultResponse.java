package org.shoper.schedule.resp;

import com.alibaba.fastjson.JSONObject;
import org.shoper.schedule.resp.ResponseBase;

/**
 * resultResponse
 * 
 * @author ShawnShoper
 *
 */
public class ResultResponse extends ResponseBase
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8292247700158638894L;
	private String addr;
	// job name
	private String jobName;
	// job done status.
	private boolean done;
	// save count
	private long saveCount;
	// task success
	private boolean success;
	// update count
	private long updateCount;
	private long handCount;
	// start time
	private long startTime;
	// end time
	private long endTime;
	private long timeConsuming;
	public String getAddr()
	{
		return addr;
	}
	public void setAddr(String addr)
	{
		this.addr = addr;
	}

	public void setTimeConsuming(long timeConsuming)
	{
		this.timeConsuming = timeConsuming;
	}
	public long getTimeConsuming()
	{
		return this.timeConsuming;
	}
	public String getJobName()
	{
		return jobName;
	}
	public void setJobName(String jobName)
	{
		this.jobName = jobName;
	}
	public boolean isDone()
	{
		return done;
	}
	public void setDone(boolean done)
	{
		this.done = done;
	}
	public long getSaveCount()
	{
		return saveCount;
	}
	public void setSaveCount(long saveCount)
	{
		this.saveCount = saveCount;
	}
	public boolean isSuccess()
	{
		return success;
	}
	public void setSuccess(boolean success)
	{
		this.success = success;
	}
	public long getUpdateCount()
	{
		return updateCount;
	}
	public void setUpdateCount(long updateCount)
	{
		this.updateCount = updateCount;
	}
	public long getStartTime()
	{
		return startTime;
	}
	public void setStartTime(long startTime)
	{
		this.startTime = startTime;
	}
	public long getEndTime()
	{
		return endTime;
	}
	public void setEndTime(long endTime)
	{
		this.endTime = endTime;
	}
	public static ResultResponse parseObject(String json)
	{
		return JSONObject.parseObject(json, ResultResponse.class);
	}
	public String toJson()
	{
		return JSONObject.toJSONString(this);
	}

	public long getHandCount()
	{
		return handCount;
	}
	public void setHandCount(long handCount)
	{
		this.handCount = handCount;
	}
	public ResultResponse(String addr, String jobName, boolean done,
			long saveCount, boolean success, long updateCount, long handleCount,
			long startTime, long endTime, long timeConsuming)
	{

		this.addr = addr;
		this.jobName = jobName;
		this.done = done;
		this.handCount = handleCount;
		this.saveCount = saveCount;
		this.success = success;
		this.updateCount = updateCount;
		this.startTime = startTime;
		this.endTime = endTime;
		this.timeConsuming = timeConsuming;
	}
	public ResultResponse()
	{
	}

}
