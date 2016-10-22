package org.shoper.schedule.provider.module;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;

import javax.annotation.PreDestroy;

import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.shoper.concurrent.future.AsynCallable;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.schedule.face.TransServer;
import org.shoper.schedule.provider.face.in.TransServerIHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
class ThriftStarter extends AsynCallable<Boolean, Object>
{
	private String name = "thrift-starter";
	@Autowired
	TransServerIHandler transServerIHandler;
	private int port;
	private String host;
	TServer server;
	boolean started = false;
	public Future<Boolean> result;
	public Future<Boolean> getResult()
	{
		return result;
	}
	@PreDestroy
	public void shutdown()
	{
		if (server != null)
			server.stop();
		FutureManager.futureDone(null, name);
	}
	/**
	 * 启动 thrift
	 * 
	 * @param port
	 * @return
	 */
	public void start(String host, int port)
	{
		this.port = port;
		this.host = host;
		result = (FutureManager.pushFuture(null, name, this));
	}
	@Override
	public Boolean run(Object params) throws Exception
	{
		try
		{
			if (!started)
				startServer();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return true;
	}
	public boolean isStarted()
	{
		if (server == null)
			return false;
		return server.isServing();
	}
	private void startServer() throws TTransportException
	{
		TMultiplexedProcessor processor = new TMultiplexedProcessor();
		InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
		TServerTransport t = new TServerSocket(inetSocketAddress);
		server = new TThreadPoolServer(
				new TThreadPoolServer.Args(t).processor(processor));
		processor.registerProcessor(TransServer.class.getName(),
				new TransServer.Processor<TransServer.Iface>(
						transServerIHandler));
		started = true;
		server.serve();
	}
}
