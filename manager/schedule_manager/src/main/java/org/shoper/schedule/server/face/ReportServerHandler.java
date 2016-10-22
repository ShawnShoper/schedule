package org.shoper.schedule.server.face;

import org.apache.thrift.TException;
import org.shoper.schedule.face.ReportServer;
import org.shoper.schedule.server.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * report server
 * 
 * @author ShawnShoper
 *
 */
@Component
@RequestMapping(value = "/")
public class ReportServerHandler implements ReportServer.Iface
{
	@Autowired
	TaskService taskService;
	@Override
	@RequestMapping(value = "/report", method = RequestMethod.PUT)
	public int reportJobDone(String report) throws TException
	{
		try
		{
			taskService.report(report);
		} catch (InterruptedException e)
		{
		}
		return 0;
	}

}
