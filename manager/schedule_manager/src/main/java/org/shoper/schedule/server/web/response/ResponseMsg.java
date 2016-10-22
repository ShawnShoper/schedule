package org.shoper.schedule.server.web.response;

import com.alibaba.fastjson.JSONObject;

/**
 * 用于嵌套关系
 * 
 * @author ShawnShoper
 *
 */
public class ResponseMsg extends Response
{
	private Object data;

	public Object getData()
	{
		return data;
	}

	public void setData(Object data)
	{
		this.data = data;
	}
	public String toJson()
	{
		return JSONObject.toJSONString(this);
	}
}
