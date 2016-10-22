package org.shoper.schedule.server.module;

import org.shoper.schedule.server.webSocket.SystemPushHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

@Component
public class LogModule
{
	@Autowired
	SystemPushHandler systemPushHandler;
	private Logger logger = LoggerFactory.getLogger(LogModule.class);
	public void info(Class<?> clazz, String log)
	{
		String msg = "[" + clazz.getName() + "]-->" + log;
		logger.info(msg);
		systemPushHandler.sendMessageToUsers(new TextMessage(msg.getBytes()));
	}

	public void debug(Class<?> clazz, String log)
	{
		String msg = "[" + clazz.getName() + "]-->" + log;
		logger.debug(msg);
		systemPushHandler.sendMessageToUsers(new TextMessage(msg.getBytes()));
	}
	public void error(Class<?> clazz, String log, Exception e)
	{
		String msg = "[" + clazz.getName() + "]-->" + log + ",error info ->"
				+ e.getLocalizedMessage();
		logger.error(msg, e);
		systemPushHandler.sendMessageToUsers(new TextMessage(msg.getBytes()));
	}
	public void error(Class<?> clazz, String log)
	{
		String msg = "[" + clazz.getName() + "]-->" + log;
		logger.error(msg);
		systemPushHandler.sendMessageToUsers(new TextMessage(msg.getBytes()));
	}
	public void warn(Class<?> clazz, String log)
	{
		String msg = "[" + clazz.getName() + "]-->" + log;
		logger.warn(msg);
		systemPushHandler.sendMessageToUsers(new TextMessage(msg.getBytes()));
	}
}
