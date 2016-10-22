package org.shoper.common.rpc.connector;


import org.shoper.common.rpc.common.URL;
import org.shoper.schedule.resp.StatusResponse;

import java.io.Serializable;

/**
 * Thrift connector...<br>
 * 支持优先级调整筛选.实现算法参考 {@code CompareTo} 方法
 *
 * @author ShawnShoper
 */
public class Connector implements Comparable<Connector>, Serializable {
    private URL url;
    private ProviderHandler thriftHandler;
    private StatusResponse statusResponse;
    private boolean disabled;
    private int priority;
    private boolean checkHeartbeatByRegistry = true;

    public void reset() {
        disabled = false;
        checkHeartbeatByRegistry = true;
    }

    public boolean isCheckHeartbeatByRegistry() {
        return checkHeartbeatByRegistry;
    }

    public void setCheckHeartbeatByRegistry(boolean checkHeartbeatByRegistry) {
        this.checkHeartbeatByRegistry = checkHeartbeatByRegistry;
    }

    public StatusResponse getStatusResponse() {
        return statusResponse;
    }

    public void setStatusResponse(StatusResponse statusResponse) {
        this.statusResponse = statusResponse;
    }

    public static Connector buider(URL tc) {
        Connector thriftConnector = new Connector();
        thriftConnector.setConnecion(tc);
        ProviderHandler thriftHandler = new ProviderHandler(tc);
        thriftConnector.setThriftHandler(thriftHandler);
        return thriftConnector;
    }

    public ProviderHandler getThriftHandler() {
        return thriftHandler;
    }

    public void setThriftHandler(ProviderHandler thriftHandler) {
        this.thriftHandler = thriftHandler;
    }

    public void setConnecion(URL thriftConnection) {
        this.url = thriftConnection;
    }

    public URL getUrl() {
        return url;
    }

    public void setThriftConnection(URL thriftConnection) {
        this.url = thriftConnection;
    }

    public synchronized void enabled() {
        if (disabled)
            disabled = false;
    }

    public synchronized void disabled() {
        if (!disabled)
            disabled = true;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isEnable() {
        return !disabled;
    }

    /**
     * 用于权重判断... <br>
     * -1那么 this 靠前，1this 靠后
     */
    @Override
    public int compareTo(Connector o) {
//		if (o.isEnable() && !this.isEnable()) return 1;
//		else if ((o.isEnable() && this.isEnable()) || (!o.isEnable() && !this.isEnable())) {
        if (this.getPriority() > o.getPriority())
            return -1;
        else if (this.getPriority() == o.getPriority())
            return 0;
//		} else if (!o.isEnable() && this.isEnable()) return -1;
        // 用于比较...权重,提升优先级
        return 1;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return this.priority;
    }

    @Override
    public String toString() {
        return "Connector{" +
                "url=" + url +
                ", thriftHandler=" + thriftHandler +
                ", statusResponse=" + statusResponse +
                ", disabled=" + disabled +
                ", priority=" + priority +
                ", checkHeartbeatByRegistry=" + checkHeartbeatByRegistry +
                '}';
    }
}
