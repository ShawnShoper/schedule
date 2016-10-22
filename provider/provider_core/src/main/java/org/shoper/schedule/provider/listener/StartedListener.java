package org.shoper.schedule.provider.listener;

import org.shoper.commons.StringUtil;
import org.shoper.schedule.SystemContext;
import org.shoper.schedule.conf.ApplicationInfo;
import org.shoper.schedule.conf.ProviderInfo;
import org.shoper.schedule.conf.ZKInfo;
import org.shoper.schedule.provider.module.*;
import org.shoper.schedule.provider.system.RunningStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static org.hsqldb.HsqlDateTime.e;

@Component
public class StartedListener
		implements
			ApplicationListener<ContextRefreshedEvent>
{
	@Autowired
	ZKInfo zkInfo;
	@Autowired
	Registrar registrar;
	@Autowired
	JobProcesser jobProcesser;
	@Autowired
	ReportModule reportModule;
	@Autowired
	HDFSModule hdfsModule;
	@Autowired
	ProviderInfo providerInfo;
	@Autowired
	ApplicationInfo appInfo;
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event)
	{
		// 启动 providerScanner 模块
		try
		{
			if (StringUtil.isEmpty(providerInfo.getGroup()))
				throw new NullPointerException();
			RunningStatus.GROUP = providerInfo.getGroup();
			RunningStatus.PORT = providerInfo.getPort();
			RunningStatus.HOST = appInfo.getBindAddr();
			reportModule.start();
			registrar.start();
			jobProcesser.start();
			hdfsModule.start();
		} catch (Exception e)
		{
			e.printStackTrace();
			SystemContext.shutdown();
		} finally
		{
		}
	}
}
