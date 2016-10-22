package org.shoper.schedule.provider.system;

import org.shoper.commons.exception.ShoperException;
import org.shoper.monitor.SystemUtil;
import org.shoper.schedule.exception.SystemException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * provider running status..
 * 
 * @author ShawnShoper
 *
 */
public class RunningStatus
{
	static class Sum
	{
		private int sum;
		public int getSum()
		{
			return sum;
		}
		public void add(int num)
		{
			this.sum += num;
		}
	}
	// Fetch system cpu of the immutable
	static
	{
		// Get this machine cpu info;
		try
		{
			Sum totalMhz = new Sum();
			Arrays.stream(SystemUtil.getCpuInfoList()).forEach(cpu ->
			{
				totalMhz.add(cpu.getMhz());
			});
			cpuWeight = Integer.parseInt(totalMhz.getSum() / 1000 + "");

		} catch (ShoperException e)
		{
			throw new ExceptionInInitializerError(e);
		}
	}
	public static int PORT;
	public static String HOST;
	public static String GROUP;
	public static int cpuWeight;
	//限制当前服务,可同时执行的任务数
	public static volatile int limitTask = 1;
	public static AtomicLong serviceTimes = new AtomicLong(0);
	public static AtomicLong failedTimes = new AtomicLong(0);
	public static AtomicLong successTimes = new AtomicLong(0);
	public static double getCpuIdlePercent()
			throws SystemException
	{
		try
		{
			return BigDecimal.valueOf(SystemUtil.getCpuPercInfo().getIdle())
					.setScale(4, BigDecimal.ROUND_HALF_EVEN)
					.multiply(new BigDecimal(100)).doubleValue();
		} catch (ShoperException e)
		{
			throw new SystemException(e);
		}
	}

	public static double getMemUsedPercent()
			throws SystemException
	{
		try
		{

			return BigDecimal.valueOf(SystemUtil.getMemInfo().getUsedPercent())
					.setScale(4, BigDecimal.ROUND_HALF_EVEN).doubleValue();
		} catch (ShoperException e)
		{
			throw new SystemException(e);
		}
	}
}
