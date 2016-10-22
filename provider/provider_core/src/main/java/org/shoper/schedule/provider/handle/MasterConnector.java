package org.shoper.schedule.provider.handle;


import org.shoper.common.rpc.common.URL;

/**
 * Thrift connector...
 * 
 * @author ShawnShoper
 *
 */
public class MasterConnector
{
	private URL thriftURL;
	private MasterHandler thriftHandler;
	private boolean disabled;
	public void reset()
	{

	}

	private MasterConnector()
	{
	}

	public static MasterConnector buider(URL tc)
	{
		MasterConnector thriftConnector = new MasterConnector();
		thriftConnector.setConnecion(tc);
		MasterHandler thriftHandler = new MasterHandler(tc);
		thriftConnector.setThriftHandler(thriftHandler);
		return thriftConnector;
	}

	public MasterHandler getThriftHandler()
	{
		return thriftHandler;
	}

	public void setThriftHandler(MasterHandler thriftHandler)
	{
		this.thriftHandler = thriftHandler;
	}

	public void setConnecion(URL thriftURL)
	{
		this.thriftURL = thriftURL;
	}
	public URL getThriftURL()
	{
		return thriftURL;
	}
	public synchronized void enabled()
	{
		if (disabled)
			disabled = false;
	}
	public synchronized void disabled()
	{
		if (!disabled)
			disabled = true;
	}

	public boolean isDisabled()
	{
		return disabled;
	}

}
