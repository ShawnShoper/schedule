package org.shoper.schedule.pojo;

import java.util.Date;

import org.springframework.data.annotation.Id;
public class TaskTemplate
{
	@Id
	private String id;
	private byte[] code;
	private String name;
	private Date createTime;
	private Date updateTime;
	private String url;
	private String cookies;
	private int removed;

	// 用于匹配 cluster 分区..
	private String group;

	public int getRemoved()
	{
		return removed;
	}
	public void setRemoved(int removed)
	{
		this.removed = removed;
	}
	public String getGroup()
	{
		return group;
	}
	public void setGroup(String group)
	{
		this.group = group;
	}
	public String getCookies()
	{
		return cookies;
	}
	public void setCookies(String cookies)
	{
		this.cookies = cookies;
	}
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public byte[] getCode()
	{
		return code;
	}
	public void setCode(byte[] code)
	{
		this.code = code;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}

	public String getUrl()
	{
		return url;
	}
	public void setUrl(String url)
	{
		this.url = url;
	}
	public Date getCreateTime()
	{
		return createTime;
	}
	public void setCreateTime(Date createTime)
	{
		this.createTime = createTime;
	}
	public Date getUpdateTime()
	{
		return updateTime;
	}
	public void setUpdateTime(Date updateTime)
	{
		this.updateTime = updateTime;
	}
	@Override
	public String toString()
	{
		return "TaskTemplate [id=" + id + ", name=" + name + ", createTime="
				+ createTime + ", updateTime=" + updateTime + ", url=" + url
				+ ", cookies=" + cookies + ", group=" + group + "]";
	}

}
