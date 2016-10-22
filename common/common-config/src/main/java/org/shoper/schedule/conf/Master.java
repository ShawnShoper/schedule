package org.shoper.schedule.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

@Component
@ConfigurationProperties(prefix="master")
//读取yaml nodePath 配置
public class Master
{
	//load in yaml
	private String nodePath;
	private String host;
	private int port;

	public static Master parseObjct(String data)
	{
		return JSONObject.parseObject(data, Master.class);
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

	public String getNodePath()
	{
		return nodePath;
	}

	public void setNodePath(String nodePath)
	{
		this.nodePath = nodePath;
	}
	
}
