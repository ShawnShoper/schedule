package org.shoper.schedule;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.shoper.commons.MD5Util;
import org.shoper.schedule.face.TransServer;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.pojo.Task;
import org.shoper.schedule.pojo.TaskMessage;
import org.shoper.schedule.pojo.TaskTemplate;
import com.sun.tools.javac.Main;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.apache.hadoop.yarn.webapp.hamlet.HamletSpec.InputType.file;
import static org.apache.hadoop.yarn.webapp.hamlet.HamletSpec.InputType.text;

/**
 * Created by ShawnShoper on 16/8/2.
 */
public class TransServerHandleTests {
    TransServer.Client transServerClient;
    TSocket socket = null;

    @Before
    public void init() {
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
    public void sendTaskTest() throws Exception {
        TaskMessage taskMessage = new TaskMessage();
        Task task = new Task();
        JobParam jobParam = new JobParam();
        jobParam.setJobCode("wolong");
        jobParam.setJobName("卧龙");
        task.setParams(JSONObject.toJSONString(jobParam));
        task.setId(UUID.randomUUID().toString());
        taskMessage.setTask(task);
        TaskTemplate taskTemplate = new TaskTemplate();
        taskTemplate.setCode(FileUtils
                                     .readFileToString(new File(
                                             "/Users/ShawnShoper/Documents/IDEAWorkspace/Schedule/TaskScript/src/main/java/ForTest.java"))
                                     .getBytes());
        taskMessage.setTaskTemplate(taskTemplate);
        taskMessage.setTask(task);
        transServerClient.sendTask(taskMessage.toJson());
    }

    @Test
    public void getStatusTest() throws TException {
    }
    @Test
    public void getAllRunningTest() throws TException {
        transServerClient.getAllRunning().forEach(e-> System.out.println(e));
    }

    @Test
    public void killTest() throws TException, IOException {
        transServerClient.getAllRunning().forEach(e -> {
            try {
                transServerClient.kill(e);
            } catch (TException e1) {
                e1.printStackTrace();
            }
        });
    }

    @After
    public void shutdown() {
        if (socket != null) socket.close();
    }

}
