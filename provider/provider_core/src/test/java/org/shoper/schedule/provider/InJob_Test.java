package org.shoper.schedule.provider;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TSocket;
import org.junit.Test;
import org.shoper.schedule.face.TransServer;
import org.shoper.schedule.pojo.Task;
import org.shoper.schedule.pojo.TaskMessage;
import org.shoper.schedule.pojo.TaskTemplate;


public class InJob_Test
{
	@Test
	public void jobIn_Test() throws Exception
	{
		// TransServer.Client transServerClient;
		// TSocket socket = null;
		// socket = new TSocket("192.168.0.60", 8888);
		// TBinaryProtocol protocol = new TBinaryProtocol(socket);
		// TMultiplexedProtocol mp1 = new TMultiplexedProtocol(protocol,
		// TransServer.class.getName());
		// transServerClient = new TransServer.Client(mp1);
		// socket.open();
		// TaskMessage taskMessage = new TaskMessage();
		// Task task = new Task();
		// JobParam jobParam = new JobParam();
		// jobParam.setCategory("Weibo");
		// jobParam.setCategory_name("微博");
		// jobParam.setCookies(
		// "SINAGLOBAL=2896592207252.9795.1453882253518;
		// un=xiehao3692@vip.qq.com;
		// YF-Page-G0=59104684d5296c124160a1b451efa4ac;
		// SUS=SID-1824363762-1454553450-GZ-8fqy1-15d3c0fea61aa42a5843ca11f30bbe6a;
		// SUE=es%3D5ce208ab50049470adfbb23fee7efb7e%26ev%3Dv1%26es2%3D9065af710774c651e87c647a790510bf%26rs0%3DkBiUPQyL4LnhZglOAeEvKhR1h9vhZUFTnA4wMwx70BXo9m4njVqoysfbNNTFT5%252FjtJU%252FAlwqm3mHO6tMKNX2USn8zFo8AWVYjFlTbRLAdKOLqA%252Br4QqyQppkFEEZ%252FdZFE8zMt73UWNrrVsW8lecZTymhtF2k9dgVWMuPeToadhE%253D%26rv%3D0;
		// SUP=cv%3D1%26bt%3D1454553450%26et%3D1454639850%26d%3Dc909%26i%3Dbe6a%26us%3D1%26vf%3D0%26vt%3D0%26ac%3D0%26st%3D0%26uid%3D1824363762%26name%3Dxiehao3692%2540vip.qq.com%26nick%3D%25E7%2594%25A8%25E6%2588%25B71824363762%26fmp%3D%26lcp%3D2014-04-26%252016%253A34%253A54;
		// SUB=_2A257tsk6DeTxGedG6VYS9i3LzT6IHXVYxb3yrDV8PUNbvtBeLWjbkW9LHesTocdSEEr7vHWYudm0SSv4dNMlrA..;
		// SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WhdPz3lCecbA_hp6RKPxGfu5JpX5KMt;
		// SUHB=0bttwxO9WKUjWr; ALF=1486089450; SSOLoginState=1454553450;
		// _s_tentry=login.sina.com.cn; Apache=8041177275590.599.1454553456984;
		// ULV=1454553456997:3:2:2:8041177275590.599.1454553456984:1454377693782;
		// UOR=os.51cto.com,widget.weibo.com,login.sina.com.cn");
		// jobParam.setJobCode("1001061989772524");
		// jobParam.setType("p");
		// jobParam.setTargetURL("http://weibo.com/u/1989772524");
		// jobParam.setJobName("桂林市旅游发展委员会");
		// jobParam.setQueryURL("http://weibo.com/p/1001061989772524");
		// task.setParams(JSONObject.toJSONString(jobParam));
		// task.setId("1232312");
		// taskMessage.setTask(task);
		// TaskTemplate taskTemplate = new TaskTemplate();
		// taskTemplate.setCode(FileUtils
		// .readFileToString(new File(
		// "/Users/ShawnShoper/Documents/workspace/daqsoft_schedule_provider/src/main/java/com/daqsoft/schedule/provider/script/Weibo_Sina.java"))
		// .getBytes());
		// taskMessage.setTaskTemplate(taskTemplate);
		// System.err.println(transServerClient.sendTask(taskMessage.toJson()));
		// socket.close();

	}
	public static void main(String[] args) throws Exception
	{
		TransServer.Client transServerClient;
		TSocket socket = null;
		socket = new TSocket("192.168.0.8", 8888);
		TBinaryProtocol protocol = new TBinaryProtocol(socket);
		TMultiplexedProtocol mp1 = new TMultiplexedProtocol(protocol,
				TransServer.class.getName());
		transServerClient = new TransServer.Client(mp1);
		socket.open();
		TaskMessage taskMessage = new TaskMessage();
		taskMessage.setTask(new Task());
		TaskTemplate taskTemplate = new TaskTemplate();
		taskTemplate.setCode(FileUtils
				.readFileToString(new File(
						"/Users/ShawnShoper/Documents/workspace/daqsoft_schedule_provider/src/test/java/com/daqsoft/schedule/provider/TaskTest.java"))
				.getBytes());
		taskMessage.setTaskTemplate(taskTemplate);
		String tm = taskMessage.toJson();
		long date1 = System.currentTimeMillis();
		System.out.println(transServerClient.sendTask(tm));
		System.out.println(System.currentTimeMillis() - date1);
		date1 = System.currentTimeMillis();
		System.out.println(transServerClient.getStatus());
		System.out.println(System.currentTimeMillis() - date1);
		date1 = System.currentTimeMillis();
		socket.close();
		System.out.println(System.currentTimeMillis() - date1);
	}
}
