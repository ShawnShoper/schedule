package org.shoper.schedule.provider.module;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.shoper.schedule.conf.HDFSInfo;
import org.shoper.schedule.conf.ZKInfo;
import org.shoper.schedule.manager.ZKModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/**
 * "Hadoop Distributed File System" module<br>
 * #TODO..目前尚未添加 zk 信息修改事件
 * 
 * @author ShawnShoper
 *
 */
@Component
public class HDFSModule extends ZKModule
{
	@Autowired
	private ZKInfo zkInfo;
	private Logger logger = LoggerFactory.getLogger(HDFSModule.class);
	@Autowired
	private HDFSInfo hdfsInfo;
	// Hadoop Distributed File System configuration
	Configuration conf;
	@PostConstruct
	public void init()
	{
		super.setZkInfo(zkInfo);
	}
	@PreDestroy
	public void destory()
	{
		stop();
	}
	@Override
	public void stop()
	{
		super.stop();
	}
	@Override
	public int start()
	{
		try
		{
			if (super.start() == 0x01)
				return 1;
			initHDFS();
		} catch (Exception e)
		{
			return 1;
		}
		setStarted(true);
		return 0;
	}
	private void initHDFS() throws KeeperException
	{
		try
		{
			HDFSInfo hdfsInfo = readData();
			hdfsInfo.setNodePath(this.hdfsInfo.getNodePath());
			this.hdfsInfo = hdfsInfo;
			conf = new Configuration();
			conf.set(hdfsInfo.getHostKey(), hdfsInfo.getHostValue());
		} catch (InterruptedException e)
		{
			;
		}
	}
	@Override
	protected void dataChangeProcess(WatchedEvent event)
	{
		try
		{
			initHDFS();
		} catch (KeeperException e)
		{
			e.printStackTrace();
		}
		logger.info("HDFS数据更改...{}", hdfsInfo);
	}
	/**
	 * Get File system operator
	 * 
	 * @return
	 * @throws IOException
	 */
	public FileSystem getFileSystem()
	{
		try
		{
			return FileSystem.get(conf);
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * readData
	 * 
	 * @return
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	private HDFSInfo readData() throws KeeperException, InterruptedException
	{
		byte[] data = super.getZkClient().showData(hdfsInfo.getNodePath());
		String info = new String(data);
		return HDFSInfo.parseObject(info);
	}

}
