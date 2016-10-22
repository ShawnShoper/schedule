package org.shoper.schedule.resp;

import com.alibaba.fastjson.JSONObject;

/**
 * Status response..
 *
 * @author ShawnShoper
 */
public class StatusResponse extends ResponseBase {
	private static final long serialVersionUID = 805407221791241137L;
	private long serveTimes;
	private boolean enable;
	private double priority;
	/**
	 * Start time
	 */
	private long startTime;
	/**
	 * Holding task count
	 */
	private long holdeCount;
	// cpu idle percent
	private double cpuIdlePercent;
	// mem used percent
	private double memUsedPercent;

	public double getPriority () {
		return priority;
	}

	public void setPriority (double priority) {
		this.priority = priority;
	}

	public double getCpuIdlePercent () {
		return cpuIdlePercent;
	}

	public void setCpuIdlePercent (double cpuIdlePercent) {
		this.cpuIdlePercent = cpuIdlePercent;
	}

	public static long getSerialversionuid () {
		return serialVersionUID;
	}

	public double getMemUsedPercent () {
		return memUsedPercent;
	}

	public void setMemUsedPercent (double memUsedPercent) {
		this.memUsedPercent = memUsedPercent;
	}

	public long getHoldeCount () {
		return holdeCount;
	}

	public void setHoldeCount (long holdeCount) {
		this.holdeCount = holdeCount;
	}

	public long getServeTimes () {
		return serveTimes;
	}

	public void setServeTimes (long serveTimes) {
		this.serveTimes = serveTimes;
	}

	public long getStartTime () {
		return startTime;
	}

	public void setStartTime (long startedTime) {
		this.startTime = startedTime;
	}

	public static StatusResponse parseObject (String json) {
		return JSONObject.parseObject(json, StatusResponse.class);
	}
	public boolean isEnable () {
		return enable;
	}
	public void setEnable (boolean enable) {
		this.enable = enable;
	}
	public String toJson () {
		return JSONObject.toJSONString(this);
	}
}
