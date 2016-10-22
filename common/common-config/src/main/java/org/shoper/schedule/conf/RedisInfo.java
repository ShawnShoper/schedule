package org.shoper.schedule.conf;

import com.alibaba.fastjson.JSONObject;
public class RedisInfo
{
	private String host;
	private int port;
	private int timeout;
	private String password;

	public String getHost()
	{
		return host;
	}
	public void setHost(String host)
	{
		this.host = host;
	}
	public int getPort()
	{
		return port;
	}
	public void setPort(int port)
	{
		this.port = port;
	}
	public int getTimeout()
	{
		return timeout;
	}
	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
	}
	public String getPassword()
	{
		return password;
	}
	public void setPassword(String password)
	{
		this.password = password;
	}
	public String toJson()
	{
		return JSONObject.toJSONString(this);
	}
	public static RedisInfo parseObject(String json)
	{
		return JSONObject.parseObject(json, RedisInfo.class);
	}
}
