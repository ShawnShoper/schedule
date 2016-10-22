package org.shoper.schedule.server.resouces;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value="application")
public class Resources {
	private String application;
	private String zkHost;
	private int zkPort;
	private int zkTimeout;
	private int providerNode;
	public String getApplication() {
		return application;
	}
	public void setApplication(String application) {
		this.application = application;
	}
	public String getZkHost() {
		return zkHost;
	}
	public void setZkHost(String zkHost) {
		this.zkHost = zkHost;
	}
	public int getZkPort() {
		return zkPort;
	}
	public void setZkPort(int zkPort) {
		this.zkPort = zkPort;
	}
	public int getZkTimeout() {
		return zkTimeout;
	}
	public void setZkTimeout(int zkTimeout) {
		this.zkTimeout = zkTimeout;
	}
	public int getProviderNode() {
		return providerNode;
	}
	public void setProviderNode(int providerNode) {
		this.providerNode = providerNode;
	}
	
}
