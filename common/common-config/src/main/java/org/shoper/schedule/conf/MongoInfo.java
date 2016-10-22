package org.shoper.schedule.conf;

import com.alibaba.fastjson.JSONObject;

public class MongoInfo
{
	private String serverAddress;
	private String dbName;
	private int timeout;

	public String getServerAddress()
	{
		return serverAddress;
	}
	public void setServerAddress(String serverAddress)
	{
		this.serverAddress = serverAddress;
	}
	public String getDbName()
	{
		return dbName;
	}
	public void setDbName(String dbName)
	{
		this.dbName = dbName;
	}
	public int getTimeout()
	{
		return timeout;
	}
	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
	}
	public static MongoInfo parseObject(String json)
	{
		return JSONObject.parseObject(json, MongoInfo.class);
	}
	public String toJson()
	{
		return JSONObject.toJSONString(this);
	}
}
