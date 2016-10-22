package org.shoper.schedule.pojo;

import org.springframework.data.annotation.Id;

public class Task
{
	@Id
	private String id;
	private String templateID;
	private String name;
	private boolean timing;
	private long createTime;
	private int loops;
	private boolean disabled = false;
	private long lastFinishTime;
	private String params;
	private String url;
	private String cronExpress;
	private String cookies;
	private int failedCount;

	public int getFailedCount()
	{
		return failedCount;
	}
	public void setFailedCount(int failedCount)
	{
		this.failedCount = failedCount;
	}
	public String getCookies()
	{
		return cookies;
	}
	public void setCookies(String cookies)
	{
		this.cookies = cookies;
	}
	public String getUrl()
	{
		return url;
	}
	public void setUrl(String url)
	{
		this.url = url;
	}
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public String getTemplateID()
	{
		return templateID;
	}
	public void setTemplateID(String templateID)
	{
		this.templateID = templateID;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public boolean isTiming()
	{
		return timing;
	}
	public void setTiming(boolean timing)
	{
		this.timing = timing;
	}
	public long getCreateTime()
	{
		return createTime;
	}
	public void setCreateTime(long createTime)
	{
		this.createTime = createTime;
	}
	public int getLoops()
	{
		return loops;
	}
	public void setLoops(int loops)
	{
		this.loops = loops;
	}
	public boolean isDisabled()
	{
		return disabled;
	}
	public void setDisabled(boolean disabled)
	{
		this.disabled = disabled;
	}
	public long getLastFinishTime()
	{
		return lastFinishTime;
	}
	public void setLastFinishTime(long lastFinishTime)
	{
		this.lastFinishTime = lastFinishTime;
	}
	public String getParams()
	{
		return params;
	}
	public void setParams(String params)
	{
		this.params = params;
	}

	public String getCronExpress()
	{
		return cronExpress;
	}
	public void setCronExpress(String cronExpress)
	{
		this.cronExpress = cronExpress;
	}
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Task other = (Task) obj;
		if (id == null)
		{
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	@Override
	public String toString()
	{
		return "Task [id=" + id + ", templateID=" + templateID + ", name="
				+ name + ", timing=" + timing + ", createTime=" + createTime
				+ ", loops=" + loops + ", disabled=" + disabled
				+ ", lastFinishTime=" + lastFinishTime + ", params=" + params
				+ ", url=" + url + ", cronExpress=" + cronExpress + ", cookies="
				+ cookies + ", failedCount=" + failedCount + "]";
	}

}
