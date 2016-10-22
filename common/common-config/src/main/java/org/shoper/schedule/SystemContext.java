package org.shoper.schedule;

import org.shoper.concurrent.future.FutureManager;
import org.springframework.context.ConfigurableApplicationContext;
/**
 * 系统操作类.存放 Spring context
 *
 * @author ShawnShoper
 *
 */
public class SystemContext
{
	private volatile static Boolean running = true;
	private static Boolean monitor = false;
	/**
	 * System running time
	 */
	public static final long startTime = System.currentTimeMillis();
	public static ConfigurableApplicationContext context;
	/**
	 * 关闭程序
	 */
	public static void shutdown()
	{
		try
		{
			if (context != null)
				context.close();
			synchronized (monitor)
			{
				running = false;
				running.notify();
			}
			FutureManager.close();
		} catch (Exception e)
		{
			System.exit(1);
		}
	}
	public static <T> T getBean(String beanName, Class<T> t)
	{
		return context.getBean(beanName, t);
	}
	/**
	 * Waiting to spring context to shutdown
	 */
	public static void waitShutdown()
	{
		synchronized (monitor)
		{
			while (running)
			{
				try
				{
					monitor.wait();
				} catch (InterruptedException e)
				{
					break;
				}
			}
		}
	}
}
