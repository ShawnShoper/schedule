package org.shoper.schedule.server.module;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.zookeeper.KeeperException;
import org.shoper.concurrent.future.AsynCallable;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.mail.MailBuilder;
import org.shoper.mail.MailInfo;
import org.shoper.schedule.conf.ApplicationInfo;
import org.shoper.schedule.conf.MailConf;
import org.shoper.schedule.conf.ZKInfo;
import org.shoper.schedule.manager.ZKModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
@Component
public class MailModule extends ZKModule
{
	@Autowired
	LogModule logModule;
	@Autowired
	ApplicationInfo appInfo;
	@Autowired
	MailConf mailConf;
	@Autowired
	org.shoper.schedule.conf.ZKInfo ZKInfo;
	MailInfo mailInfo;
	private AtomicInteger statu = new AtomicInteger(0);

	@PostConstruct
	public void init()
	{
		setZkInfo(ZKInfo);
	}
	@PreDestroy
	public void destroy()
	{
		super.stop();
	}
	@Override
	public int start()
	{
		if (super.start() == 1)
			return 1;
		try
		{
			mailInfo = readInfo();
			if (statu.get() == 0)
			{
				FutureManager.pushFuture(appInfo.getName(), "mail-sender",
										 new AsynCallable<Boolean, Object>() {

											 @Override
											 public Boolean run(Object param) throws Exception
											 {
												 sendMailProcesser();
												 return true;
											 }
										 });
			}
		} catch (InterruptedException e)
		{
			return 1;
		}
		setStarted(true);
		return 0;
	}
	/**
	 * read info from zookeeper
	 *
	 * @return
	 * @throws InterruptedException
	 */
	private MailInfo readInfo() throws InterruptedException
	{
		MailInfo mailInfo = null;
		int retry = 1;
		while (mailInfo == null && retry < 3)
		{
			try
			{
				byte[] info = getZkClient().showData(mailConf.getNodePath());
				String conf = new String(info);
				mailInfo = JSONObject.parseObject(conf, MailInfo.class);
			} catch (KeeperException e)
			{
				logModule.error(MailModule.class, "从 zookeeper读取配置失败..", e);
			} finally
			{
				retry++;
			}
		}
		if (mailInfo == null)
			statu.incrementAndGet();
		return mailInfo;
	}
	private BlockingQueue<Map<String, String>> mailQueue = new LinkedBlockingQueue<>();
	/**
	 * 发送消息
	 *
	 * @param subject
	 *            主题
	 * @param message
	 *            消息
	 */
	public void sendMessage(String subject, String message)
	{
		Map<String, String> data = new HashMap<String, String>();
		data.put("subject", subject);
		data.put("content", message);
		mailQueue.offer(data);
	}
	/**
	 * 邮件发送处理器
	 */
	public void sendMailProcesser()
	{
		sendMsg : for (;;)
		{
			try
			{
				Map<String, String> mailMsg = mailQueue.take();
				MailBuilder.getInstances()
						.setAccount(mailInfo.getAccount(),
									mailInfo.getPassword())
						.setAuth(mailInfo.isAuth()).setCharset("utf-8")
						.setSMTPServer(mailInfo.getSmtp())
						.setSubject(mailMsg.get("subject"))
						.setTo(mailInfo.getTo())
						.setContent(mailMsg.get("content")).build().send();

			} catch (InterruptedException e)
			{
				break sendMsg;
			} catch (Exception e)
			{
				logModule.error(MailModule.class, "发送邮件失败.", e);
			}
		}
		logModule.info(MailModule.class, "邮件发送器,终止...");
	}
}
