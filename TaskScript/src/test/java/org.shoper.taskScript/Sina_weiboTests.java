package org.shoper.taskScript;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.junit.Before;
import org.junit.Test;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.face.TransServer;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.pojo.Task;
import org.shoper.schedule.pojo.TaskMessage;
import org.shoper.schedule.pojo.TaskTemplate;
import org.shoper.schedule.provider.job.JobCaller;
import org.shoper.taskScript.script.Sina_weibo;
import org.shoper.taskScript.script.Sina_weibo_bak;

import java.io.File;

/**
 * Created by ShawnShoper on 16/8/8.
 */
public class Sina_weiboTests {
    TransServer.Client transServerClient;
    TSocket socket = null;

    @Before
    public void init () {
        socket = new TSocket("192.168.2.2", 8888);
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
        jobParam.setJobCode("1002061852855013");
        jobParam.setJobName("音乐人");
        jobParam.setQueryURL("http://weibo.com/p/1002061852855013");
        jobParam.setTargetURL("http://weibo.com/p/1002061852855013");
        jobParam.setType("p");
        jobParam.setCookies("SINAGLOBAL=9348856106441.83.1472537556283; _s_tentry=-; Apache=2909225647594.7085.1473152861402; ULV=1473152861480:4:3:2:2909225647594.7085.1473152861402:1473149454085; login_sid_t=630ae5517cb46aacf352ebcc0b65204e; UOR=,,os.51cto.com; SCF=AgNr6xLsZAbjKKboLflOPU-AjLyIppQoNUwvRPOBW76tAzh9KtfaSiFZovKSso_6EB9GhW_BfekYo0_WrYgtgKU.; SUB=_2A256y8wQDeTxGedG6VYS9i3LzT6IHXVZoLrYrDV8PUNbmtBeLVXdkW9gBp09M1f5BFOLXWa6Q3mLTLjcFw..; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WhdPz3lCecbA_hp6RKPxGfu5JpX5K2hUgL.Fo2ReoB0SoeNSoz2dJLoIEqLxK-LBK2L1--LxKML12qLB-B_TCH8SCHFxbHWSEH8Sb-RSF-ReBtt; SUHB=04W_Txiuw3v1aJ; ALF=1504767936; SSOLoginState=1473231936; un=xiehao3692@vip.qq.com; wvr=6");
        task.setParams(JSONObject.toJSONString(jobParam));
        taskMessage.setTask(task);

        TaskTemplate taskTemplate = new TaskTemplate();
        taskTemplate.setCode(FileUtils
                                     .readFileToString(new File(
                                             "script/Sina_weibo.java"))
                                     .getBytes());
        taskMessage.setTaskTemplate(taskTemplate);
        System.err.println(transServerClient.sendTask(taskMessage.toJson()));
    }

    public static void main (String[] args) throws InterruptedException, SystemException {
        JobParam jobParam = new JobParam();
        jobParam.setJobCode("1002061852855013");
        jobParam.setJobName("音乐人");
        jobParam.setQueryURL("http://weibo.com/p/1002061852855013");
        jobParam.setTargetURL("http://weibo.com/p/1002061852855013");
        jobParam.setType("p");
        jobParam.setCookies("SINAGLOBAL=9348856106441.83.1472537556283; _s_tentry=-; Apache=2909225647594.7085.1473152861402; ULV=1473152861480:4:3:2:2909225647594.7085.1473152861402:1473149454085; login_sid_t=630ae5517cb46aacf352ebcc0b65204e; UOR=,,os.51cto.com; SCF=AgNr6xLsZAbjKKboLflOPU-AjLyIppQoNUwvRPOBW76tAzh9KtfaSiFZovKSso_6EB9GhW_BfekYo0_WrYgtgKU.; SUB=_2A256y8wQDeTxGedG6VYS9i3LzT6IHXVZoLrYrDV8PUNbmtBeLVXdkW9gBp09M1f5BFOLXWa6Q3mLTLjcFw..; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WhdPz3lCecbA_hp6RKPxGfu5JpX5K2hUgL.Fo2ReoB0SoeNSoz2dJLoIEqLxK-LBK2L1--LxKML12qLB-B_TCH8SCHFxbHWSEH8Sb-RSF-ReBtt; SUHB=04W_Txiuw3v1aJ; ALF=1504767936; SSOLoginState=1473231936; un=xiehao3692@vip.qq.com; wvr=6");
        JobCaller jobCaller = new Sina_weibo();
        jobCaller.init(jobParam);
        jobCaller.run();
        jobCaller.destroy();


    }
}
