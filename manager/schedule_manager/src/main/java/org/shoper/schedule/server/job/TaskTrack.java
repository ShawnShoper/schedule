package org.shoper.schedule.server.job;

import org.shoper.commons.StringUtil;
import org.shoper.schedule.pojo.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TaskTrack
{
	/**
	 * Storing provider and task
	 */
	private static volatile ConcurrentHashMap<String, List<Task>> holdJobs = new ConcurrentHashMap<>();

	public static List<Task> getTaskHoldJobs(String token)
	{
		if (null != token)
			throw new NullPointerException();
		return holdJobs.get(token);
	}
	public static void taskDone(String token, String taskID)
	{
		if (holdJobs.containsKey(token))
		{
			List<Task> task = holdJobs.get(token);
			task.stream().parallel().findAny()
					.filter(e -> taskID.equals(e.getId()));
		}
	}
	public static Task takeFaildTaskAndRemove(String token, String taskID)
	{
		Task task = null;
		if (holdJobs.containsKey(token))
			task = holdJobs.get(token).stream().findAny()
					.filter(e -> taskID.equals(e.getId())).get();
		holdJobs.get(token).remove(task);
		return task;
	}

	public static void pushTask(String token, Task task)
	{
		if (!holdJobs.containsKey(token))
			holdJobs.put(token, new ArrayList<Task>());
		List<Task> tasks = holdJobs.get(token);
		tasks.add(task);
	}
	public static void removeTask(String token, String job)
	{
		if (StringUtil.isEmpty(token) || StringUtil.isEmpty(job))
			return;
		System.out.println(holdJobs.get(token));
		System.out.println(job);
		if (holdJobs.containsKey(token))
			holdJobs.get(token).removeIf(e -> job.equals(e.getId()));
	}
}
