package org.shoper.schedule.server.web;

import javax.servlet.http.HttpServletRequest;

import org.shoper.schedule.conf.ApplicationInfo;
import org.shoper.schedule.conf.ServerConf;
import org.shoper.schedule.server.web.response.ResponseMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sysConf")
public class SysconfigController
{
	@Autowired
	ApplicationInfo appInfo;
	@Autowired
	ServerConf serverConf;
	@RequestMapping(value = "/getWSAddr", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String getWSAddress(HttpServletRequest req)
	{
		ResponseMsg responseMsg = new ResponseMsg();
		responseMsg.setData(appInfo.getBindAddr() + ":" + serverConf.getPort());
		return responseMsg.toJson();
	}
}
