package org.shoper.schedule.provider;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.shoper.mail.MailInfo;
import org.shoper.schedule.conf.HDFSInfo;
import org.shoper.schedule.conf.MongoInfo;
import org.shoper.schedule.conf.RedisInfo;
import org.shoper.zookeeper.ZKClient;
import org.shoper.zookeeper.ZKWatcher;


public class ZKClient_test
{
	public static void main(String[] args)
			throws KeeperException, InterruptedException
	{
		ZKClient zkClient = new ZKClient("192.168.2.4", 2181, 50000,
				new MyWatch());
		zkClient.createNode("/org", CreateMode.PERSISTENT);
		zkClient.createNode("/org/config", CreateMode.PERSISTENT);
		zkClient.createNode("/org/config/provider", CreateMode.PERSISTENT);
		zkClient.createNode("/org/master", CreateMode.PERSISTENT);
		zkClient.createNode("/org/provider", CreateMode.PERSISTENT);
		zkClient.createNode("/org/config/master", CreateMode.PERSISTENT);
		zkClient.createNode("/org/config/redis", CreateMode.PERSISTENT);
		zkClient.createNode("/org/config/mongo", CreateMode.PERSISTENT);
		zkClient.createNode("/org/config/hdfs", CreateMode.PERSISTENT);
		zkClient.createNode("/org/config/mail", CreateMode.PERSISTENT);
		{
			RedisInfo redisInfo = new RedisInfo();
			redisInfo.setHost("192.168.2.4");
			redisInfo.setPassword("shawnshoper");
			redisInfo.setPort(6379);
			redisInfo.setTimeout(10000);
			zkClient.editData("/org/config/redis", redisInfo.toJson());
		}
		{
			MongoInfo mongoInfo = new MongoInfo();
			mongoInfo.setDbName("org");
			mongoInfo.setServerAddress("192.168.2.4:27017");
			mongoInfo.setTimeout(20000);
			zkClient.editData("/org/config/mongo", mongoInfo.toJson());
		}
		{
			HDFSInfo hdfsInfo = new HDFSInfo();
			hdfsInfo.setHostKey("fs.defaultFS");
			hdfsInfo.setHostValue("hdfs://192.168.100.178:8020");
			zkClient.editData("/org/config/hdfs", hdfsInfo.toJson());
		}
		{
			MailInfo mailInfo = new MailInfo();
			mailInfo.setAccount("xieh@orgsoft.com");
			mailInfo.setPassword("Xiehao1993");
			mailInfo.setAuth(true);
			mailInfo.setSmtp("smtp.exmail.qq.com");
			mailInfo.setTo("xiehao3692@vip.qq.com");
			zkClient.editData("/org/config/mail", mailInfo.toJson());
		}
		zkClient.close();
	}
	static class MyWatch extends ZKWatcher
	{

		@Override
		public void childrenNodeChangeProcess(WatchedEvent event)
		{
			System.out.println("childrenNodeChangeProcess");
		}

		@Override
		public void dataChangeProcess(WatchedEvent event)
		{
			System.out.println("dataChangeProcess");
		}

		@Override
		public void nodeDeleteProcess(WatchedEvent event)
		{
			System.out.println("nodeDeleteProcess");
		}

		@Override
		public void nodeCreateProcess(WatchedEvent event)
		{
			String path = event.getPath();
			path = path.substring(path.lastIndexOf("/") + 1);
			System.out.println(path);
			System.out.println("nodeCreateProcess");
		}
	}
}
