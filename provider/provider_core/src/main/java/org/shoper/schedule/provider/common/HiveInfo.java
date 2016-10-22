package org.shoper.schedule.provider.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
@Component
@ConfigurationProperties(prefix = "hive")
public class HiveInfo
{
	// hive host
	private String host;
	// hive port
	private int port;
	// hive jdbc driver class
	private String driverClass;
	// hive database name
	private String dbName;
	// hive user
	private String user;
	// hive password
	private String password;

	public String getDriverClass()
	{
		return driverClass;
	}
	public void setDriverClass(String driverClass)
	{
		this.driverClass = driverClass;
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
	public String getDbName()
	{
		return dbName;
	}
	public void setDbName(String dbName)
	{
		this.dbName = dbName;
	}
	public String getUser()
	{
		return user;
	}
	public void setUser(String user)
	{
		this.user = user;
	}
	public String getPassword()
	{
		return password;
	}
	public void setPassword(String password)
	{
		this.password = password;
	}

}
