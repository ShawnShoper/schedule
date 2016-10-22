package com.daqsoft;

/**
 * @author ShawnShoper
 * @date 16/10/13
 * @sice
 */
public class ProxyTest {
	static class A {
		public void say () {
			System.out.println("say");
		}
	}

	public static void main (String[] args) throws ClassNotFoundException {
		Proxy proxy = new Proxy();
		proxy.setProxy(new A());
		System.out.println(proxy.get(A.class.getName()).getClass());
	}

	static class Proxy {
		private Object proxy;
		private String className;

		public Object getProxy () {
			return proxy;
		}

		public void setProxy (Object proxy) {
			this.proxy = proxy;
		}

		public String getClassName () {
			return className;
		}

		public void setClassName (String className) {
			this.className = className;
		}

		public <T> T get (String className) throws ClassNotFoundException {
			T cast = (T) Class.forName(className).cast(proxy);
			return cast;
		}
	}
}
