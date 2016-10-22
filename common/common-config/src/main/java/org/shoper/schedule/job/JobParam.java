package org.shoper.schedule.job;

import com.alibaba.fastjson.JSONObject;

/**
 * job 参数
 * 
 * @author ShawnShoper
 *
 */
public class JobParam
{
	private String category;
	private String category_name;
	private String jobCode;
	private String jobName;
	private String type;
	private String cookies;
	private String targetURL;
	private String queryURL;

	@Override
	public String toString()
	{
		return "JobParam [category=" + category + ", category_name="
				+ category_name + ", jobCode=" + jobCode + ", jobName="
				+ jobName + ", type=" + type + ", cookies=" + cookies
				+ ", targetURL=" + targetURL + ", queryURL=" + queryURL + "]";
	}
	public String getQueryURL()
	{
		return queryURL;
	}
	public void setQueryURL(String queryURL)
	{
		this.queryURL = queryURL;
	}
	public String getTargetURL()
	{
		return targetURL;
	}
	public void setTargetURL(String targetURL)
	{
		this.targetURL = targetURL;
	}
	public String getCookies()
	{
		return cookies;
	}
	public void setCookies(String cookies)
	{
		this.cookies = cookies;
	}
	public String getCategory()
	{
		return category;
	}
	public void setCategory(String category)
	{
		this.category = category;
	}
	public String getCategory_name()
	{
		return category_name;
	}
	public void setCategory_name(String category_name)
	{
		this.category_name = category_name;
	}
	public String getJobCode()
	{
		return jobCode;
	}
	public void setJobCode(String jobCode)
	{
		this.jobCode = jobCode;
	}
	public String getJobName()
	{
		return jobName;
	}
	public void setJobName(String jobName)
	{
		this.jobName = jobName;
	}
	public String getType()
	{
		return type;
	}
	public void setType(String type)
	{
		this.type = type;
	}
	public static JobParam parseObject(String json)
	{
		return JSONObject.parseObject(json, JobParam.class);
	}
}
