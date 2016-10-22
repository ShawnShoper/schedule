package org.shoper.schedule.server.web.response;

import com.alibaba.fastjson.JSONObject;

public class Response
{
	private String message = "Success";
	private long responseTime = System.currentTimeMillis();
	private int code = 0;

	public int getCode()
	{
		return code;
	}
	public void setCode(int code)
	{
		this.code = code;
	}

	public long getResponseTime()
	{
		return responseTime;
	}
	public void setResponseTime(long responseTime)
	{
		this.responseTime = responseTime;
	}
	public String getMessage()
	{
		return message;
	}
	public void setMessage(String message)
	{
		this.message = message;
	}
	public String toJson()
	{
		return JSONObject.toJSONString(this);
	}
}
