package org.shoper.taskScript;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.junit.Before;
import org.junit.Test;
import org.shoper.commons.StringUtil;
import org.shoper.http.apache.proxy.ProxyServerPool;
import org.shoper.schedule.face.TransServer;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.pojo.Task;
import org.shoper.schedule.pojo.TaskMessage;
import org.shoper.schedule.pojo.TaskTemplate;
import org.shoper.schedule.provider.job.JobCaller;
import org.shoper.taskScript.script.Ctrip_Hotel_crawlData;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;

/**
 * Created by ShawnShoper on 16/8/26.
 */
public class Ctrip_hotelTests {
	TransServer.Client transServerClient;
	TSocket socket = null;

	@Before
	public void init () {
		socket = new TSocket("192.168.2.238", 8888);
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
	}
	@Before
	public void initProxyPool () throws FileNotFoundException {
		ProxyServerPool.importProxyServer(new File("proxyip.ls"), Charset.forName("utf-8"));
	}
	@Test
	public void Test () throws Exception {
		TaskMessage taskMessage = new TaskMessage();
		Task task = new Task();
		JobParam jobParam = new JobParam();
		//jobParam.setType("0515");
		//jobParam.setCategory("yangcheng");
		jobParam.setJobCode("21556");
		jobParam.setJobName("扎囊");
		jobParam.setCategory("zhanang");
		jobParam.setType("0893");
		task.setParams(JSONObject.toJSONString(jobParam));
		task.setId("56341684641646");
		taskMessage.setTask(task);
		TaskTemplate taskTemplate = new TaskTemplate();
		taskTemplate.setCode(FileUtils
									 .readFileToString(new File(
											 "src/main/java/org/shoper/taskScript/script/Ctrip_Hotel_crawlData.java"))
									 .getBytes());
		taskMessage.setTaskTemplate(taskTemplate);
//		System.err.println(transServerClient.sendTask(taskMessage.toJson()));
		JobCaller jobCaller = new Ctrip_Hotel_crawlData();
		jobCaller.init(jobParam);
		jobCaller.run();
		jobCaller.destroy();
	}

	public static void main (String[] args) {
		System.out.println(StringUtil.unicodeToStr("\u6210\u90fd\u56db\u53f7\u5de5\u5382\u9752\u5e74\u65c5\u9986"));
	}
}
