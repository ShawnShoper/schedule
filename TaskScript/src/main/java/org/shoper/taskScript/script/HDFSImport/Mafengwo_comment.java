package org.shoper.taskScript.script.HDFSImport;

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.shoper.commons.StringUtil;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.job.JobCaller;
import org.shoper.schedule.resp.ReportResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.shoper.http.apache.HttpClient;
import org.shoper.http.apache.HttpClientBuilder;
import org.shoper.http.exception.HttpClientException;
import com.alibaba.fastjson.JSONObject;
/**
 * 马蜂窝点评数据抓取...<br>
 * Required args jobCode and JobName place id<br>
 * 
 * @author ShawnShoper
 *
 */
public class Mafengwo_comment extends JobCaller
{
	// %JD% = 景点ID
	private final String JD_QUERY = "http://www.mafengwo.cn/jd/%JD%/gonglve.html";
	// %id%== 景点 ID %page% 页码 %t%当前时间锉
	private final String NOTE_URL = "http://www.mafengwo.cn/gonglve/ajax.php?act=get_poi_comments&poi_id=%id%&type=0&category=4&page=%page%&ts=%t%";
	private final String WEBSITE = "http://www.mafengwo.cn/";
	private final String ID = "mafengwo";
	private final String category = "mafengwo_comment";
	private final String category_name = "蚂蜂窝点评";
	private AtomicBoolean queryAllJD = new AtomicBoolean(false);
	private AtomicBoolean parseDone = new AtomicBoolean(false);
	private AtomicBoolean saveDone = new AtomicBoolean(false);
	private final long limitTime = new Date().getTime();
	private final String CHARSET = "UTF_8";
	volatile LinkedBlockingQueue<Map<String, String>> spotQueue = new LinkedBlockingQueue<>();
	volatile LinkedBlockingQueue<Map<String, Object>> saveQueue = new LinkedBlockingQueue<>();
	ExecutorService service = Executors.newFixedThreadPool(40);
	@Override
	public JobResult call() throws SystemException
	{
		CountDownLatch cdl = new CountDownLatch(3);
		service.submit(new Runnable() {
			@Override
			public void run()
			{
				try
				{
					queryAllJD();
				} catch (Exception e)
				{
					e.printStackTrace();
				} finally
				{
					queryAllJD.set(true);
					cdl.countDown();
				}
			}

		});
		service.submit(new Runnable() {
			@Override
			public void run()
			{
				try
				{
					parseData();
				} catch (Exception e)
				{
					e.printStackTrace();
				} finally
				{
					parseDone.set(true);
					cdl.countDown();
				}
			}
		});

		service.submit(new Runnable() {

			@Override
			public void run()
			{
				try
				{
					saveData();
				} catch (Exception e)
				{
					e.printStackTrace();
				} finally
				{
					saveDone.set(true);
					cdl.countDown();
				}
			}
		});
		try
		{
			cdl.await();
			result.setSuccess(true);
		} catch (InterruptedException e)
		{
			result.setSuccess(false);
		} finally
		{
			result.setDone(true);
		}
		logger.info("Task over,update amount:[" + result.getUpdateCount().get()
				+ "],new add amount:[" + result.getSaveCount().get() + "]");
		return result;
	}
	private void queryAllJD() throws SystemException
	{
		String jd_query = JD_QUERY.replace("%JD%", args.getJobCode());
		HttpClient hc = null;
		try {
			hc = HttpClientBuilder.custom().setUrl(jd_query).setCharset(CHARSET).setTimeout(20).setTimeoutUnit(TimeUnit.SECONDS).setRetry(3).build();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		Document document = null;
		try
		{
			document = hc.getDocument();
		} catch (HttpClientException e)
		{
			logger.error("Read data for [{}] faild.", jd_query, e);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		if (document == null)
		{
			logger.info("Read data for {} faild.Task over...");
		}
		Elements class_list = document.getElementsByAttributeValue("class",
				"list");
		if (class_list == null || class_list.size() != 1)

		{
			logger.error("This website {} rule has be changed..TaskOver",
					WEBSITE);
			result.setError(ReportResponse.Error.EXCEP);
			result.setErrMessage("This website " + args.getJobName()
					+ " rule has be changed..");

		}
		Element list_div = class_list.get(0);
		Elements list_li = list_div.getElementsByTag("li");
		if (list_li == null || list_li.isEmpty())
		{
			logger.info("This city {} has no spots,TaskOver",
					args.getJobName());
			return;
		}
		// read emt-li data
		for (Element emt_li : list_li)
		{
			Element emt_a = emt_li.getElementsByTag("a").get(0);
			String SpotID = emt_a.attr("href").replaceAll("/poi/|\\.html", "");
			String spotName = emt_a.text();
			Map<String, String> spot = new HashMap<String, String>();
			spot.put("spotID", SpotID);
			spot.put("spotName", spotName);
			spotQueue.offer(spot);

		}
	}
	@Autowired
	MongoTemplate mongoTemplate;
	private void parseData() throws InterruptedException
	{
		for (;;)
		{
			// check flag empty...
			if (spotQueue.isEmpty() && queryAllJD.get())
				break;
			Map<String, String> data = spotQueue.poll(2, TimeUnit.SECONDS);
			if (data == null)
				continue;
			String spotID = data.get("spotID");
			String spotName = data.get("spotName");
			// Checking db has exists the spot id
			long count = mongoTemplate.count(
					Query.query(Criteria.where("spot_id").is(spotID)), "datas");
			boolean byTime = false;
			if (count > 0)
				byTime = true;
			// %id%== 景点 ID %page% 页码 %t%当前时间锉

			int page = 0;
			int amount = 0;
			inner : for (;;)
			{
				logger.info("抓取当前分类:{}-抓取量{}", spotName, amount);
				String query = NOTE_URL.replace("%id%", spotID)
						.replace("%t%", System.currentTimeMillis() + "")
						.replace("%page%", page + "");
				org.shoper.http.apache.HttpClient hc = null;
				String doc = null;
				try {
					hc  = HttpClientBuilder.custom().setUrl(query).setCharset(CHARSET).setTimeout(20).setTimeoutUnit(TimeUnit.SECONDS).setRetry(3).build();
					 doc = hc.doGet();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (HttpClientException e) {
					e.printStackTrace();
				} catch (TimeoutException e) {
					e.printStackTrace();
				}
				if (doc == null)
					continue;
				try
				{
					JSONObject json = JSONObject.parseObject(doc);
					String msg = json.getString("msg");
					if (!"succ".equals(msg))
						break;
					String html = json.getJSONObject("html").getString("html");
					Document document = Jsoup.parse(html);
					Elements emt_lis = document.getElementsByAttributeValue(
							"class", "rev-item clearfix");
					if (emt_lis == null || emt_lis.isEmpty())
						break;
					for (Element emt_li : emt_lis)
					{
						Map<String, Object> commont_data = new HashMap<>();
						if (!"li".equals(emt_li.tagName()))
							continue;
						String id = ID + "_" + emt_li.attr("id");
						Element emt_txt = emt_li
								.getElementsByAttributeValue("class", "rev-txt")
								.get(0);
						Element emt_name = emt_li
								.getElementsByAttributeValue("class", "name")
								.get(0);
						Element emt_star = emt_li
								.getElementsByAttributeValueStarting("class",
										"star")
								.get(0);
						Element emt_info = emt_li.getElementsByAttributeValue(
								"class", "info clearfix").get(0);
						Element emt_date = emt_info.getElementsByTag("span")
								.get(0);
						String date_text = emt_date.text();
						SimpleDateFormat sdf = new SimpleDateFormat(
								"yyyy-MM-dd HH:mm:dd");
						Date date = sdf.parse(date_text);
						String cn = emt_star.className();
						int rate = Integer
								.valueOf(cn.substring(cn.length() - 1));
						long creatTime = date.getTime();
						if (byTime)
						{
							Calendar calendar = Calendar.getInstance();
							calendar.setTimeInMillis(limitTime);
							calendar.add(Calendar.DAY_OF_MONTH, -7);
							long limit = calendar.getTimeInMillis();
							if (creatTime < limit)
								break inner;
						}
						String author = emt_name.text();
						String content = emt_txt.text();
						commont_data.put("author", author);
						commont_data.put("content", content);
						commont_data.put("time", creatTime);
						commont_data.put("updatetime",
								System.currentTimeMillis());
						commont_data.put("id", id);
						commont_data.put("rate", rate);
						commont_data.put("spot_id", spotID);
						commont_data.put("spot_name", spotName);
						commont_data.put("category", this.category);
						commont_data.put("category_name", category_name);
						commont_data.put("fcity", args.getJobCode());
						commont_data.put("fcity_name", args.getJobName());
						result.getHandleCount().incrementAndGet();
						this.saveQueue.offer(commont_data);
						amount++;
					}
				} catch (Exception e)
				{
					e.printStackTrace();
					break;
				}
				page++;
			}
		}
	}
	int limit = 1000;
	private void saveData() throws InterruptedException
	{

		List<Map<String, Object>> saveList = new ArrayList<>(limit);
		for (;;)
		{
			// check flag empty...
			if (saveQueue.isEmpty() && parseDone.get())
			{
				this.save(saveList);
				saveList.clear();
				break;
			}
			Map<String, Object> data = this.saveQueue.poll(2, TimeUnit.SECONDS);
			if (data == null)
				continue;
			saveList.add(data);
			if (saveList.size() >= limit)
			{
				this.save(saveList);
				saveList.clear();
			}
		}
	}
	/**
	 * save data and calc update or save amount
	 * 
	 * @param datas
	 */
	void save(List<Map<String, Object>> datas)
	{
		if (datas != null && !datas.isEmpty())
			super.saveDatas(datas);
	}
	@Override
	public boolean checkArgs(JobParam args)
	{
		if (StringUtil.isEmpty(args.getJobCode())
				|| StringUtil.isEmpty(args.getJobName()))
			return false;
		return true;
	}

	@Override
	public void destroy()
	{
		service.shutdown();
	}

}
