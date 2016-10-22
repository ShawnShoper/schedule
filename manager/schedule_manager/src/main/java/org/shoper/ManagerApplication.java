package org.shoper;

import org.shoper.schedule.SystemContext;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
public class ManagerApplication extends SpringBootServletInitializer {
	@Override
	protected SpringApplicationBuilder configure (SpringApplicationBuilder builder) {
		return super.configure(builder);
	}

	public static void main (String[] args) throws Exception {
		ConfigurableApplicationContext web = new SpringApplicationBuilder()
				.bannerMode(Banner.Mode.CONSOLE).sources(ManagerApplication.class)
				.web(true).run(args);
		web.registerShutdownHook();
		SystemContext.context = web;
		SystemContext.waitShutdown();
	}
}