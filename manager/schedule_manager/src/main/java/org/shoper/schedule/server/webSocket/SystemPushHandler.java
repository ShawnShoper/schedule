package org.shoper.schedule.server.webSocket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;

import org.shoper.concurrent.future.AsynCallable;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.schedule.conf.ApplicationInfo;
import org.shoper.schedule.server.module.LogModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class SystemPushHandler implements WebSocketHandler
{
	@Autowired
	ApplicationInfo appInfo;
	@Autowired
	LogModule logModule;
	private BlockingQueue<TextMessage> messages = new LinkedBlockingQueue<TextMessage>();
	private volatile List<WebSocketSession> users = new ArrayList<>();
	@PostConstruct
	public void init()
	{
		// 启动一个线程来推送日志消息..
		FutureManager.pushFuture(appInfo.getName(), "push-message",
				new AsynCallable<Boolean, Object>() {

					@Override
					public Boolean run(Object params) throws Exception
					{
						for (;;)
						{
							TextMessage textMessage = messages.take();
							try
							{
								for (WebSocketSession user : users)
								{
									user.sendMessage(textMessage);
								}
							} catch (Exception e)
							{
								if (e instanceof InterruptedException)
								{
									return false;
								}
								messages.offer(textMessage);
							}

						}
					}
				});
	}
	@Override
	public void afterConnectionClosed(WebSocketSession session,
			CloseStatus status) throws Exception
	{
		users.remove(session);
		logModule.info(SystemPushHandler.class,
				"" + session.getLocalAddress() + " connection closed");
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session)
			throws Exception
	{
		users.add(session);
		logModule.info(SystemPushHandler.class,
				"" + session.getLocalAddress() + " connection OK");
	}

	@Override
	public void handleMessage(WebSocketSession session,
			WebSocketMessage message) throws Exception
	{
		logModule.info(SystemPushHandler.class,
				session.getLocalAddress() + " recived message");
		TextMessage tm = new TextMessage(message.getPayload() + "");
		session.sendMessage(tm);
	}

	@Override
	public void handleTransportError(WebSocketSession session,
			Throwable throwable) throws Exception
	{
		if (session.isOpen())
		{
			session.close();
		}
	}

	@Override
	public boolean supportsPartialMessages()
	{
		return false;
	}

	/**
	 * 给所有在线用户发送消息
	 * 
	 * @param message
	 */
	public void sendMessageToUsers(TextMessage message)
	{
		messages.offer(message);
	}
}