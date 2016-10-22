package org.shoper.schedule.job;

import com.alibaba.fastjson.JSONObject;
import org.shoper.schedule.resp.ReportResponse;

import java.util.concurrent.atomic.AtomicLong;

/**
 * job result
 * 
 * @author ShawnShoper
 */
public class JobResult
{
	// job name
	private String jobName;
	// job done status.
	private boolean done;
	// save count
	private AtomicLong saveCount = new AtomicLong(0);
	// task success
	private boolean success;
	// update count
	private AtomicLong updateCount = new AtomicLong(0);
	// handle count
	private AtomicLong handleCount = new AtomicLong(0);
	// start time
	private long startTime;
	// end time
	private long endTime;
	private ReportResponse.Error error;
	private String errMessage;

	public AtomicLong getHandleCount()
	{
		return handleCount;
	}
	public ReportResponse.Error getError()
	{
		return error;
	}
	public void setError(ReportResponse.Error error)
	{
		this.error = error;
	}
	public String getErrMessage()
	{
		return errMessage;
	}
	public void setErrMessage(String errMessage)
	{
		this.errMessage = errMessage;
	}
	public String getJobName()
	{
		return jobName;
	}
	public void setJobName(String jobName)
	{
		this.jobName = jobName;
	}
	public boolean isSuccess()
	{
		return success;
	}
	public void setSuccess(boolean success)
	{
		this.success = success;
	}
	public long getTimeConsuming()
	{
		return System.currentTimeMillis() - startTime;
	}
	public AtomicLong getSaveCount()
	{
		return saveCount;
	}

	public AtomicLong getUpdateCount()
	{
		return updateCount;
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

	public boolean isDone()
	{
		return done;
	}

	public void setDone(boolean done)
	{
		this.done = done;
	}
	public static JobResult parseObject(String json)
	{
		return JSONObject.parseObject(json, JobResult.class);
	}
	public String toJson()
	{
		return JSONObject.toJSONString(this);
	}
}
