package org.shoper.schedule.provider.handle;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.shoper.common.rpc.common.URL;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.face.ReportServer;

public class MasterHandler
{
	private volatile ReportServer.Client reportClient;
	private volatile URL thriftURL;
	TSocket socket = null;

	public MasterHandler(URL thriftURL)
	{
		this.thriftURL = thriftURL;
	}

	public ReportServer.Client getTransServerClient()
	{
		return reportClient;
	}

	public void setTransServerClient(ReportServer.Client transServerClient)
	{
		this.reportClient = transServerClient;
	}
	/**
	 * Connecting remote server...<br>
	 * if connect fail , will try 2 times to reconnect
	 * 
	 * @return server instance
	 * @throws SystemException
	 */
	public ReportServer.Client connect() throws SystemException
	{
		boolean flag = false;
		for (int i = 0; i < 3; i++)
		{
			try
			{
				socket = new TSocket(thriftURL.getHost(),
						thriftURL.getPort());
				TBinaryProtocol protocol = new TBinaryProtocol(socket);
				TMultiplexedProtocol mp1 = new TMultiplexedProtocol(protocol,
						thriftURL.getClusterName());
				reportClient = new ReportServer.Client(mp1);
				socket.open();
				flag = true;
				break;
			} catch (TTransportException e)
			{
				e.printStackTrace();
			}
		}
		if (!flag)
			throw new SystemException("连接" + thriftURL.getHost() + ":"
					+ thriftURL.getPort() + "失败.");
		return reportClient;
	}
	/**
	 * 关闭连接...
	 */
	public void close()
	{
		socket.close();
	}

	public int report(String report) throws SystemException
	{
		try
		{
			return connect().reportJobDone(report);
		} catch (TException e)
		{
			// 发送数据或者连接异常,这部分需要捕获处理，如果是连接 master 则只需要重试。

			// 如果是链接的是 slave,那么需要做负载以及重试
			e.printStackTrace();
		} finally
		{
			close();
		}
		return 1;
	}

	public void check() throws SystemException
	{
		try
		{
			connect();
		} finally
		{
			close();
		}
	}
}
