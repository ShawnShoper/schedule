package org.shoper.schedule.resp;

import com.alibaba.fastjson.JSONObject;

/**
 * Accept response.<br>
 * response current provider has accepted.
 * @author ShawnShoper
 *
 */
public class AcceptResponse extends ResponseBase
{
	private static final long serialVersionUID = 2379566254811772239L;
	private boolean accepted = false;
	private String message;
	public String toJson(){
		return JSONObject.toJSONString(this);
	}
	public static AcceptResponse parseObject(String json){
		return JSONObject.parseObject(json,AcceptResponse.class);
	}
	public String getMessage()
	{
		return message;
	}
	public void setMessage(String message)
	{
		this.message = message;
	}
	public boolean isAccepted()
	{
		return accepted;
	}
	public void setAccepted(boolean accepted)
	{
		this.accepted = accepted;
	}
	
}
