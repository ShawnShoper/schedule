package org.shoper.schedule.server.web.vo;

import org.shoper.schedule.pojo.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * 重造 task，提供前台页面使用
 * 
 * @author ShawnShoper
 *
 */
public class TaskVO
{
	private String id;
	private String templateID;
	private String name;
	private String timing;
	private String createTime;
	private int loops;
	private String enabled;
	private String lastFinishTime;
	private String params;
	private String cronExpress;
	private String url;
	private String cookies;
	private static SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	public static TaskVO toVO(Task task)
	{
		TaskVO taskVO = null;
		if (task != null)
		{
			taskVO = new TaskVO();
			taskVO.setCreateTime(sdf.format(task.getCreateTime()));
			taskVO.setCronExpress(task.getCronExpress());
			taskVO.setEnabled(
					"<input type=\"checkbox\" class=\"disable_checkbox\" cust-id='"
							+ task.getId() + "' "
							+ (task.isDisabled() ? "" : "checked")
							+ " data-toggle=\"toggle\" data-style=\"ios slow\" data-offstyle=\"danger\" data-toggle=\"toggle\" data-onstyle=\"info\">");
			taskVO.setId(task.getId());
			taskVO.setLastFinishTime(sdf.format(task.getLastFinishTime()));
			taskVO.setLoops(task.getLoops());
			taskVO.setUrl(task.getUrl());
			taskVO.setParams(task.getParams());
			taskVO.setCookies(task.getCookies());
			taskVO.setTemplateID(task.getTemplateID());
			taskVO.setTiming(task.isTiming() ? "是" : "否");
			taskVO.setName(task.getName());
		}
		return taskVO;
	}
	public static List<TaskVO> toVO(List<Task> tasks)
	{
		List<TaskVO> taskVOs = new ArrayList<TaskVO>();
		tasks.stream().forEach(task ->
		{
			TaskVO taskVO = toVO(task);
			if (taskVO != null)
				taskVOs.add(taskVO);

		});
		return taskVOs;
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
	public String getCreateTime()
	{
		return createTime;
	}
	public void setCreateTime(String createTime)
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
	public String getLastFinishTime()
	{
		return lastFinishTime;
	}
	public void setLastFinishTime(String lastFinishTime)
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
	public String getTiming()
	{
		return timing;
	}
	public void setTiming(String timing)
	{
		this.timing = timing;
	}
	public String getEnabled()
	{
		return enabled;
	}
	public void setEnabled(String enabled)
	{
		this.enabled = enabled;
	}
	public String getUrl()
	{
		return url;
	}
	public void setUrl(String url)
	{
		this.url = url;
	}
	public String getCookies()
	{
		return cookies;
	}
	public void setCookies(String cookies)
	{
		this.cookies = cookies;
	}

}
