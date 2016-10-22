package org.shoper.taskScript.script;

import com.alibaba.fastjson.JSONObject;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.shoper.commons.StringUtil;
import org.shoper.http.apache.HttpClient;
import org.shoper.http.apache.HttpClientBuilder;
import org.shoper.http.apache.handle.AbuyunProxyResponseHandler;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.job.JobCaller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by ShawnShoper on 16/9/14.
 */
public class Sina_Weibo_User_website_2HDFS extends JobCaller {
	String url = "http://s.weibo.com/user/&nickname=%qn%&auth=%auth%&page=%page%";
	LinkedBlockingDeque<PageDocument> docs = new LinkedBlockingDeque<>(50);
	AtomicBoolean fetchUrl_ab = new AtomicBoolean(false);
	private String floder = "/weibo/sina/user_list";

	class PageDocument {
		private int page;
		private String webContent;

		public int getPage () {
			return page;
		}

		public void setPage (int page) {
			this.page = page;
		}

		public String getWebContent () {
			return webContent;
		}

		public void setWebContent (String webContent) {
			this.webContent = webContent;
		}
	}

	FileSystem fs = null;

	@Override
	public JobResult call () throws SystemException, InterruptedException {
		Configuration conf = new Configuration();
		conf.set("fs.defaultFS", "hdfs://192.168.100.45:9000");
		try {
			fs = FileSystem.get(conf);
			if (!fs.exists(new Path(floder))) {
				fs.mkdirs(new Path(floder));
			}
		} catch (IOException e) {
			return result;
		}
		CountDownLatch cdl = new CountDownLatch(2);
		Thread fetchUrls = new Thread(() -> {
			try {
				fetchUrls();
			} catch (InterruptedException e) {
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				fetchUrl_ab.set(true);
				cdl.countDown();
			}
		});
		fetchUrls.start();
		Thread persistence = new Thread(() -> {
			try {
				try {
					persistence();
				} catch (InterruptedException e1) {
				} catch (IOException e) {
					e.printStackTrace();
				}
			} finally {
				cdl.countDown();
			}
		});
		persistence.start();
		cdl.await();
		return result;
	}

	public static void main (String[] args) {
		LocalDate now = LocalDate.now();
		System.out.println(now.getYear());
		System.out.println(now.getMonth().getValue());
		System.out.println(now.getDayOfMonth());
	}

	public void persistence () throws InterruptedException, IOException {
		LocalDate now = LocalDate.now();
		int yy = now.getYear();
		int mm = now.getMonth().getValue();
		int dd = now.getDayOfMonth();
		String pf = yy + File.separator + mm + File.separator + dd;
		String path = floder + File.separator + pf;
		try {
			if (!fs.exists(new Path(floder + File.separator + pf))) {
				fs.mkdirs(new Path(floder + File.separator + pf));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		FSDataOutputStream out = fs.create(new Path(path + File.separator + "data"), true);
		for (; ; ) {
			if (docs.isEmpty() && fetchUrl_ab.get()) {
				break;
			}
			PageDocument pageDocument;
			while (Objects.isNull(pageDocument = docs.poll(500, TimeUnit.MILLISECONDS))) ;
			int page = pageDocument.getPage();
			String content = pageDocument.getWebContent();
			String data = page + "\t" + content.replaceAll("\n", "").trim() + "\n";
			out.write(data.getBytes());
			//content 去掉换行符
		}
		out.flush();
		out.close();
	}

	public void fetchUrls () throws InterruptedException {
		int total = 0;
		int page = 1;
		String query;
		for (; ; ) {
			try {
				query = this.url.replace("%qn%", args.getJobCode()).replace("%auth%", args.getCategory()).replace("%page%", page + "");
				HttpClient httpClient = HttpClientBuilder.custom().setUrl(query).setProxy(true).build();
				httpClient.setResponseHandle(new AbuyunProxyResponseHandler());
				Map<String, String> header = new HashMap<>();
				header.put("Proxy-Switch-Ip", "yes");
				httpClient.setRequestHeader(header);
				httpClient.setCookies(args.getCookies());
				Document document = httpClient.getDocument();
				Element element = document.getElementsByTag("script").stream().filter(c -> c.html().toString().contains("\"pid\":\"pl_user_feedList\"")).findAny().get();
				String content = element.html();
				content = content.replace("STK && STK.pageletM && STK.pageletM.view(", "");
				content = content.substring(0, content.length() - 1);
				JSONObject jsonObject = JSONObject.parseObject(content);
				String html = jsonObject.getString("html");
				if (StringUtil.isEmpty(html))
					return;
				Document doc = Jsoup.parse(html);
				if (doc.getElementsByClass("W_pages").get(0).children().isEmpty())
					return;
				Elements list = doc.getElementsByAttributeValue("action-type", "feed_list_page_morelist");
				if (page == 1)
					total = Integer.valueOf(list.get(0).getElementsByTag("a").last().text().replace("第", "").replace("页", ""));
				PageDocument pageDocument = new PageDocument();
				pageDocument.setPage(page);
				pageDocument.setWebContent(doc.toString());
				while (!docs.offer(pageDocument, 1, TimeUnit.SECONDS)) ;
				if (page >= total) break;
				page++;
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	protected boolean checkArgs (JobParam args) {
		return true;
	}

	@Override
	public void destroy () {

	}

}
