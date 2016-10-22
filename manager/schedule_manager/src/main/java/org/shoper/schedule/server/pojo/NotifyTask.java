package org.shoper.schedule.server.pojo;

import java.util.Date;

import org.springframework.data.annotation.Id;
/**
 * 外围系统任务通知 pojo
 *
 * @author ShawnShoper
 *
 */
public class NotifyTask
{
	@Id
	private String id;
	private Date createTime;
	private int type;
	private String domain;
	private boolean notify;

	public boolean isNotify()
	{
		return notify;
	}
	public void setNotify(boolean notify)
	{
		this.notify = notify;
	}
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public Date getCreateTime()
	{
		return createTime;
	}
	public void setCreateTime(Date createTime)
	{
		this.createTime = createTime;
	}
	public int getType()
	{
		return type;
	}
	public void setType(int type)
	{
		this.type = type;
	}
	public String getDomain()
	{
		return domain;
	}
	public void setDomain(String domain)
	{
		this.domain = domain;
	}

	public NotifyTask()
	{
	}
	public NotifyTask(int type, String domain)
	{
		this.type = type;
		this.domain = domain;
	}

}
