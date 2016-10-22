package org.shoper.taskScript.script;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.text.ParseException;
import java.util.Calendar;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

/**
 * Created by ShawnShoper on 16/9/26.
 */
public class HDFSTestApacheByCloudera {
	public static void main (String[] args) throws IOException, ParseException {
//		//{"hostKey":"fs.defaultFS","hostValue":"hdfs://192.168.100.178:8020"}
//		Configuration conf = new Configuration();
//		conf.set("fs.defaultFS", "hdfs://192.168.100.45:9000");
//		FileSystem fileSystem = FileSystem.get(conf);
//		//fileSystem.create(new Path("/"));
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-ss");
//		Path path=new Path("/a/test"+sdf.format(new Date()));
//		FSDataOutputStream fsout=fileSystem.create(path,true);
//		StringBuffer sb = new StringBuffer();
//		sb.append("囧的的个埃尔文娃娃儿认为好过分 了破解皮卡皮卡【 我就看见胸襟和很顾家很健康护理");
//		fsout.write(sb.substring(0, sb.length() - 1).getBytes());
//		fsout.flush();
//		fsout.close();
//		System.out.println(fileSystem.exists(new Path("/a")));
//		LocalDate now = LocalDate.now();
//		System.out.println(now.getYear());
//		System.out.println(now.getMonthValue());
//		System.out.println(now.getDayOfMonth());
		Calendar now = Calendar.getInstance();
		System.out.println("年: " + now.get(Calendar.YEAR));
		System.out.println("月: " + (now.get(Calendar.MONTH) + 1) + "");
		System.out.println("日: " + now.get(Calendar.DAY_OF_MONTH));
		System.out.println("时: " + now.get(Calendar.HOUR_OF_DAY));
		System.out.println("分: " + now.get(Calendar.MINUTE));
		System.out.println("秒: " + now.get(Calendar.SECOND));
		System.out.println("当前时间毫秒数：" + now.getTimeInMillis());
		System.out.println(now.getTime());

		Date d = new Date();
		System.out.println(d);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateNowStr = sdf.format(d);
		System.out.println("格式化后的日期：" + dateNowStr);

		String str = "2012-1-13 17:26:33";  //要跟上面sdf定义的格式一样
		Date today = sdf.parse(str);
		System.out.println("字符串转成日期：" + today);

	}
}
