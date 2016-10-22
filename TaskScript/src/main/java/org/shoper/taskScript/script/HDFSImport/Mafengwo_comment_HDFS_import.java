package org.shoper.taskScript.script.HDFSImport;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.shoper.commons.StringUtil;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.job.JobCaller;
import org.shoper.schedule.provider.module.HDFSModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Do not need any args
 *
 * @author ShawnShoper
 */
public class Mafengwo_comment_HDFS_import extends JobCaller {
	String TimeKey = Mafengwo_comment_HDFS_import.class.getName()
			+ "-updatetime";
	private final String category = "mafengwo_comment";
	private AtomicBoolean readOver = new AtomicBoolean(false);
	private AtomicBoolean buildFileOver = new AtomicBoolean(false);
	private String FOLDER ;
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH");
	Path path = new Path(
			FOLDER + "/Mafengwo_comment_" + sdf.format(new Date()) + ".txt");
	private int cap = 10000;
	FileSystem fs;
	FSDataOutputStream fsout;
	private volatile LinkedBlockingQueue<List<Map>> saveQueue = new LinkedBlockingQueue<>(
			3);
	@Autowired
	RedisTemplate redisTemplate;
	@Autowired
	MongoTemplate mongoTemplate;
	@Autowired
	HDFSModule hdfsModule;

	@Override
	protected JobResult call () throws SystemException, InterruptedException {
		try {
			fs = hdfsModule.getFileSystem();
			if (fs.exists(path))
				fs.delete(path, true);
			fsout = fs.create(path, false);
		} catch (IOException e) {
			throw new SystemException(e);
		}
		CountDownLatch cdl = new CountDownLatch(2);
		Thread readThread = new Thread(() -> {
			try {
				readData();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				readOver.set(true);
				cdl.countDown();
			}
		});
		readThread.start();
		Thread buildFile = new Thread(() -> {
			try {
				buildFile();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				buildFileOver.set(true);
				cdl.countDown();
			}
		});
		buildFile.start();
		cdl.await();
		return result;
	}

	private void buildFile () throws InterruptedException, SystemException,
			IllegalArgumentException, IOException {
		for (; ; ) {
			List<Map> lists = null;
			if (readOver.get() && saveQueue.isEmpty())
				break;

			if ((lists = saveQueue.poll(1, TimeUnit.SECONDS)) == null) {
				continue;
			}
			for (Map map : lists) {
				String city = String.valueOf(map.get("fcity_name"));
				String distination = String.valueOf(map.get("spot_name"));
				String author = String.valueOf(map.get("author"));
				String content = StringUtil.removeASCIIControlChar(
						String.valueOf(map.get("content")));
				String rate = String.valueOf(map.get("rate"));
				Float score = Float.valueOf(
						rate == null || "null".equals(rate) ? "0" : rate);
				String source = String.valueOf(map.get("category_name"));
				long time = Long.valueOf(String.valueOf(map.get("time")));
				Date date = new Date(time);
				SimpleDateFormat date_sdf = new SimpleDateFormat("yyyyMMdd");
				int date_time = Integer.valueOf(date_sdf.format(date));
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				int h = calendar.get(Calendar.HOUR);
				// 获取到数据,生成文件...
				String result = buildStringWithTable(city, distination, author,
													 content, score, source, date_time, h
				);
				// System.out.println(result);
				fsout.write(result.getBytes());
			}
		}
		System.out.println("read data over");
	}

	private String buildStringWithTable (Object... objects) {
		StringBuilder sb = new StringBuilder();
		for (Object object : objects)
			sb.append(object).append("\t");
		if (sb.length() > 0)
			sb.delete(sb.length() - 1, sb.length());
		sb.append("\n");
		return sb.toString();
	}

	private void readData () throws InterruptedException {
		Object updatetime_obj = redisTemplate.opsForValue().get(TimeKey);

		long condition = Objects.isNull(updatetime_obj) ? 0l : Long.valueOf(String.valueOf(updatetime_obj));
		// 读取所有数据总量...
		long query_all = mongoTemplate
				.count(Query.query(Criteria.where("category").is(category)
										   .and("updatetime").gt(condition)), "datas");
		long total = query_all;
		int current = 0;
		int page = 0;
		for (; ; ) {
			List<Map> datas = mongoTemplate.find(Query
														 .query(Criteria.where("category").is(category)
																		.and("updatetime").gt(condition))
														 .with(new Sort(new Order(Direction.ASC, "updatetime")))
														 .skip(page * cap).limit(cap), Map.class, "datas");

			current += datas.size();
			while (!saveQueue.offer(datas, 1, TimeUnit.SECONDS)) ;
			if (current >= total) {
				if (!datas.isEmpty()) {
					Map<String, Object> data = datas.get(datas.size() - 1);
					redisTemplate.opsForValue().set(TimeKey, String.valueOf(data.get("updatetime")),
													Integer.MAX_VALUE, TimeUnit.SECONDS
					);
				}
				break;
			}
			page++;
		}
		System.out.println("read data over");
	}

	@Override
	protected boolean checkArgs (JobParam args) {
		FOLDER=args.getCategory();  //"/user/cloudera/travel_comment";
		return true;
	}

	@Override
	public void destroy () {
		try {
			saveQueue.clear();
			saveQueue = null;
			fsout.close();
			fs.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
