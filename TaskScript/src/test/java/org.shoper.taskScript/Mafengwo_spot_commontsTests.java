package org.shoper.taskScript;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.junit.Before;
import org.junit.Test;
import org.shoper.schedule.face.TransServer;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.pojo.Task;
import org.shoper.schedule.pojo.TaskMessage;
import org.shoper.schedule.pojo.TaskTemplate;

import java.io.File;

/**
 * Created by ShawnShoper on 16/8/26.
 */
public class Mafengwo_spot_commontsTests {
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
	@Test
	public void Test () throws Exception {
		TaskMessage taskMessage = new TaskMessage();
		Task task = new Task();
		JobParam jobParam = new JobParam();
		jobParam.setJobCode("10099");
		jobParam.setJobName("上海");
		task.setParams(JSONObject.toJSONString(jobParam));
		task.setId("56341684641646");
		taskMessage.setTask(task);
		TaskTemplate taskTemplate = new TaskTemplate();
		taskTemplate.setCode(FileUtils
									 .readFileToString(new File(
											 "src/main/java/org/shoper/taskScript/script/HDFSImport/Mafengwo_comment.java"))
									 .getBytes());
		taskMessage.setTaskTemplate(taskTemplate);
		System.err.println(transServerClient.sendTask(taskMessage.toJson()));
	}
}
