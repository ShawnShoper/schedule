package org.shoper.common.rpc.common;

public class ThriftURLBuilder extends AbstractURLBuilder {
	String protocol = "thrift://";

	@Override
	public String getProtocol () {
		return protocol;
	}

	@Override
	protected String buildURI (URL url) {
		return this.protocol + url.getHost() + ":" + url.getPort();
	}
}