package org.shoper.taskScript.script.HDFSImport;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.lib.aggregate.StringValueMax;
import org.shoper.commons.DateUtil;
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
public class BaiDuTop_Region_HDFS_import extends JobCaller {
	private Category category = Category.BaiDU_Top;
	private AtomicBoolean readOver = new AtomicBoolean(false);
	private AtomicBoolean buildFileOver = new AtomicBoolean(false);
	private String FOLDER ;
	String TimeKey = BaiDuTop_Region_HDFS_import.class.getName()
			+ "-updatetime";
	private int cap = 10000;
	FileSystem fs = null;
	private BlockingQueue<List<Map>> saveQueue = new LinkedBlockingQueue<>(10);
	@Autowired
	HDFSModule hdfsModule;
	@Autowired
	MongoTemplate mongoTemplate;
	@Autowired
	RedisTemplate redisTemplate;

	@Override
	protected JobResult call () throws SystemException, InterruptedException {

		fs = hdfsModule.getFileSystem();
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
		readThread.start();
		buildFile.start();
		cdl.await();
		return result;
	}

	private void buildFile () throws InterruptedException, SystemException,
			IllegalArgumentException, IOException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH");
		Path path = new Path(
				FOLDER + "/BaiduTop_" + sdf.format(new Date()) + "2");
		if (fs.exists(path))
			fs.delete(path, true);
		FSDataOutputStream fsout = fs.create(path, true);
		for (; ; ) {
			if (readOver.get() && saveQueue.isEmpty())
				break;
			List<Map> lists = saveQueue.poll(10, TimeUnit.SECONDS);
			if (lists == null || lists.isEmpty())
				continue;
			long d1 = System.currentTimeMillis();
			StringBuffer sb = new StringBuffer();
			for (Map map : lists) {
				String subcategory = String.valueOf(map.get("type_name"));
				String province = String.valueOf(map.get("f_city_name"));
				String city = String.valueOf((map.get("city_name")));
				if (StringUtil.isEmpty(province) || "null".equals(province))
					province = city;
				Long time = Long.valueOf(String.valueOf(map.get("updatetime")));
				Date date = new Date(time);
				SimpleDateFormat date_sdf = new SimpleDateFormat("yyyyMMdd");
				int date_time = Integer.valueOf(date_sdf.format(date));
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				int h = calendar.get(Calendar.HOUR);
				String kword = String.valueOf(map.get("keyWord"));
				long search_count = Long
						.valueOf(String.valueOf(map.get("searcheCount")));
				int popularity = Integer
						.valueOf(String.valueOf(map.get("percentage")));
				String is_new = String.valueOf(map.get("is_new"));
				if (is_new == null || "null".equals(is_new))
					is_new = "";
				String trend = String.valueOf(map.get("trend"));
				String cate = String.valueOf(map.get("f_type_name"));
				int change_rate = Integer
						.valueOf(String.valueOf(map.get("changeRate"))); // 获取到数据,生成文件...
				String result = buildStringWithTable(province, city, cate,
													 subcategory, kword, search_count, popularity, is_new,
													 trend, change_rate, date_time, h
				);
				sb.append(result + "\n");
			}
			fsout.write(sb.substring(0, sb.length() - 1).getBytes());
			fsout.flush();
			lists.clear();
			System.out.println("write data spend times " + DateUtil.TimeToStr(System.currentTimeMillis() - d1));
		}
		fsout.close();
		fs.close();
	}

	private String buildStringWithTable (Object... objects) {
		StringBuilder sb = new StringBuilder();
		for (Object object : objects) {
			sb.append(object).append("\t");
		}
		String result = null;
		if (sb.length() > 0)
			result = sb.substring(0, sb.lastIndexOf("\t"));
		result += "\n";
		return result;
	}

	private void readData () throws InterruptedException {
		Object updatetime_obj = redisTemplate.opsForValue().get(TimeKey);

		long condition = Objects.isNull(updatetime_obj) ? 0l : Long.valueOf(String.valueOf(updatetime_obj));
		// 读取所有数据总量....
		System.out.println(condition);
		long query_all = mongoTemplate
				.count(
						Query
								.query(Criteria.where("category").is(category.getCode())
											   .and("updatetime").gt(condition)),
						"datas"
				);
		System.out.println("total" + query_all);
		long total = query_all;
		int current = 0;
		int page = 0;
		// 根据时间来查询数据,按增量时间来定时做处理导最新的数据
		// 每次从缓存中取出上一次存的时间节点,然后根据时间节点获取到现在的最新更新的数据
		for (; ; ) {
			long d1 = System.currentTimeMillis();
			List<Map> datas = mongoTemplate.find(Query
														 .query(Criteria.where("category").is(category.getCode())
																		.and("updatetime").gt(condition))
														 //.skip(page * cap)
														 .limit(cap)
														 .with(new Sort(new Order(Direction.ASC, "updatetime"))),
												 Map.class, "datas"
			);
			current += datas.size();
			while (!saveQueue.offer(datas, 1, TimeUnit.SECONDS)) ;
			if (!datas.isEmpty()) {
				Map<String, Object> data = datas.get(datas.size() - 1);
				condition = Long.valueOf(String.valueOf(data.get("updatetime")));
				if (current >= total) {
					redisTemplate.opsForValue().set(TimeKey, Long.valueOf(String.valueOf(data.get("updatetime"))),
													Integer.MAX_VALUE, TimeUnit.SECONDS
					);
					break;
				}
			} else {
				break;
			}
			page++;
		}

		System.out.println("over");
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
