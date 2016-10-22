package org.shoper.schedule.server.service;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.quartz.CronExpression;
import org.shoper.common.rpc.manager.NodeManager;
import org.shoper.commons.StringUtil;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.face.TransServer;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.pojo.Task;
import org.shoper.schedule.pojo.TaskMessage;
import org.shoper.schedule.pojo.TaskTemplate;
import org.shoper.schedule.resp.ReportResponse;
import org.shoper.schedule.server.dao.TaskDao;
import org.shoper.schedule.server.job.TaskManager;
import org.shoper.schedule.server.job.TaskTrack;
import org.shoper.schedule.server.module.LogModule;
import org.shoper.schedule.server.module.MailModule;
import org.shoper.schedule.server.pojo.NotifyTask;
import org.shoper.schedule.server.system.RunningStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TaskService {
	@Autowired
	LogModule logModule;
	@Autowired
	TaskDao taskDao;
	@Autowired
	TaskManager taskManager;
	@Autowired
	NodeManager providerManager;

	/**
	 * 新增任务
	 *
	 * @param name,url,temlateID
	 * @throws InterruptedException
	 */
	public void addTask (String name, String url, String templateID,
						 String cookies, String params, String cronexp)
			throws SystemException, InterruptedException {
		// check args
		if (StringUtil.isEmpty(name))
			throw new SystemException("任务参数 [名称] 不能为空...");
		if (StringUtil.isEmpty(url))
			throw new SystemException("任务参数 [链接地址] 不能为空...");
		if (StringUtil.isEmpty(templateID))
			throw new SystemException("任务参数 [任务模版 ID] 不能为空...");
		if (!taskDao.taskTemplateHasExistsByID(templateID))
			throw new SystemException("指定的任务模版不存在...");
		Task task = new Task();
		task.setName(name);
		task.setUrl(url);
		task.setTemplateID(templateID);
		task.setCookies(cookies);
		task.setParams(params);
		// Disable timing ,if cron exp is null or empty
		if (!StringUtil.isEmpty(cronexp)) {
			if (CronExpression.isValidExpression(cronexp)) {
				task.setCronExpress(cronexp);
				task.setTiming(true);
			} else
				throw new SystemException("Cron表达式不合法...");
		}
		task.setCreateTime(System.currentTimeMillis());
		task.setLastFinishTime(0);
		taskDao.addTask(task);
		task = taskDao.getTaskByName(task.getName());
		taskManager.addTask(task);
	}

	/**
	 * 获取指定时间段之前的任务
	 *
	 * @param interval
	 * 		时间间隔
	 * @param unit
	 * 		时间单位
	 * @return
	 */
	public List<Task> getTask (int interval, TimeUnit unit, boolean isTiming) {
		// if (interval == null)
		// throw new IllegalArgumentException("The interval can not be null");
		if (unit == null)
			throw new IllegalArgumentException("The unit can not be null");
		List<Task> tasks = taskDao.getTask(interval, unit, isTiming);
		return tasks;
	}

	/**
	 * 获取定时任务..所有的..
	 *
	 * @return 定时任务集合
	 */
	public List<Task> getTimingTask () {
		return getTask(0, TimeUnit.SECONDS, true);
	}

	/**
	 * 获取 taskMessage
	 *
	 * @return
	 */
	public TaskMessage getTaskMessage (Task task) {
		TaskTemplate taskTemplate = taskDao
				.getTaskTemplate(task.getTemplateID());
		TaskMessage taskMessage = null;
		if (StringUtil.isEmpty(task.getCookies()))
			task.setCookies(taskTemplate.getCookies());
		if (taskTemplate != null)
			taskMessage = new TaskMessage(task, taskTemplate);
		return taskMessage;
	}

	/**
	 * 获取 task template
	 *
	 * @param tasks
	 * @return
	 */
	public List<TaskMessage> getTaskTemplate (List<Task> tasks) {

		List<TaskMessage> taskMessages = new ArrayList<>();
		for (Task task : tasks) {
			TaskMessage taskMessage = getTaskMessage(task);
			if (taskMessage != null)
				taskMessages.add(taskMessage);
		}
		return taskMessages;
	}

	public TaskTemplate getTaskTemplate (String templateID)
			throws SystemException {
		if (StringUtil.isEmpty(templateID))
			throw new SystemException("Tasktemplate ID can't be null or empty");
		return taskDao.getTaskTemplate(templateID);
	}

	/**
	 * 获取task 分页
	 *
	 * @return
	 * @throws SystemException
	 */
	public List<Task> getTask (String offset, String limit, String search,
							   String sort, String order) throws SystemException {
		vilidate(offset, limit, search, sort, order);
		int offset_i = Integer.valueOf(offset);
		if (offset_i < 0)
			offset_i = 0;
		int limit_i = Integer.valueOf(limit);
		if (limit_i < 1 || limit_i > 50)
			limit_i = 1;
		return taskDao.getTask(offset_i, limit_i, search, sort, order);
	}

	/**
	 * 获取task template 分页
	 *
	 * @return
	 * @throws SystemException
	 */
	public List<TaskTemplate> getTaskTemplate (String offset, String limit,
											   String search, String sort, String order) throws SystemException {
		vilidate(offset, limit, search, sort, order);
		int offset_i = Integer.valueOf(offset);
		if (offset_i < 0)
			offset_i = 0;
		int limit_i = Integer.valueOf(limit);
		if (limit_i < 1 || limit_i > 50)
			limit_i = 10;
		return taskDao.getTaskTemplate(offset_i, limit_i, search, sort, order);
	}

	private void vilidate (String offset, String limit, String search,
						   String sort, String order) throws SystemException {
		if (StringUtil.isNull(offset) || !StringUtil.isDigit(offset))
			throw new SystemException("Offset must not be null and digit");
		if (StringUtil.isNull(limit) || !StringUtil.isDigit(limit))
			throw new SystemException("Limit must not be null and digit");
		if (!StringUtil.isEmpty(order) && !(order.equalsIgnoreCase("desc")
				|| order.equalsIgnoreCase("asc")))
			throw new SystemException(
					"Order must not be null and only be DESC or ASC");
	}

	/**
	 * 获取 task 总数量
	 *
	 * @return
	 */
	public long getTaskSize (String search) {
		return taskDao.getTaskSize(search);
	}

	public long getTaskTemplateSize (String search) {
		return taskDao.getTaskTemplateSize(search);
	}

	/**
	 * client task report
	 *
	 * @param report
	 * @return
	 * @throws InterruptedException
	 */
	public int report (String report) throws InterruptedException {
		logModule.info(TaskService.class, "接受到新汇报消息...[" + report + "]");
		// parse report to Object..
		if (StringUtil.isEmpty(report))
			return 1;
		ReportResponse reportResponse = ReportResponse.parseObject(report);
		// check args
		String job = reportResponse.getJob();
		String providerToken = reportResponse.getProviderToken();
		JobResult jobResult = reportResponse.getJobResult();
		ReportResponse.Error error = reportResponse.getErr();
		if (error == ReportResponse.Error.NONE) {
			taskDao.taskDone(job, jobResult.getEndTime(),
							 jobResult.getSaveCount()
			);
			RunningStatus.addCount.addAndGet(jobResult.getSaveCount().get());
			RunningStatus.updateCount
					.addAndGet(jobResult.getUpdateCount().get());
			RunningStatus.timeConsuming.addAndGet(jobResult.getTimeConsuming());
		} else {
			Task task = taskDao.getTaskByID(job);
			boolean fetal = false;
			if (ReportResponse.Error.EXCEP.equals(error)) {
				if (task.getFailedCount() == 2)
					fetal = true;
				else {
					taskDao.taskFailed(job);
					taskManager.returnTask(task);
				}
			} else if (ReportResponse.Error.FETAL.equals(error))
				fetal = true;
			if (fetal)
				taskDao.disabledTask(job);
		}
		TaskTrack.removeTask(providerToken, job);
		providerManager.putBack(reportResponse.getGroup(), providerToken);
		return 0;
	}

	/**
	 * 设置状态
	 *
	 * @param id
	 * 		id
	 * @param value
	 * 		状态值
	 * @param type
	 * 		类型 0 task template,1 task
	 * @throws SystemException
	 * @throws InterruptedException
	 */
	public void inverseStatus (String id, String value, String type)
			throws SystemException, InterruptedException {

		if (StringUtil.isEmpty(id))
			throw new SystemException("The id must be not null or empty!");
		if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
			throw new SystemException("The inverse value is invalid");
		if (StringUtil.isNull(type) || !StringUtil.isDigit(type))
			throw new SystemException("The type must be not null and digit");
		int type_i = Integer.valueOf(type);
		if (type_i != 0 && type_i != 1)
			throw new SystemException("The type must be 0 or 1");
		taskDao.inverseStatus(id, Boolean.valueOf(value), type_i);
		List<Task> tasks = new ArrayList<>();
		if (type_i == 0x01) {
			Task task = taskDao.getTaskByID(id);
			if (null != task)
				tasks.add(task);
		} else if (type_i == 0x00) {
			List<Task> tasks2 = taskDao.getTaskByTemplateID(id);
			if (null != tasks2 && !tasks2.isEmpty())
				tasks.addAll(tasks2);
		}
		// 并行修改内存中task 状态
		Arrays.stream(tasks.toArray(new Task[]{})).parallel().forEach(task ->
																	  {
																		  try {
																			  taskDao.inverseStatus(task.getId(), Boolean.valueOf(value), 1);
																			  taskManager.inverseStatus(task, Boolean.valueOf(value));
																		  } catch (Exception e) {
																			  e.printStackTrace();
																		  }
																	  });

		logModule.info(TaskService.class, "更改 ‘" + (type_i == 1 ? "任务" : "任务模版")
				+ "’ 状态，当前状态【" + (Boolean.valueOf(value) ? "启用" : "禁用") + "】");
	}

	/**
	 * 添加 task template
	 *
	 * @param name
	 * @param url
	 * @param cookies
	 * @param template
	 * @throws SystemException
	 */
	public void addTaskTemplate (String name, String url, String cookies,
								 String template) throws SystemException {
		if (StringUtil.isEmpty(name))
			throw new SystemException("The name must be not null or empty!");
		if (StringUtil.isEmpty(template))
			throw new SystemException(
					"The template must be not null or empty!");
		// check task template's name is exists。
		if (!taskDao.taskTemplateHasExists(name)) {
			TaskTemplate newadd = new TaskTemplate();
			newadd.setName(name);
			newadd.setUrl(url);
			newadd.setCookies(cookies);
			newadd.setCode(template.getBytes());
			newadd.setCreateTime(new Date());
			newadd.setGroup("normal");
			taskDao.addTaskTemplate(newadd);
		} else
			throw new SystemException("The name has be exists");
	}

	@Autowired
	MailModule mailModule;

	public void notifyTask (String type, String domain) throws SystemException {
		logModule.info(TaskService.class, "Notify task request.domain is ["
				+ domain + "],operate type is [" + type + "].");
		if (!StringUtil.isDigit(type))
			throw new SystemException("启停用参数 Type 不能为 null 且必须为数字 (0 、1)");
		if (StringUtil.isEmpty(domain))
			throw new SystemException("网站域参数 domain 不能为空...");
		int type_int = Integer.valueOf(type);
		if (type_int != 0 && type_int != 1)
			throw new SystemException("启停用参数 Type 不能为 null 且必须为数字 (0 、1)");
		NotifyTask notifyTask = new NotifyTask(type_int, domain);
		notifyTask.setCreateTime(new Date());
		NotifyTask nt = taskDao.notifyTaskInfo(domain);
		if (nt != null && nt.getType() == type_int)
			throw new SystemException("当前操作的 Domain 的状态与更改状态一致...");
		else {
			taskDao.pushNotifyTask(notifyTask);
			// 发送邮件通知....
			mailModule.sendMessage("任务通知", "新任务通知:域 [" + domain + "],操作类型["
					+ (type_int == 0 ? "启用" : "禁用") + "].");
		}

	}

	/**
	 * 删除指定的任务 ID
	 *
	 * @param id
	 * 		需要删除的 ID
	 * @throws SystemException
	 */
	public void deleteTask (String id) throws SystemException {
		if (StringUtil.isEmpty(id))
			throw new SystemException("Task id can not be null or empty.");
		if (!taskDao.taskHasExistsByID(id))
			throw new SystemException("The specified task id has not exists..");
		taskManager.removeTask(taskDao.getTaskByID(id));
		taskDao.deleteTask(id);

	}

	public void deleteTaskTemp (String id) throws SystemException {
		if (StringUtil.isEmpty(id))
			throw new SystemException("Task id can not be null or empty.");
		if (!taskDao.taskTemplateHasExistsByID(id))
			throw new SystemException(
					"The specified task template id has not exists..");
		taskDao.deleteTaskTemp(id);
	}

	public String getTaskGroup (String id) {

		return taskDao.getTaskGroup(id);
	}

	/**
	 * 根据 ID 获取 Task
	 *
	 * @param id
	 * @return
	 * @throws SystemException
	 */
	public Task getTaskById (String id) throws SystemException {
		if (StringUtil.isEmpty(id))
			throw new SystemException("Task id can't be null or empty...");
		Task task = taskDao.getTaskByID(id);
		return task;
	}

	public void editTask (String id, String name, String url, String cookies,
						  String params, String cronexp) throws SystemException {
		// check args
		if (StringUtil.isEmpty(name))
			throw new SystemException("任务参数 [名称] 不能为空...");
		// Disable timing ,if cron exp is null or empty
		if (!StringUtil.isEmpty(cronexp))
			if (!CronExpression.isValidExpression(cronexp))
				throw new SystemException("Cron表达式不合法...");
		Task task = getTaskById(id);
		if (task == null)
			throw new SystemException("不存在该任务.ID[" + id + "]");
		task.setName(name);
		task.setUrl(url);
		task.setCookies(cookies);
		task.setParams(params);
		if (StringUtil.isEmpty(cronexp)) {
			task.setTiming(false);
			task.setCronExpress(null);
		} else
			task.setCronExpress(cronexp);
		taskDao.editTask(task);
	}

	public void editTaskTaskTemplate (String id, String name, String url,
									  String cookies, String template) throws SystemException {
		if (StringUtil.isEmpty(name))
			throw new SystemException("任务参数 [名称] 不能为空...");
		TaskTemplate taskTemplate = getTaskTemplateById(id);
		if (taskTemplate == null)
			throw new SystemException("不存在该任务.ID[" + id + "]");
		taskTemplate.setCode(template.getBytes());
		taskTemplate.setCookies(cookies);
		taskTemplate.setUrl(url);
		taskDao.editTaskTemplate(taskTemplate);
	}

	private TaskTemplate getTaskTemplateById (String id) throws SystemException {
		if (StringUtil.isEmpty(id))
			throw new SystemException("任务参数 [ID] 不能为空...");

		return taskDao.getTaskTemplateByID(id);
	}

	public void updateTaskStatus (String token) {
		if (StringUtil.isEmpty(token))
			throw new NullPointerException(
					"Task token must not be null or empty");
		taskDao.updateTaskStatus(token);

	}

	public void killRunningTask (String id, String host, int port) throws TException {
		TSocket socket = null;
		try {
			TransServer.Client transServerClient;
			socket = new TSocket(host, port);
			TBinaryProtocol protocol = new TBinaryProtocol(socket);
			TMultiplexedProtocol mp1 = new TMultiplexedProtocol(
					protocol,
					TransServer.class.getName()
			);
			transServerClient = new TransServer.Client(mp1);
			try {
				socket.open();
			} catch (TTransportException e) {
				e.printStackTrace();
			}
			transServerClient.kill(id);
		} finally {
			if (socket != null) socket.close();
		}
	}

	public List<String> showAllTask (String host, int port) throws TException {
		TSocket socket = null;
		try {
			TransServer.Client transServerClient;
			socket = new TSocket(host, port);
			TBinaryProtocol protocol = new TBinaryProtocol(socket);
			TMultiplexedProtocol mp1 = new TMultiplexedProtocol(
					protocol,
					TransServer.class.getName()
			);
			transServerClient = new TransServer.Client(mp1);
			try {
				socket.open();
			} catch (TTransportException e) {
				e.printStackTrace();
			}
			return transServerClient.getAllRunning();
		} finally {
			if (socket != null) socket.close();
		}
	}
}
