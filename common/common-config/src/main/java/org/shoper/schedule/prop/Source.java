package org.shoper.schedule.prop;

public enum Source
{
	BAIDU_TRIP("baidu_trip", "百度旅游");
	private Source(String code, String name)
	{
		this.code = code;
		this.name = name;
	}
	private String code;
	private String name;
	public String getCode()
	{
		return code;
	}
	public void setCode(String code)
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

}
