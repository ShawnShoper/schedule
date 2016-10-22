package org.shoper.common.rpc.common;

import java.util.concurrent.TimeUnit;

import static org.shoper.schedule.Constant.DEFAULT_GROUP;

/**
 * Connection base info
 *
 * @author ShawnShoper
 */
public class URL {
	private String host;
	private int port;
	private String group = DEFAULT_GROUP;
	private String clusterName;
	private int timeout;
	private TimeUnit unit;
	private String version;
	private String originPath;
	private String token;
	private String className;

	public String getClassName () {
		return className;
	}

	public void setClassName (String className) {
		this.className = className;
	}

	public String getToken () {
		return token;
	}

	public void setToken (String token) {
		this.token = token;
	}

	public String getOriginPath () {
		return originPath;
	}

	public void setOriginPath (String originPath) {
		this.originPath = originPath;
	}

	public String getID () {
		return host + port + (group == null ? "" : group) + (version == null ? "" : version);
	}

	public String getVersion () {
		return version;
	}

	public void setVersion (String version) {
		this.version = version;
	}

	public String getHost () {
		return host;
	}

	public void setHost (String host) {
		this.host = host;
	}

	public int getPort () {
		return port;
	}

	public void setPort (int port) {
		this.port = port;
	}

	public String getGroup () {
		return group;
	}

	public void setGroup (String group) {
		this.group = group;
	}

	public String getClusterName () {
		return clusterName;
	}

	public void setClusterName (String clusterName) {
		this.clusterName = clusterName;
	}

	public int getTimeout () {
		return timeout;
	}

	public void setTimeout (int timeout) {
		this.timeout = timeout;
	}

	public TimeUnit getUnit () {
		return unit;
	}

	public void setUnit (TimeUnit unit) {
		this.unit = unit;
	}

	@Override
	public String toString () {
		return "ThriftConnection [host=" + host + ", port=" + port + ", group="
				+ group + ", clusterName=" + clusterName + ", timeout="
				+ timeout + ", unit=" + unit + ", version=" + version
				+ ", originPath=" + originPath + "]";
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((token == null) ? 0 : token.hashCode());
		return result;
	}

	@Override
	public boolean equals (Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		URL other = (URL) obj;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		return true;
	}

}
