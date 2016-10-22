package org.shoper.schedule;

import java.io.File;
import java.io.IOException;

/**
 * 解决部署为jar包后，无法动态编译java的问题（classpath无法获取）
 * Created by admin on 2016-8-25.
 */
public class CompileTool {
	public static void compileTool () {
		String classPath = System.getProperty("java.class.path");
		File file = new File("provider_application.jar");
		if (file.exists()) {
			File lib = new File("target/");
			lib.mkdir();
			ZIPReader reader = new ZIPReader(file);
			StringBuilder builder = new StringBuilder();
			reader.ls().forEach(s -> {
									if (s.startsWith("BOOT-INF/lib")) {
										try {
											File libDir = new File(lib, s);
											libDir.getParentFile().mkdir();
											builder.append(File.pathSeparator).append(libDir.getAbsolutePath());
											reader.unpack(s, libDir);
										} catch (IOException E) {
											E.printStackTrace();
										}
									}
								}
			);
			System.setProperty("java.class.path", classPath + builder.toString());
		}
	}
}