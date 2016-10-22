package org.shoper.common.rpc.common;

/**
 * thrift连接 url 以及对象互转构造器...
 *
 * @author ShawnShoper
 */


public class URLBuilder {

	public enum RPCType {
		Thrift
	}

	public static AbstractURLBuilder Builder (RPCType rpcType) {
		AbstractURLBuilder urlBuilder = null;
		if (RPCType.Thrift == rpcType)
			urlBuilder = new ThriftURLBuilder();
		return urlBuilder;
	}
}
