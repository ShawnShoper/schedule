package org.shoper.taskScript;

import org.junit.Test;

import java.io.File;

/**
 * Created by ShawnShoper on 16/9/30.
 */
public class FilePathTest {
	public static void main (String[] args) {
		File file = new File("pom.xml");
		System.out.println(file.getAbsoluteFile());
		System.out.println(file.exists());
	}
	@Test
	public void test(){
		File file = new File("pom.xml");
		System.out.println(file.getAbsoluteFile());
		System.out.println(file.exists());
	}
}
