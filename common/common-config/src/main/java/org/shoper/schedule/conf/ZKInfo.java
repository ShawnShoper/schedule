package org.shoper.schedule.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
@Component
@ConfigurationProperties(prefix="zookeeper",value="zookeeper")
public class ZKInfo {
	private String host;
	private int port;
	private int timeout;
	
	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	} 
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
}
