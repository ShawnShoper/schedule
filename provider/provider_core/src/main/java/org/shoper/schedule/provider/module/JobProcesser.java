package org.shoper.schedule.provider.module;

import org.shoper.commons.MD5Util;
import org.shoper.commons.StringUtil;
import org.shoper.concurrent.future.AsynRunnable;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.concurrent.future.RunnableCallBack;
import org.shoper.dynamiccompile.ClassLoaderHandler;
import org.shoper.schedule.SystemContext;
import org.shoper.schedule.conf.ApplicationInfo;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.module.StartableModule;
import org.shoper.schedule.pojo.TaskMessage;
import org.shoper.schedule.provider.face.in.TransServerIHandler;
import org.shoper.schedule.provider.job.JobCaller;
import org.shoper.schedule.provider.job.queue.JobQueue;
import org.shoper.schedule.provider.job.queue.ReportQueue;
import org.shoper.schedule.provider.system.RunningStatus;
import org.shoper.schedule.resp.ReportResponse;
import org.shoper.schedule.resp.ResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.crypto.dsig.TransformService;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static org.hsqldb.HsqlDateTime.e;
import static org.shoper.concurrent.future.FutureManager.pushFuture;
import static sun.misc.PostVMInitHook.run;

@Component
public class JobProcesser extends StartableModule {
	@Autowired
	ApplicationInfo appInfo;
	@Autowired
	ReportModule reportModule;
	@Autowired
	HDFSModule hdfsModule;

	private Logger log = LoggerFactory.getLogger(JobProcesser.class);

	@PostConstruct
	public void init () {
		log.info("Job processer initializing");

	}

	@PreDestroy
	public void destroy () {
		stop();
	}

	@Autowired
	TransServerIHandler transServerIHandler;

	@Override
	public int start () {
		pushFuture(RunningStatus.GROUP, "jobProcess",
				   new AsynRunnable() {
					   @Override
					   public void call () throws Exception {
						   for (; ; ) {
							   try {
								   if (transServerIHandler.getAllRunning().size() < RunningStatus.limitTask) {
									   final TaskMessage taskMessage = JobQueue
											   .takePending(60);
									   JobQueue.addRunning(taskMessage.getTask().getId(), taskMessage);
									   if (Objects.isNull(taskMessage))
										   continue;
									   doTask(taskMessage);
								   }
							   } catch (Exception e) {
								   e.printStackTrace();
							   }
						   }
					   }
				   }
		);
		super.setStarted(true);
		return 0;
	}


	@Autowired
	RedisTemplate<String, String> redisTemplate;
	@Autowired
	AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor;
	@Autowired
	ConfigurableListableBeanFactory factory;

	public JobCaller getSpringBean (String beanName, JobCaller bean) {
		DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) factory;
		autowiredAnnotationBeanPostProcessor.processInjection(bean);
		factory.registerSingleton(beanName, bean);
		JobCaller jobCaller = factory.getBean(beanName, JobCaller.class);
		defaultListableBeanFactory.destroySingleton(beanName);
		return jobCaller;
	}

	/**
	 * creat bean to spring IoC
	 *
	 * @param jobID
	 * @param taskMessage
	 * @return
	 * @throws SystemException
	 */
	JobCaller creatBean (String jobID, TaskMessage taskMessage, ClassLoaderHandler clh) throws SystemException {
		byte[] code = taskMessage.getTaskTemplate().getCode();
		Class<JobCaller> clazz;
		//创建实例后,丢进 springIOC,使其管理,以至于我们能直接在其中注入我们需要的工具
		JobCaller jobCaller;

		try {
			clazz = (Class<JobCaller>) clh.getClassFromJavaCode(new String(code, "UTF-8"));
			jobCaller = getSpringBean(jobID + System.currentTimeMillis(), clazz.newInstance());
		} catch (Exception e) {
			e.printStackTrace();
			throw new SystemException(e.getLocalizedMessage());
		}
		return jobCaller;
	}

	public void doTask (TaskMessage taskMessage) {
		//生成随机ID
		String jobID = "job" + UUID.randomUUID().toString().replaceAll("-", "");
		FutureManager.pushFuture(
				RunningStatus.GROUP,
				jobID,
				new AsynRunnable() {
					@Override
					public void call () throws Exception {
						ClassLoaderHandler clh = ClassLoaderHandler.newInstance();
						try {
							if (Objects.nonNull(taskMessage)) {
								toJobRunner(creatBean(jobID, taskMessage, clh), taskMessage);
							}
						} catch (SystemException e) {
							jobOver(taskMessage.getTask().getId(), null, ReportResponse.Error.FETAL);
							RunningStatus.failedTimes.incrementAndGet();
						} catch (InterruptedException e) {
							//Do nothing
						} finally {
//                            JobCaller jobCaller = factory.getBean(jobID, JobCaller.class);
//                            if (Objects.nonNull(jobCaller))
//                                factory.destroyBean(jobID, jobCaller);
							clh.close();
						}
					}
				}.setCallback(new RunnableCallBack() {
					@Override
					protected boolean preDo () {
						return true;
					}

					@Override
					protected void done () {
						JobQueue.removeRunning(jobID);
					}
				})
		);
	}

	/**
	 * 主要2种异常,一个编译失败异常,一个是执行失败异常....
	 *
	 * @param taskMessage
	 * @throws SystemException
	 */
	private void toJobRunner (JobCaller jobCaller, TaskMessage taskMessage) throws InterruptedException {

		JobResult result = jobCaller.getJobResult();
		String param = taskMessage.getTask().getParams();
		JobParam argsTmp;
		// 如果该任务没有任何参数,不在反序列化
		if (StringUtil.isEmpty(param)) {
			argsTmp = new JobParam();
		} else {
			argsTmp = JobParam.parseObject(param);
		}
		//检查 cookies
		if (StringUtil.isEmpty(argsTmp.getCookies())) {
			argsTmp.setCookies(taskMessage.getTask().getCookies());
		}

		JobParam args = argsTmp;
		// 监听....
		Timer timer = new Timer(true);
		result.setJobName(
				taskMessage.getTask().getName());
		result.setStartTime(System.currentTimeMillis());
		timer.schedule(new TimerTask() {
			@Override
			public void run () {
				// Do save 日志..
				ResultResponse response = new ResultResponse(
						appInfo.getBindAddr() + ":"
								+ appInfo.getPort(), result.getJobName(),
						result.isDone(),
						result.getSaveCount().get(),
						result.isSuccess(),
						result.getUpdateCount().get(),
						result.getHandleCount().get(),
						result.getStartTime(),
						result.getEndTime(),
						System.currentTimeMillis()
								- result.getStartTime()
				);
				redisTemplate.opsForList().rightPush("progress", response.toJson());
			}
		}, 0, 5000);
		try {
			if (!jobCaller.init(args))
				throw new SystemException("参数解析失败...");
			//init();run(),destroy();
			jobCaller.run();
			log.info("{} job has be success.", taskMessage.getTask().getId());
			RunningStatus.successTimes.incrementAndGet();
			// 发送通知给队列...
			jobOver(taskMessage.getTask().getId(), result, null);
		} catch (SystemException e1) {
			e1.printStackTrace();
			log.error(
					"{} job has be failed.{}",
					taskMessage.getTask().getId(), e1
			);
			RunningStatus.failedTimes.incrementAndGet();
			e1.printStackTrace();
			jobOver(taskMessage.getTask().getId(), null,
					ReportResponse.Error.FETAL
			);
		} finally {
			result.setEndTime(System.currentTimeMillis());
			log.info(
					"{} job has be done.destroying",
					taskMessage.getTask().getId()
			);
			ResultResponse response = new ResultResponse(
					appInfo.getBindAddr() + ":"
							+ appInfo.getPort(),
					result.getJobName(), result.isDone(),
					result.getSaveCount().get(),
					result.isSuccess(),
					result.getUpdateCount().get(),
					result.getHandleCount().get(),
					result.getStartTime(),
					result.getEndTime(),
					System.currentTimeMillis()
							- result.getStartTime()
			);
			redisTemplate.opsForList().rightPush("progress", response.toJson());
			timer.cancel();
			jobCaller.destroy();
		}
	}

	/**
	 * 任务结束
	 *
	 * @param id
	 * @param jobResult
	 * @param error
	 */
	public void jobOver (String id, JobResult jobResult, ReportResponse.Error error) {
		JobQueue.removeRunning(id);
		ReportResponse reportResponse = new ReportResponse();
		reportResponse.setProviderToken(
				MD5Util.GetMD5Code((RunningStatus.HOST + RunningStatus.PORT)));
		reportResponse.setGroup(RunningStatus.GROUP);
		reportResponse.setJob(id);
		if (error != null)
			reportResponse.setErr(error);

		if (jobResult != null)
			reportResponse.setJobResult(jobResult);

		ReportQueue.putReport(reportResponse);
	}

	@Override
	public void stop () {
		FutureManager.futureDone(appInfo.getName(), "jobProcess");
		log.info("Job processer 	destroying");
		setStarted(false);
	}
}
