package org.shoper.schedule.provider.job.queue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.shoper.schedule.resp.ReportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * report queue
 * 
 * @author ShawnShoper
 *
 */
public class ReportQueue
{

	private static Logger logger = LoggerFactory.getLogger(ReportQueue.class);
	private static volatile LinkedBlockingQueue<ReportResponse> finishedQueue = new LinkedBlockingQueue<ReportResponse>();
	/**
	 * 获取一个 report..如果没有阻塞..
	 * 
	 * @return
	 */
	public static ReportResponse takeReport()
	{
		logger.info("Requesting a report.....");
		ReportResponse reportResponse = null;
		for (;;)
		{
			try
			{
				reportResponse = finishedQueue.poll(1000, TimeUnit.SECONDS);
				if (reportResponse != null)
				{
					logger.info("take a report	{}", reportResponse);
					break;
				}
			} catch (InterruptedException e)
			{
				;
			}
		}
		return reportResponse;
	}
	/**
	 * 添加 report 入队列.. 并通知接受方接收信息.
	 * 
	 * @param rr
	 * @return
	 */
	public static boolean putReport(ReportResponse rr)
	{
		return finishedQueue.offer(rr);
	}
}
