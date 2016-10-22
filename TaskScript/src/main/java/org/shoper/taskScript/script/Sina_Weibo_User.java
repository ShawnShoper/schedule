package org.shoper.taskScript.script;

import com.alibaba.fastjson.JSONObject;
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

import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by ShawnShoper on 16/9/14.
 */
public class Sina_Weibo_User extends JobCaller {
	String url = "http://s.weibo.com/user/&nickname=%qn%&auth=%auth%&page=%page%";
	LinkedBlockingDeque<Document> docs = new LinkedBlockingDeque<>(50);
	LinkedBlockingDeque<List<User>> users = new LinkedBlockingDeque<>(50);
	AtomicBoolean fetchUrl_ab = new AtomicBoolean(false);
	AtomicBoolean fetchUsers_ab = new AtomicBoolean(false);


	@Override
	public JobResult call () throws SystemException, InterruptedException {
		CountDownLatch cdl = new CountDownLatch(3);
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
		Thread fetchUsers = new Thread(() -> {
			try {
				fetchUsers();
			} catch (InterruptedException e) {
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				fetchUsers_ab.set(true);
				cdl.countDown();
			}
		});
		fetchUsers.start();
		Thread saveData = new Thread(() -> {
			try {
				save();
			} catch (InterruptedException e) {
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				cdl.countDown();
			}
		});
		saveData.start();
		cdl.await();
		return result;
	}

	public void save () throws InterruptedException {
		for (; ; ) {
			List<Map<String, Object>> final_users = new ArrayList<>();
			if (fetchUsers_ab.get() && this.users.isEmpty())
				break;
			List<User> pojo_users;
			while (Objects.isNull(pojo_users = users.poll(1, TimeUnit.SECONDS))) ;
			pojo_users.stream().map(user -> {
				Map<String, Object> map = new HashMap<>();
				map.put("id", user.getId());
				map.put("name", user.getName());
				map.put("webSite", user.getWebSite());
				map.put("verify", user.getVerify());
				map.put("verify_name", user.getVerifyName());
				map.put("vip", user.isVip());
				return map;
			}).forEach(map -> final_users.add(map));
			saveDatas(final_users, "weibo_user");
			final_users.clear();
		}
	}


	ExecutorService executorService = Executors.newFixedThreadPool(5);

	public void fetchUsers () throws InterruptedException {

		for (; ; ) {
			if (fetchUrl_ab.get() && docs.isEmpty())
				break;
			Document tmp;
			while (Objects.isNull(tmp = docs.poll(1, TimeUnit.SECONDS))) ;
			Document doc = tmp;
			executorService.submit(() -> {
									   try {
										   parsePage(doc);
									   } catch (InterruptedException e1) {
									   }
								   }
			);
		}
	}

	class User {
		private String name;
		private String id;
		private String webSite;
		private String verify;
		private String verifyName;
		private boolean vip;

		public boolean isVip () {
			return vip;
		}

		public void setVip (boolean vip) {
			this.vip = vip;
		}

		public String getVerifyName () {
			return verifyName;
		}

		public void setVerifyName (String verifyName) {
			this.verifyName = verifyName;
		}

		public String getVerify () {
			return verify;
		}

		public void setVerify (String verify) {
			this.verify = verify;
		}

		public String getWebSite () {
			return webSite;
		}

		public void setWebSite (String webSite) {
			this.webSite = webSite;
		}

		public String getId () {
			return id;
		}

		public void setId (String id) {
			this.id = id;
		}

		public String getName () {
			return name;
		}

		public void setName (String name) {
			this.name = name;
		}

		@Override
		public String toString () {
			return "User{" +
					"name='" + name + '\'' +
					", id='" + id + '\'' +
					", webSite='" + webSite + '\'' +
					", verify='" + verify + '\'' +
					", verifyName='" + verifyName + '\'' +
					", vip=" + vip +
					'}';
		}
	}

	public void parsePage (final Document doc) throws InterruptedException {
		Elements person_name = doc.getElementsByClass("person_name");

		List<User> users = new ArrayList<>();
		person_name.stream().map(m -> {
			User user = new User();
			Element a = m.child(0);
			user.setName(a.attr("title"));
			user.setWebSite(a.attr("href"));
			user.setId(a.attr("uid"));

			//获取微博认证
			String verify = "";
			String verifyName = "";
			if (m.children().size() > 1) {
				verifyName = m.child(1).attr("title").trim();
				if ("微博机构认证".equals(verifyName))
					verify = "verify_org";
				else if ("微博个人认证".equals(verifyName))
					verify = "verify_personal";
			} else {
				verifyName = "微博未认证";
				verifyName = "verify_none";
			}
			user.setVerify(verify);
			user.setVerifyName(verifyName);
			//是否是 vip 会员
			boolean isVip = false;
			if (m.children().size() > 2) {
				String vip = m.child(2).attr("title").trim();
				if ("微博会员".equals(vip)) isVip = true;
			}
			user.setVip(isVip);
			return user;
		}).forEach(users::add);
		while (!this.users.offer(users, 1, TimeUnit.SECONDS)) ;
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
				while (!docs.offer(doc, 1, TimeUnit.SECONDS)) ;
				if (page >= total) break;
//				TimeUnit.SECONDS.sleep(10);
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
		executorService.shutdownNow();
	}
}
