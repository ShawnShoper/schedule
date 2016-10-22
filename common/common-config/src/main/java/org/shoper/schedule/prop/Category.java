package org.shoper.schedule.prop;

public enum Category
{
	TRAVEL_NEWS("travel_news", "旅游新闻"), TRAVEL_STRATEGY("travel_strategy",
			"旅游游记"), TRAVEL_REMARK("travel_remark", "旅游点评"), BaiDU_Top(
					"baidu_top", "百度top"), Weibo("Weibo", "微博");
	private Category(String code, String name)
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
