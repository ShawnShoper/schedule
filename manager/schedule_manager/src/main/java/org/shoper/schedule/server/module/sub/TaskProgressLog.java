package org.shoper.schedule.server.module.sub;

import org.shoper.commons.DateUtil;
import org.shoper.concurrent.future.AsynCallable;
import org.shoper.concurrent.future.FutureCallback;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.schedule.conf.ApplicationInfo;
import org.shoper.schedule.module.StartableModule;
import org.shoper.schedule.resp.ResultResponse;
import org.shoper.schedule.server.module.LogModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TaskProgressLog extends StartableModule
{
	@Autowired
	ApplicationInfo appInfo;
	@Autowired
	LogModule logModule;
	@Autowired
	RedisTemplate<String,String> redisTemplate;
	@Override
	public int start()
	{
		FutureManager.pushFuture(appInfo.getName(), "log-progress-accepter",
				new AsynCallable<Boolean, Object>() {
					@Override
					public Boolean run(Object param) throws Exception
					{
						for (;;)
						{
							try
							{
								String progress =
										redisTemplate.opsForList().rightPop("progress", 10, TimeUnit.SECONDS);
								if(progress==null)continue;
								ResultResponse response = ResultResponse
										.parseObject(progress);
								if (response != null)
								{
									String message = String.format(
											"任务名:[%s],执行端地址:[%s]启动时间:[%s],结束时间[%s],处理量:[%d],新增量:[%d],更新量:[%d],耗时:[%s]",

											response.getJobName(),
											response.getAddr(),
											DateUtil.TimeToString(
													DateUtil.DATE24_CN,
													response.getStartTime()),
											response.getEndTime() == 0
													? ""
													: DateUtil.TimeToString(
															DateUtil.DATE24_CN,
															response.getEndTime()),
											response.getHandCount(),
											response.getSaveCount(),
											response.getUpdateCount(),
											DateUtil.TimeToStr(response
													.getTimeConsuming()));
									logModule.info(TaskProgressLog.class,
											message);
								}
							} catch (Exception e)
							{
								// 因为如果没有明抛InterruptedException.是无法直接捕获该异常的。所以需要用
								// instanceof 判断子异常是否是'打断异常'
								if (e instanceof InterruptedException)
								{
									return false;
								}
								logModule.error(TaskProgressLog.class,
										"获取进度日志失败..", e);
							}
						}
					}
				}.setCallback(new FutureCallback<Boolean, Object>() {
					@Override
					public void fail(Exception e)
					{
						logModule.error(TaskProgressLog.class, "获取进度日志失败..", e);
					}


				}));
		FutureManager.pushFuture(appInfo.getName(), "log-task-progress-accepter",
				new AsynCallable<Boolean, Object>() {
					@Override
					public Boolean run(Object param) throws Exception
					{
						for (;;)
						{
							try{
								String progress =redisTemplate.opsForList().rightPop("task-message",10, TimeUnit.SECONDS);
								if(progress==null)continue;
								logModule.info(TaskProgressLog.class,
										progress);
							}catch(Exception e){
								e.printStackTrace();
							}
						}
					}
				}.setCallback(new FutureCallback<Boolean, Object>() {
					@Override
					public void fail(Exception e)
					{
						logModule.error(TaskProgressLog.class, "获取进度task log 失败..", e);
					}
				}));
		setStarted(true);
		return 0;
	}
	@Override
	public void stop()
	{
		setStarted(false);
	}
}
