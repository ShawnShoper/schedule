package org.shoper.common.rpc;

import org.junit.Test;
import org.shoper.common.rpc.common.URLBuilder;
import org.shoper.common.rpc.common.URL;

public class ThriftURLBuilder_Test {
	private static String URL = null;

	@Test
	public void build_Test () {
		URL thriftConnection = new URL();
		thriftConnection.setGroup("test");
		thriftConnection.setVersion("1.1.1");
		thriftConnection.setHost("192.168.100.45");
		thriftConnection.setPort(2222);
		thriftConnection.setClusterName("test");
		String url = URLBuilder.Builder(URLBuilder.RPCType.Thrift).build(thriftConnection);
		URL = url;
		System.out.println(url);
	}

	@Test
	public void deBuild_Test () {
		URL thriftConnection = URLBuilder.Builder(URLBuilder.RPCType.Thrift).deBuild(URL);
		System.out.println(thriftConnection);
	}
}
