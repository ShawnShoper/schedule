package org.shoper.schedule;

import org.shoper.http.apache.proxy.ProxyServerPool;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;

@SpringBootApplication
public class ProviderApplication {
	public static void main (String[] args) throws FileNotFoundException {
		ProxyServerPool.importProxyServer(new File("proxyip.ls"), Charset.forName("utf-8"));
		CompileTool.compileTool();
		ConfigurableApplicationContext context = null;
		try {
			context = new SpringApplicationBuilder()
					.bannerMode(Banner.Mode.CONSOLE).sources(ProviderApplication.class)
					.run(args);
			SystemContext.context = context;
			context.registerShutdownHook();
		} catch (Exception e) {
			SystemContext.shutdown();
		}
		SystemContext.waitShutdown();
	}
}
