package org.shoper.schedule.conf;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "application")
public class ApplicationInfo {
	private int port;
	private int heartBeat;
	private String name;
	private int timeout;
	private String mongoPath;
	private int taskGenerateInterval;
	private TimeUnit generateIntervalUnit;
	private String masterPath;
	private String redisPath;
	private String bindAddr;

	public String getBindAddr () {
		return bindAddr;
	}

	public void setBindAddr (String bindAddr) {
		this.bindAddr = bindAddr;
	}

	public String getRedisPath () {
		return redisPath;
	}

	public void setRedisPath (String redisPath) {
		this.redisPath = redisPath;
	}

	public String getMasterPath () {
		return masterPath;
	}

	public void setMasterPath (String masterPath) {
		this.masterPath = masterPath;
	}

	public int getPort () {
		return port;
	}

	public void setPort (int port) {
		this.port = port;
	}

	public int getTaskGenerateInterval () {
		return taskGenerateInterval;
	}

	public void setTaskGenerateInterval (int taskGenerateInterval) {
		this.taskGenerateInterval = taskGenerateInterval;
	}

	public TimeUnit getGenerateIntervalUnit () {
		return generateIntervalUnit;
	}

	public void setGenerateIntervalUnit (TimeUnit generateIntervalUnit) {
		this.generateIntervalUnit = generateIntervalUnit;
	}

	public String getMongoPath () {
		return mongoPath;
	}

	public void setMongoPath (String mongoPath) {
		this.mongoPath = mongoPath;
	}

	public int getTimeout () {
		return timeout;
	}

	public void setTimeout (int timeout) {
		this.timeout = timeout;
	}

	public int getHeartBeat () {
		return heartBeat;
	}

	public void setHeartBeat (int heartBeat) {
		this.heartBeat = heartBeat;
	}

	public String getName () {
		return name;
	}

	public void setName (String name) {
		this.name = name;
	}

}
