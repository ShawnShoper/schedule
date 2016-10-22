package org.shoper.taskScript;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NaiveBayesClassifierReader {
	public static void main(String[] args) throws IOException {
		Configuration conf = new Configuration();
		conf.set("fs.defaultFS", "hdfs://192.168.100.178:8020");
		FileSystem fs = FileSystem.get(conf);
		FSDataInputStream in = fs.open(new Path(
				"/user/cloudera/tmp/emotion/sortor/data/"));
		BufferedReader d = new BufferedReader(new InputStreamReader(in));
		System.out.println(d.readLine());
	}
}