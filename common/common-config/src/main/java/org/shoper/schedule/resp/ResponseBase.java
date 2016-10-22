package org.shoper.schedule.resp;

import org.shoper.schedule.pojo.BasePojo;

/**
 * response base class
 * 
 * @author ShawnShoper
 *
 */
public class ResponseBase extends BasePojo
{
	private static final long serialVersionUID = 8291504360245445587L;
	private long respTime;
	public long getRespTime()
	{
		return respTime;
	}
	public void setRespTime(long respTime)
	{
		this.respTime = respTime;
	}

}
