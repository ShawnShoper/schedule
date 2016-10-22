package org.shoper.schedule.provider.face.in;

import org.apache.thrift.TException;
import org.shoper.schedule.face.TransServer;
import org.shoper.schedule.pojo.TaskMessage;
import org.shoper.schedule.provider.handle.TransServerHandleIn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransServerIHandler implements TransServer.Iface {
    @Autowired
    TransServerHandleIn dispatch;

    @Override
    public String sendTask(String taskMessage) throws TException {
        TaskMessage tm = TaskMessage.parseObject(taskMessage);
        String response = dispatch.receive(tm).toJson();
        return response;
    }

    @Override
    public String getStatus() throws TException {
        String response = dispatch.getStatus().toJson();
        return response;
    }

    @Override
    public List<String> getAllRunning() throws TException {
        return dispatch.getAllRunning();
    }

    @Override
    public int kill(String id) throws TException {
        return dispatch.kill(id);
    }

    @Override
    public boolean isAvailable() throws TException {
        return false;
    }

}
