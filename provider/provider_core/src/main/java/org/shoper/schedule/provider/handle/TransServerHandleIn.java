package org.shoper.schedule.provider.handle;

import java.util.List;

import org.apache.thrift.TException;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.schedule.SystemContext;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.pojo.TaskMessage;
import org.shoper.schedule.provider.job.JobCaller;
import org.shoper.schedule.provider.job.queue.JobQueue;
import org.shoper.schedule.provider.system.RunningStatus;
import org.shoper.schedule.resp.AcceptResponse;
import org.shoper.schedule.resp.StatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/**
 * Request dispatcher
 * 
 * @author ShawnShoper
 *
 */
@Component
public class TransServerHandleIn
{
	Logger log = LoggerFactory.getLogger(TransServerHandleIn.class);

	/**
	 * Receiving task
	 * 
	 * @param taskMessage
	 * @return
	 */
	public synchronized AcceptResponse receive(TaskMessage taskMessage)
	{
		AcceptResponse acceptResponse = new AcceptResponse();
		try
		{
			checkArgs(taskMessage);
			acceptResponse.setRespTime(System.currentTimeMillis());
			JobQueue.putPending(taskMessage);
			RunningStatus.serviceTimes.incrementAndGet();
			acceptResponse.setAccepted(true);
		} catch (SystemException e)
		{
			log.warn("During a exception {}", e.getLocalizedMessage(), e);
			acceptResponse.setMessage(e.getLocalizedMessage());
		}
		return acceptResponse;
	}
	/**
	 * 检查参数是否有效值
	 * 
	 * @param taskMessage
	 * @throws SystemException
	 */
	private void checkArgs(TaskMessage taskMessage)
			throws SystemException
	{
		if (taskMessage == null)
			throw new SystemException(
					"TaskMessage can not be null...");
		if (taskMessage.getTask() == null)
			throw new SystemException(
					"Task can not be null...");
		if (taskMessage.getTask().getId() == null)
			throw new SystemException(
					"Task id can not be null...");
		if (taskMessage.getTaskTemplate() == null)
			throw new SystemException(
					"TaskTemplate can not be null...");
	}
	/**
	 * Get system's status
	 * @return
	 * @throws TException
	 */
	public StatusResponse getStatus() throws TException
	{
		StatusResponse statusResponse = new StatusResponse();
		statusResponse.setServeTimes(RunningStatus.serviceTimes.get());
		statusResponse.setStartTime(SystemContext.startTime);
		statusResponse.setHoldeCount(JobQueue.getHolder());
		statusResponse.setRespTime(System.currentTimeMillis());
		return statusResponse;
	}

	public List<String> getAllRunning() throws TException
	{
		return JobQueue.getRunning();
	}

	public int kill(String id) {
		return FutureManager.stopFuture(RunningStatus.GROUP,id);
	}
}
