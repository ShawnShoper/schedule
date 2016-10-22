
package org.shoper.taskScript;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.junit.Before;
import org.junit.Test;
import org.shoper.http.apache.AccessBean;
import org.shoper.schedule.face.TransServer;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.pojo.Task;
import org.shoper.schedule.pojo.TaskMessage;
import org.shoper.schedule.pojo.TaskTemplate;
import org.shoper.schedule.provider.job.JobCaller;

import java.io.File;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

/**
 * Created by xp7.82414 on 2016/10/10.
 */
public class BaiDu_PachuTest {
    TransServer.Client transServerClient;
    TSocket socket = null;

    @Before
    public void init () {
        socket = new TSocket("192.168.2.150", 8888);
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
    public void Test1() throws MalformedURLException, InterruptedException{
        AccessBean accessBean = new AccessBean("http://www.dianping.com/search/category/252/30/g20038");
        accessBean.setCharset("utf-8");
        accessBean.setRetry(3);
        accessBean.setTimeoutUnit(TimeUnit.SECONDS);
        accessBean.setTimeout(20);

    }
    @Test
    public void Test () throws Exception {
        TaskMessage taskMessage = new TaskMessage();
        Task task = new Task();
        task.setId("1232312");
        JobParam jobParam = new JobParam();
        jobParam.setType("广东省");
        jobParam.setJobName("广州");
        jobParam.setJobCode("257");
        task.setParams(JSONObject.toJSONString(jobParam));
        taskMessage.setTask(task);
        TaskTemplate taskTemplate = new TaskTemplate();
        taskTemplate.setCode(FileUtils
                .readFileToString(new File(
                        "src/main/java/org/shoper/taskScript/script/BaiduMaplocaldata.java"))
                .getBytes());
        taskMessage.setTaskTemplate(taskTemplate);
        System.err.println(transServerClient.sendTask(taskMessage.toJson()));
    }
}
