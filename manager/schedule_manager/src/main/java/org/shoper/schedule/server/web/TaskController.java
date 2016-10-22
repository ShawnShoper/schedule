package org.shoper.schedule.server.web;

import org.shoper.schedule.conf.ApplicationInfo;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.pojo.Task;
import org.shoper.schedule.pojo.TaskTemplate;
import org.shoper.schedule.server.service.TaskService;
import org.shoper.schedule.server.web.response.BootstrapTableResponse;
import org.shoper.schedule.server.web.response.Response;
import org.shoper.schedule.server.web.response.ResponseList;
import org.shoper.schedule.server.web.response.ResponseMsg;
import org.shoper.schedule.server.web.vo.TaskTemplateVO;
import org.shoper.schedule.server.web.vo.TaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;


@RestController
@RequestMapping("/task")
public class TaskController
{
	@Autowired
	TaskService taskService;
	@Autowired
	ApplicationInfo applicationInfo;
	/**
	 * 分页获取 task 数据
	 *
	 * @return
	 */
	@RequestMapping(value = "/getTask", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public BootstrapTableResponse<TaskVO> getAllTask(String limit,
													 String offset, String search, String sort, String order,
													 HttpServletResponse resp)
	{
		BootstrapTableResponse<TaskVO> response = new BootstrapTableResponse<TaskVO>();
		try
		{
			List<Task> tasks = taskService.getTask(offset, limit, search, sort,
					order);
			response.setRows(TaskVO.toVO(tasks));
			response.setRow(Integer.valueOf(limit));
			response.setTotal(taskService.getTaskSize(search));
		} catch (SystemException e)
		{
			response.setCode(1);
			resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			response.setMessage(e.getLocalizedMessage());
		}
		return response;
	}
	@RequestMapping(value = "/killRunningTask/{taskId}/{host}-{port}", method = {
			RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Response killRunningTask(@PathVariable("taskId") String id, @PathVariable("host") String host,@PathVariable("port")  int port, HttpServletResponse resp){
		Response response = new Response();
		try
		{
			taskService.killRunningTask(id,host,port);
		} catch (Exception e)
		{
			response.setCode(1);
			resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			response.setMessage(e.getLocalizedMessage());
		}
		return response;
	}
	@RequestMapping(value = "/showAllTask/{host}-{port}", method = {
			RequestMethod.POST}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseList showAllTask( @PathVariable("host") String host,@PathVariable("port")  int port, HttpServletResponse resp){
		ResponseList response = new ResponseList();
		try
		{
			response.setData(taskService.showAllTask(host,port));
		} catch (Exception e)
		{
			response.setCode(1);
			resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			response.setMessage(e.getLocalizedMessage());
		}
		return response;
	}

	/**
	 * 设置状态..
	 *
	 * @param id
	 *            id
	 * @param type
	 *            0 task template,1 task
	 */
	@RequestMapping(value = "/inverseStatus", method = {
			RequestMethod.PUT}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Response inverseStatus(String id, String value, String type,
								  HttpServletResponse resp)
	{
		Response response = new Response();
		try
		{
			taskService.inverseStatus(id, value, type);
		} catch (Exception e)
		{
			response.setCode(1);
			resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			response.setMessage(e.getLocalizedMessage());
		}
		return response;
	}
	/**
	 * 根据指定的 id 获取对应的 task
	 *
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/getTask/{id}", method = {
			RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Response getTaskById(@PathVariable String id)
	{
		ResponseMsg response = new ResponseMsg();
		try
		{
			Task task = taskService.getTaskById(id);
			TaskVO taskVO = TaskVO.toVO(task);
			response.setData(taskVO);
		} catch (SystemException e)
		{
			response.setCode(1);
			response.setMessage(e.getLocalizedMessage());
		}
		return response;
	}

	@RequestMapping(value = "/getTaskTemplate/{id}", method = {
			RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Response getTaskTemplateById(@PathVariable String id)
	{
		ResponseMsg response = new ResponseMsg();
		try
		{
			TaskTemplate task = taskService.getTaskTemplate(id);
			TaskTemplateVO taskVO = TaskTemplateVO.toVO(task);
			response.setData(taskVO);
		} catch (SystemException e)
		{
			response.setCode(1);
			response.setMessage(e.getLocalizedMessage());
		}
		return response;
	}

	@RequestMapping(value = "/editTask/{id}", method = {
			RequestMethod.PUT}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Response editTask(@PathVariable String id, String name, String url,
			String cookies, String params, String cronexp,
			HttpServletResponse resp)
	{
		Response response = new Response();
		try
		{
			taskService.editTask(id, name, url, cookies, params, cronexp);
		} catch (SystemException e)
		{
			resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			response.setCode(1);
			response.setMessage(e.getLocalizedMessage());
		}
		return response;
	}
	@RequestMapping(value = "/editTaskTemplate/{id}", method = {
			RequestMethod.PUT}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Response editTaskTemplate(@PathVariable String id, String name,
			String url, String cookies, String template,
			HttpServletResponse resp)
	{
		Response response = new Response();
		try
		{
			taskService.editTaskTaskTemplate(id, name, url, cookies, template);
		} catch (SystemException e)
		{
			resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			response.setCode(1);
			response.setMessage(e.getLocalizedMessage());
		}
		return response;
	}
	/**
	 * 添加 task template
	 *
	 * @param name
	 * @param url
	 * @param cookies
	 * @param template
	 * @return
	 */
	@RequestMapping(value = "/addTaskTemplate", method = RequestMethod.POST)
	public Response addTaskTemplate(String name, String url, String cookies,
			String template, HttpServletResponse resp)
	{
		Response response = new Response();
		try
		{
			taskService.addTaskTemplate(name, url, cookies, template);
		} catch (SystemException e)
		{
			resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			response.setCode(1);
			response.setMessage(e.getLocalizedMessage());
		}
		return response;
	}
	/**
	 * 添加 task
	 *
	 * @param name
	 * @param url
	 * @param templateID
	 * @param cookies
	 * @param params
	 * @param cronexp
	 * @return
	 */
	@RequestMapping(value = "/addTask", method = RequestMethod.POST)
	public Response addTask(String name, String url, String templateID,
			String cookies, String params, String cronexp,
			HttpServletResponse resp)
	{
		Response response = new Response();
		try
		{
			taskService.addTask(name, url, templateID, cookies, params,
					cronexp);
		} catch (Exception e)
		{
			resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			response.setCode(1);
			response.setMessage(e.getLocalizedMessage());
		}
		return response;
	}
	/**
	 * 获取 task template
	 *
	 * @param limit
	 * @param offset
	 * @param search
	 * @param sort
	 * @param order
	 * @return
	 */
	@RequestMapping(value = "/getTaskTemp", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public BootstrapTableResponse<TaskTemplateVO> getAllTaskTemplate(
			String limit, String offset, String search, String sort,
			String order)
	{
		BootstrapTableResponse<TaskTemplateVO> response = new BootstrapTableResponse<TaskTemplateVO>();
		try
		{
			List<TaskTemplate> taskTemplate = taskService
					.getTaskTemplate(offset, limit, search, sort, order);
			response.setRows(TaskTemplateVO.toVOs(taskTemplate));
			response.setRow(Integer.valueOf(limit));
			response.setTotal(taskService.getTaskTemplateSize(search));
		} catch (SystemException e)
		{
			response.setCode(1);
			response.setMessage(e.getLocalizedMessage());
		}
		return response;
	}
	/**
	 * 新任务通知与修改.
	 *
	 * @param type
	 *            0启用,1停用
	 * @param domain
	 *            新任务域
	 * @return 响应
	 */
	@RequestMapping(value = "/notifyTask", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Response notifyTask(String type, String domain)
	{
		Response response = new Response();
		try
		{
			taskService.notifyTask(type, domain);
		} catch (SystemException e)
		{
			// 这里不使用resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			// 为了兼容提供出去的 api
			response.setCode(1);
			response.setMessage(e.getLocalizedMessage());
		}
		return response;
	}
	/**
	 * 删除指定 ID 的任务 ID
	 *
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/deleteTask/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Response deleteTask(@PathVariable String id,
			HttpServletResponse response)
	{
		Response resp = new Response();
		try
		{
			taskService.deleteTask(id);
		} catch (Exception e)
		{
			response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			resp.setCode(1);
			resp.setMessage(e.getLocalizedMessage());
		}
		return resp;
	}
	/**
	 * 删除指定 ID 的任务 ID
	 *
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/deleteTaskTemp/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Response deleteTaskTemp(@PathVariable String id,
			HttpServletResponse response)
	{
		Response resp = new Response();
		try
		{
			taskService.deleteTaskTemp(id);
		} catch (Exception e)
		{
			response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			resp.setCode(1);
			resp.setMessage(e.getLocalizedMessage());

		}
		return resp;
	}
}
