package org.shoper.schedule.conf;

import org.springframework.stereotype.Component;

/**
 * zk数据读取
 * 
 * @author ShawnShoper
 */
@Component
public class MasterInfo
{
	private String host;
	private int port;
	private Status status;
	public void available()
	{
		this.status = Status.AVAILABLE;
	}
	public void unAvailable()
	{
		this.status = Status.UNAVAILABLE;
	}
	enum Status
	{
		AVAILABLE, UNAVAILABLE;
	}

	public Status getStatus()
	{
		return status;
	}
	public void setStatus(Status status)
	{
		this.status = status;
	}
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
	

}
