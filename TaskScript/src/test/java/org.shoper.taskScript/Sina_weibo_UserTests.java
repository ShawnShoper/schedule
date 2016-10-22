package org.shoper.taskScript;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.junit.Before;
import org.junit.Test;
import org.shoper.http.apache.proxy.ProxyServerPool;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.face.TransServer;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.pojo.Task;
import org.shoper.schedule.pojo.TaskMessage;
import org.shoper.schedule.pojo.TaskTemplate;
import org.shoper.schedule.provider.job.JobCaller;
import org.shoper.taskScript.script.Sina_Weibo_User;
import org.shoper.taskScript.script.Sina_weibo;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;

/**
 * Created by ShawnShoper on 16/8/8.
 */
public class Sina_weibo_UserTests {
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
		task.setId("1232312");
		JobParam jobParam = new JobParam();
		jobParam.setJobCode("旅游发展委员会");
		jobParam.setCategory("org_vip");
		jobParam.setCookies("SINAGLOBAL=9348856106441.83.1472537556283; wvr=6; UOR=,,baike.baidu.com; YF-Ugrow-G0=8751d9166f7676afdce9885c6d31cd61; SCF=AgNr6xLsZAbjKKboLflOPU-AjLyIppQoNUwvRPOBW76t2AblawmQ26eloHsfDkCJ_1DBw-6iMoXbKF7eN_D5Afw.; SUB=_2A2562zBiDeTxGedG6VYS9i3LzT6IHXVZkSaqrDV8PUNbmtBeLWXCkW-WG_DzhPw5zHOTtWs0fT2PUufaoA..; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WhdPz3lCecbA_hp6RKPxGfu5JpX5KMhUgL.Fo2ReoB0SoeNSoz2dJLoIEqLxK-LBK2L1--LxKML12qLB-B_TCH8SCHFxbHWSEH8Sb-RSF-ReBtt; SUHB=0M2UKbAZ6NI5dO; ALF=1505784753; SSOLoginState=1474248754; YF-V5-G0=d8809959b4934ec568534d2b6c204def; YF-Page-G0=fc0a6021b784ae1aaff2d0aa4c9d1f17; _s_tentry=weibo.com; Apache=8647307627765.6875.1474248802993; ULV=1474248803000:7:6:1:8647307627765.6875.1474248802993:1473846303633");
		task.setParams(JSONObject.toJSONString(jobParam));
		taskMessage.setTask(task);
		TaskTemplate taskTemplate = new TaskTemplate();
		taskTemplate.setCode(FileUtils
									 .readFileToString(new File(
											 "src/main/java/org/shoper/taskScript/script/Sina_Weibo_User.java"))
									 .getBytes());
		taskMessage.setTaskTemplate(taskTemplate);
		System.err.println(transServerClient.sendTask(taskMessage.toJson()));
	}

	public static void main (String[] args) throws InterruptedException, SystemException, FileNotFoundException {
		ProxyServerPool.importProxyServer(new File("proxyip.ls"), Charset.forName("utf-8"));
		JobCaller jobCaller = new Sina_Weibo_User();
		JobParam jobParam = new JobParam();
		jobParam.setJobCode("旅游局");
		jobParam.setCategory("org_vip");
		jobParam.setCookies("TC-V5-G0=f88ad6a0154aa03e3d2a393c93b76575; YF-Page-G0=23b9d9eac864b0d725a27007679967df; _s_tentry=-; Apache=4466064745352.929.1475033718231; SINAGLOBAL=4466064745352.929.1475033718231; ULV=1475033718237:1:1:1:4466064745352.929.1475033718231:; YF-V5-G0=55fccf7be1706b6814a78384fa94e30c; YF-Ugrow-G0=9642b0b34b4c0d569ed7a372f8823a8e; SSOLoginState=1475035244; wvr=6; SCF=Ah9Z2M30c4B9_6Thfkz--Vh0Lr5sJgOXdV44r8enK_f2yMJxL_yK6EOtqboUTDNhJKpXI9WLaguHnMvbc42io20.; SUB=_2A2566KoTDeTxGedG6VYS9i3LzT6IHXVZn5zbrDV8PUNbmtBeLUnXkW99mHXyB9IDfoNrMnuepr8r3GFbxw..; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WhdPz3lCecbA_hp6RKPxGfu5JpX5KMhUgL.Fo2ReoB0SoeNSoz2dJLoIEqLxK-LBK2L1--LxKML12qLB-B_TCH8SCHFxbHWSEH8Sb-RSF-ReBtt; SUHB=0HTwr4KwiUPXAq; ALF=1506676163; UOR=,,login.sina.com.cn");
		jobCaller.init(jobParam);
		jobCaller.run();
		jobCaller.destroy();
	}
}
