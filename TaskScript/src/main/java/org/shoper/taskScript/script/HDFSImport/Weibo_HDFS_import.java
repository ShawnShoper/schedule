package org.shoper.taskScript.script.HDFSImport;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.shoper.commons.StringUtil;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.prop.Category;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Do not need any args
 *
 * @author ShawnShoper
 */
public class Weibo_HDFS_import extends JobCaller {
	String TimeKey = Weibo_HDFS_import.class.getName() + "-updatetime";
	private static Category category = Category.Weibo;
	private AtomicBoolean readOver = new AtomicBoolean(false);
	private AtomicBoolean buildFileOver = new AtomicBoolean(false);
	private String FOLDER;
	private int cap = 10000;
	@Autowired
	MongoTemplate mongoTemplate;
	@Autowired
	HDFSModule hdfsModule;
	private BlockingQueue<List<Map>> saveQueue = new LinkedBlockingQueue<>(3);

	@Override
	protected JobResult call () throws SystemException, InterruptedException {
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
		FileSystem fs = hdfsModule.getFileSystem();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH");
		Path path = new Path(FOLDER + "/WEIBO_" + sdf.format(new Date()));
		if (fs.exists(path))
			fs.delete(path, true);
		FSDataOutputStream fsout = fs.create(path, true);
		for (; ; ) {
			if (readOver.get() && saveQueue.isEmpty())
				break;
			List<Map> lists = null;
			if (Objects.isNull(lists = saveQueue.poll(1, TimeUnit.SECONDS)))
				continue;
			for (Map map : lists) {
				String id = (String) map.get("id");
				String author = String.valueOf(map.get("author"));
				String author_id = (String) map.get("fid");
				String content = StringUtil.removeASCIIControlChar(
						String.valueOf((map.get("content"))));
				Long time = Long.valueOf(String.valueOf(map.get("time")));
				Date date = new Date(time);
				SimpleDateFormat date_sdf = new SimpleDateFormat("yyyyMMdd");
				int date_time = Integer.valueOf(date_sdf.format(date));
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				int h = calendar.get(Calendar.HOUR);
				String via = String.valueOf(map.get("via"));
				String forward_count = String.valueOf(map.get("forward_count"));
				String comment_count = String.valueOf(map.get("comment_count"));
				String like_count = String.valueOf(map.get("like_count"));
				// 获取到数据,生成文件...
				String result = "" + id + "\t" + author_id + "\t" + author
						+ "\t" + content + "\t" + forward_count + "\t"
						+ comment_count + "\t" + like_count + "\t" + via + "\t"
						+ time + "\t" + date_time + "\t" + h + "\n";
				fsout.write(result.getBytes());
			}
		}
		fsout.close();
		fs.close();
	}

	@Autowired
	RedisTemplate redisTemplate;

	private void readData () throws InterruptedException {
		Object updatetime_obj = redisTemplate.opsForValue().get(TimeKey);
		long condition = 0;
		if (Objects.nonNull(updatetime_obj)) condition = Long.valueOf(String.valueOf(updatetime_obj));
		// 读取所有数据总量....
		long query_all = mongoTemplate
				.count(
						Query
								.query(Criteria.where("category").is(category.getCode())
											   .and("updatetime").gt(condition)),
						//这是collection名
						"datas"
				);
		long total = query_all;
		int current = 0;
		int page = 0;
		for (; ; ) {
			List<Map> datas = mongoTemplate.find(Query
														 .query(Criteria.where("category").is(category.getCode())
																		.and("updatetime").gt(condition))
														 .skip(page * cap).limit(cap)
														 .with(new Sort(new Order(Direction.ASC, "updatetime"))),
												 Map.class, "datas"
			);
			current += datas.size();
			while (!saveQueue.offer(datas, 1, TimeUnit.SECONDS)) ;
			if (current >= total) {
				if (!datas.isEmpty()) {
					Map<String, Object> data = datas.get(datas.size() - 1);
					System.out.println(data.get("updatetime"));
					redisTemplate.opsForValue().set(TimeKey, data.get("updatetime"), Integer.MAX_VALUE, TimeUnit.SECONDS);
				}
				break;
			}
			page++;
		}
	}

	@Override
	protected boolean checkArgs (JobParam args) {
		FOLDER=args.getCategory();
		return true;
	}

	@Override
	public void destroy () {
	}
}
