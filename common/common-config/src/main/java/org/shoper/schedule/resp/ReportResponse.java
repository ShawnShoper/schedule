package org.shoper.schedule.resp;

import com.alibaba.fastjson.JSONObject;
import org.shoper.schedule.job.JobResult;

public class ReportResponse extends ResponseBase
{
	public static enum Error
	{
		NONE, EXCEP, FETAL;
	}
	private static final long serialVersionUID = 1635318217684162364L;
	// error message
	private String errMessage;
	// job ID
	private String job;
	// provider key
	private String providerToken;
	// job result
	private JobResult jobResult;
	private String group;

	public String getErrMessage()
	{
		return errMessage;
	}
	public void setErrMessage(String errMessage)
	{
		this.errMessage = errMessage;
	}
	public String getGroup()
	{
		return group;
	}
	public void setGroup(String group)
	{
		this.group = group;
	}
	/**
	 * err level<br>
	 * 0 常规异常<br>
	 * 1 规则异常(传递的内容格式正确,但是规则出现有不能容错的情况,批回,需要修改)<br>
	 * 2 致命异常(传递内容错误解析失败)<br>
	 */
	private Error err = Error.NONE;
	public Error getErr()
	{
		return err;
	}
	public void setErr(Error err)
	{
		this.err = err;
	}
	public String getJob()
	{
		return job;
	}
	public void setJob(String job)
	{
		this.job = job;
	}
	public JobResult getJobResult()
	{
		return jobResult;
	}
	public void setJobResult(JobResult jobResult)
	{
		this.jobResult = jobResult;
	}

	public String getProviderToken()
	{
		return providerToken;
	}
	public void setProviderToken(String providerToken)
	{
		this.providerToken = providerToken;
	}
	public String toJson()
	{
		return JSONObject.toJSONString(this);
	}
	public static ReportResponse parseObject(String json)
	{
		return JSONObject.parseObject(json, ReportResponse.class);
	}
}
