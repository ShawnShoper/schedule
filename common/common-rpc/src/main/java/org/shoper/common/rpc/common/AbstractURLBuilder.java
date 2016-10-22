package org.shoper.common.rpc.common;

import org.shoper.commons.StringUtil;
import org.springframework.util.Assert;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

/**
 * @author ShawnShoper
 * @date 16/10/14
 * @sice
 */
public abstract class AbstractURLBuilder {
	private String protocol = "http://";

	public String getProtocol () {
		return protocol;
	}

	public static void main (String[] args) {
		ThriftURLBuilder thriftURLBuilder = new ThriftURLBuilder();
		URL url = new URL();
		url.setHost("192.168.2.238");
		url.setPort(8888);
		url.setClusterName("asd");
		System.out.println(thriftURLBuilder.build(url));
	}

	public String build (URL url) {
		checkRequiredParamter(url);
		return buildURI(url) + "?" + buildParamters(url);
	}

	protected String buildURI (URL url) {
		return this.protocol + url.getHost() + ":" + url.getPort();
	}

	protected String buildParamters (URL tc) {
		StringBuilder parameter = new StringBuilder();
		parameter.append("timeout=" + tc.getTimeout() + "&timeUnit="
								 + StringUtil.urlEncode(tc.getUnit().name()));
		if (StringUtil.isNotEmpty(tc.getGroup())) {//tc.getGroup() != null && !tc.getGroup().isEmpty()) {
			parameter.append("&group=" + tc.getGroup());
		}
		if (StringUtil.isNotEmpty(tc.getVersion())) {
			parameter.append(
					"&version=" + StringUtil.urlEncode(tc.getVersion()));
		}
		if (StringUtil.isNotEmpty(tc.getClusterName())) {
			parameter.append("&provides=" + tc.getClusterName());
		}
		if (StringUtil.isNotEmpty(tc.getToken()))
			parameter.append("&token=" + tc.getToken());
		return parameter.toString();
	}

	public URL deBuild (String url) {
		checkProtocol(url);
		URL thriftConnection = null;
		try {

			// 欺骗一下 URL 的 url handler..
			thriftConnection = new URL();
			java.net.URL purl = new java.net.URL(url.replace(getProtocol(), "http://"));
			thriftConnection.setOriginPath(url);
			thriftConnection.setHost(purl.getHost());
			thriftConnection.setPort(purl.getPort() == -1
											 ? purl.getDefaultPort()
											 : purl.getPort());
			String query = purl.getQuery();
			String[] qs = query.split("&");
			for (String q : qs) {
				String[] q_v = q.split("=");
				String queryName = q_v[0];
				String value = q_v[1];
				try {
					value = StringUtil.urlDecode(value);
					switch (queryName) {
						case "version":
							thriftConnection.setVersion(value);
							break;
						case "group":
							thriftConnection.setGroup(value);
							break;
						case "timeout":
							thriftConnection
									.setTimeout(Integer.valueOf(value));
							break;
						case "timeUnit":
							thriftConnection
									.setUnit(TimeUnit.valueOf(value));
							break;
						case "provides":
							thriftConnection.setClusterName(value);
							break;
						case "token":
							thriftConnection.setToken(value);
							break;
						default:
							break;
					}
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return thriftConnection;
	}

	public void checkProtocol (String url) {
		if (!url.startsWith(getProtocol()))
			throw new IllegalArgumentException(
					"The protocol is invalid,modify the protocol to [thrift://]");
	}

	public void checkRequiredParamter (URL tc) {
		Assert.notNull(tc, "the [thriftConnection] argument can not null");
		Assert.notNull(tc.getHost(), "the [host] argument can not be null");
		Assert.hasLength(tc.getHost(), "the [host] argument can not be null");
		if (tc.getPort() < 0 || tc.getPort() > 65535)
			throw new IllegalArgumentException(
					"the [port] argument must be 0-65535");
		Assert.notNull(
				tc.getClusterName(),
				"the [provideName] argument can not null"
		);
		if (tc.getTimeout() == 0)
			tc.setTimeout(20);
		if (tc.getUnit() == null)
			tc.setUnit(TimeUnit.SECONDS);
	}
}
