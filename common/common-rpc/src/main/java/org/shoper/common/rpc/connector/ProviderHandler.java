package org.shoper.common.rpc.connector;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.shoper.common.rpc.common.URL;
import org.shoper.schedule.exception.RPCConnectionException;
import org.shoper.schedule.face.TransServer;
import org.shoper.schedule.pojo.TaskMessage;
import org.shoper.schedule.resp.AcceptResponse;
import org.shoper.schedule.resp.StatusResponse;

import java.util.List;

public class ProviderHandler {
    private volatile TransServer.Client transServerClient;
    private volatile URL thriftConnection;
    TSocket socket = null;

    public ProviderHandler(URL thriftConnection) {
        this.thriftConnection = thriftConnection;
    }

    public TransServer.Client getTransServerClient() {
        return transServerClient;
    }

    public void setTransServerClient(TransServer.Client transServerClient) {
        this.transServerClient = transServerClient;
    }

    /**
     * Connecting remote server...<br>
     * if connect fail , will try 2 times to reconnect
     *
     * @return server instance
     * @throws RPCConnectionException
     */
    public TransServer.Client connect() throws RPCConnectionException {
        boolean flag = false;

        for (int i = 0; i < 3; i++) {
            try {
                socket = new TSocket(thriftConnection.getHost(),
                        thriftConnection.getPort());
                TBinaryProtocol protocol = new TBinaryProtocol(socket);
                TMultiplexedProtocol mp1 = new TMultiplexedProtocol(protocol,
                        thriftConnection.getClusterName());
                transServerClient = new TransServer.Client(mp1);
                socket.open();
                flag = true;
                break;
            } catch (TTransportException e) {
                // Retry
            }
        }
        if (!flag)
            throw new RPCConnectionException("连接" + thriftConnection.getHost()
                    + ":" + thriftConnection.getPort() + "失败.");
        return transServerClient;
    }

    public void close() {
        socket.close();
    }

    public AcceptResponse sendTask(TaskMessage taskMessage)
            throws RPCConnectionException {
        AcceptResponse acceptResponse = null;
        try {
            acceptResponse = AcceptResponse
                    .parseObject(connect().sendTask(taskMessage.toJson()));
        } catch (TException e) {
            throw new RPCConnectionException(e);
        } finally {
            close();
        }
        return acceptResponse;
    }

    public boolean isAvailable()
            throws RPCConnectionException {
        try {
            return connect().isAvailable();
        } catch (TException e) {
            throw new RPCConnectionException(e);
        } finally {
            close();
        }
    }

    public StatusResponse getStatus() throws RPCConnectionException {
        StatusResponse result = null;
        try {
            result = StatusResponse.parseObject(connect().getStatus());
        } catch (TException e) {
            throw new RPCConnectionException(e);
        } finally {
            close();
        }
        return result;
    }

    public List<String> getAllRunning() throws RPCConnectionException {
        List<String> result;
        try {

            result = connect().getAllRunning();
        } catch (TException e) {
            throw new RPCConnectionException(e);
        } finally {
            close();
        }
        return result;
    }

}
