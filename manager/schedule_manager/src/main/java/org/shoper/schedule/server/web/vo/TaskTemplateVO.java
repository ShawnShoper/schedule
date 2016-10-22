package org.shoper.schedule.server.web.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.shoper.commons.DateUtil;
import org.shoper.schedule.pojo.TaskTemplate;

/**
 * Provider - Server connections...
 * 
 * @author ShawnShoper
 *
 */
public class TaskTemplateVO
{
	private String id;
	private String name;
	private String createTime;
	private String updateTime;
	private String url;
	private String code;

	public static TaskTemplateVO toVO(TaskTemplate taskTemplate)
	{
		TaskTemplateVO taskTemplateVO = null;
		if (taskTemplate != null)
		{
			taskTemplateVO = new TaskTemplateVO();

			Date createTime = taskTemplate.getCreateTime();
			if (createTime != null)
				taskTemplateVO.setCreateTime(
						DateUtil.dateToString(DateUtil.DATE24, createTime));
			taskTemplateVO.setId(taskTemplate.getId());
			taskTemplateVO.setName(taskTemplate.getName());
			if (taskTemplate.getCode() != null)
				taskTemplateVO.setCode(new String(taskTemplate.getCode()));
			taskTemplateVO.setUrl(taskTemplate.getUrl());
			Date updateTime = taskTemplate.getUpdateTime();
			if (updateTime != null)
				taskTemplateVO.setUpdateTime(
						DateUtil.dateToString(DateUtil.DATE24, updateTime));
		}
		return taskTemplateVO;
	}

	public static List<TaskTemplateVO> toVOs(List<TaskTemplate> taskTemplates)
	{
		List<TaskTemplateVO> taskTemplateVOs = new ArrayList<TaskTemplateVO>();
		for (TaskTemplate taskTemplate : taskTemplates)
		{
			TaskTemplateVO taskTemplateVO = toVO(taskTemplate);
			if (taskTemplateVO != null)
				taskTemplateVOs.add(taskTemplateVO);
		}
		return taskTemplateVOs;
	}
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
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

	public String getUpdateTime()
	{
		return updateTime;
	}

	public void setUpdateTime(String updateTime)
	{
		this.updateTime = updateTime;
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getCode()
	{
		return code;
	}

	public void setCode(String code)
	{
		this.code = code;
	}

}
