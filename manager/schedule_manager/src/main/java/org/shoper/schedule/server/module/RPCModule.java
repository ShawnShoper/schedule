package org.shoper.schedule.server.module;

import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.shoper.concurrent.future.AsynCallable;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.schedule.face.ReportServer;
import org.shoper.schedule.module.StartableModule;
import org.shoper.schedule.server.face.ReportServerHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;

public class RPCModule extends StartableModule
{
	@Component
	class ThriftStarter extends AsynCallable<Boolean, Object>
	{
		private final String name = "thrift-starter";
		@Autowired
		ReportServerHandler reportServerHandler;
		private int port;
		private String host;
		TServer server;
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
		public Boolean run(Object object) throws Exception
		{
			try
			{
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
			InetSocketAddress inetSocketAddress = new InetSocketAddress(host,
					port);
			TServerTransport t = new TServerSocket(inetSocketAddress);
			server = new TThreadPoolServer(
					new TThreadPoolServer.Args(t).processor(processor));
			processor.registerProcessor(ReportServer.class.getName(),
					new ReportServer.Processor<ReportServer.Iface>(
							reportServerHandler));
			server.serve();
		}
	}

	@Override
	public int start()
	{
		return 0;
	}

	@Override
	public void stop()
	{

	}
}
