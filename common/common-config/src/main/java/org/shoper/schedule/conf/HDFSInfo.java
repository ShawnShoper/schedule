package org.shoper.schedule.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

/**
 * HDFS 连接信息....
 * 
 * @author ShawnShoper
 *
 */
@Component
@ConfigurationProperties(prefix = "hdfs")
public class HDFSInfo
{
	private String nodePath;
	public String getNodePath()
	{
		return nodePath;
	}
	public void setNodePath(String nodePath)
	{
		this.nodePath = nodePath;
	}
	private String hostKey;
	private String hostValue;
	public String getHostKey()
	{
		return hostKey;
	}
	public void setHostKey(String hostKey)
	{
		this.hostKey = hostKey;
	}
	public String getHostValue()
	{
		return hostValue;
	}
	public void setHostValue(String hostValue)
	{
		this.hostValue = hostValue;
	}
	public static HDFSInfo parseObject(String json)
	{
		return JSONObject.parseObject(json, HDFSInfo.class);
	}
	public String toJson()
	{
		return JSONObject.toJSONString(this);
	}
	@Override
	public String toString()
	{
		return "HDFSInfo [nodePath=" + nodePath + ", hostKey=" + hostKey
				+ ", hostValue=" + hostValue + "]";
	}

}
