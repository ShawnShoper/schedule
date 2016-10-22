package org.shoper.taskScript.script;

import org.apache.commons.collections.map.HashedMap;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.shoper.commons.StringUtil;
import org.shoper.http.apache.HttpClient;
import org.shoper.http.apache.HttpClientBuilder;
import org.shoper.http.apache.proxy.ProxyServerPool;
import org.shoper.http.exception.HttpClientException;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.job.JobCaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Created by ShawnShoper on 16/9/27.
 */
public class Bigdata_palmyou extends JobCaller {
	private String url = "http://bigdata.palmyou.com/ntsms/homeSearchTeam.action?coptype=%ct%&head=%head%&carrivestate=&bgndate=2016-09-28&enddate=2016-09-28&currentPage=1";
	private String detailUrl = "http://bigdata.palmyou.com/ntsms/main/domesticTeamRevView.action?uid=ed531f73-0548-475f-9e14-ee98ef796601";
	private LinkedBlockingQueue<List<Map<String, String>>> listData = new LinkedBlockingQueue<>(
			100);
	private String rootUrl = "http://bigdata.palmyou.com/";
	private AtomicBoolean list_over = new AtomicBoolean(false);

	public static void main (String[] args) throws InterruptedException, SystemException, FileNotFoundException {
		ProxyServerPool.importProxyServer(new File("proxyip.ls"), Charset.forName("utf-8"));
		JobCaller jobCaller = new Bigdata_palmyou();
		JobParam jobParam = new JobParam();
		jobParam.setCookies("JSESSIONID=FDE81DBBC06F5F99ADD22C120CDD9FB2; DWRSESSIONID=YDMcAMniuD2iMiyu4AH$eOiNFtl; Hm_lvt_1b1f059ea2c708661a7c62b98791ca71=1474596123; aliyungf_tc=AQAAAL1ztgHVLwwAOdaXthZd6XOLBTLb; Hm_lvt_8fed1432e6c3c3c8f80a8016890c19bf=1474595731,1474948302; Hm_lpvt_8fed1432e6c3c3c8f80a8016890c19bf=1474962728");
		jobParam.setCategory("出境");
		jobParam.setType("出境游组团查询");
		jobCaller.init(jobParam);
		jobCaller.run();
		jobCaller.destroy();
	}

	private void fetchDetail () throws InterruptedException {
		for (; ; ) {
			if (listData.isEmpty() && list_over.get()) break;
			List<Map<String, String>> datas;
			while (Objects.isNull(datas = listData.poll(1, TimeUnit.SECONDS))) ;
			datas.stream().parallel().map(this::parseTableData).forEach(System.out::println);
		}
	}


	public Map<String, Object> parseTableData (Map<String, String> map) {
		Map<String, Object> data = new HashMap<>();
		String link = map.get("link");
		try {
			Map<String, String> headers = new HashMap<>();
			headers.put("Cookie", args.getCookies());
			HttpClient httpClient = HttpClientBuilder.custom().setProxy(true).setUrl(link).build();
			httpClient.setRequestHeader(headers);
			Document document = null;
				document = httpClient.getDocument();

			if(Objects.isNull(document))return data;
			Elements baseinfo2s = document.getElementsByClass("baseInfor2");
			//团队基本信息
			if (Objects.nonNull(baseinfo2s) && !baseinfo2s.isEmpty()) {
				Element ele = baseinfo2s.get(0);
				Elements trs = ele.getElementsByTag("tr");
				trs.stream().forEach(e -> {
					Elements child = e.children();
					for (int i = 0; i < child.size(); i += 2) {
						String key = child.get(i).text();
						if (StringUtil.isEmpty(key)) continue;
						String value = child.get(i + 1).text();
						data.put(key, value);
					}
				});
			}
			//导游信息
			Elements baseinfos = document.getElementsByClass("baseInfor");
			if (Objects.nonNull(baseinfos) && !baseinfos.isEmpty()) {
				Element ele = baseinfos.get(0);
				Elements trs = ele.getElementsByTag("tr");
				trs.stream().forEach(e -> {
					Elements child = e.children();
					for (int i = 0; i < child.size(); i += 2) {
						String key = child.get(i).text();
						if (StringUtil.isEmpty(key.trim())) continue;
						String value = child.get(i + 1).text();
						data.put(key, value);
					}
				});
			}
			Element routeList = document.getElementById("routeList");
			if (Objects.nonNull(routeList)) {
				Elements trs = routeList.getElementsByTag("tr");
				if (Objects.nonNull(trs) && !trs.isEmpty()) {
					List<Map<String, Object>> routes = new ArrayList<>();
					for (Element t : trs) {
						Map<String, Object> route = new HashedMap();
						Elements tds = routeList.getElementsByTag("td");
						route.put("day", tds.get(0).text());
						route.put("station", tds.get(1).text());
						route.put("destinationCity", tds.get(2).text());
						route.put("destinationProvince", tds.get(3).text());
						route.put("scenicSpot", tds.get(4).text());
						Element emt = tds.get(5).child(0);
						if ("disabled".equals(emt.attr("disabled")))
							route.put("scenicSpot", false);
						else
							route.put("scenicSpot", true);
						routes.add(route);
					}
					data.put("routes", routes);
				}
			}
			//游客信息
			Element guestList = document.getElementById("guestList");
			if (Objects.nonNull(guestList)) {
				Elements trs = guestList.getElementsByTag("tr");
				if (Objects.nonNull(trs) && !trs.isEmpty()) {
					List<Map<String, Object>> guests = new ArrayList<>();
					for (Element t : trs) {
						Map<String, Object> guest = new HashedMap();
						Elements tds = t.getElementsByTag("td");
						guest.put("name", tds.get(1).text());
						guest.put("pinyin_familyname", tds.get(2).text());
						guest.put("pinyin_lastname", tds.get(3).text());
						guest.put("gender", tds.get(4).text());
						guest.put("birthday", tds.get(5).text());
						guest.put("birthplace", tds.get(6).text());
						guests.add(guest);
					}
					data.put("guests", guests);
				}
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (HttpClientException e) {
			e.printStackTrace();
		}
		return data;
	}

	public void fetchList () throws InterruptedException {
		int page = 0;
		String query = url.replace("%ct%", args.getCategory()).replace("%head%", args.getType());
		for (; ; ) {
			++page;
			Map<String, String> headers = new HashMap<>();
			headers.put("Cookie", args.getCookies());
			Document document = null;
			try {
				HttpClient
						httpClient = HttpClientBuilder.custom().setUrl(query.replace("%p%", page + "")).setProxy(true).build();
				httpClient.setRequestHeader(headers);
				document = httpClient.getDocument();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (HttpClientException e1) {
				e1.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
			if (Objects.isNull(document)) continue;
			Element list_table = document.getElementById("listTable");
			if (Objects.isNull(list_table)) continue;
			Elements trs = list_table.getElementsByTag("tr");
			List<Map<String, String>> list = trs.stream().filter(e -> e.hasAttr("align")).map(e -> {
				Map<String, String> map = new HashMap<>();
				Element a = e.child(0).child(0);
				String link = a.attr("href");
				String odd = a.text();
				map.put("link", rootUrl + link);
				map.put("odd", odd);
				return map;
			}).collect(Collectors.toList());
			while (!listData.offer(list, 1, TimeUnit.SECONDS)) ;
		}
	}


	@Override
	protected JobResult call () throws SystemException, InterruptedException {
		CountDownLatch cdl = new CountDownLatch(2);
		Thread fetchList = new Thread(() -> {
			try {
				fetchList();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				list_over.set(true);
				cdl.countDown();
			}
		});
		fetchList.start();
		Thread fetchDetail = new Thread(() -> {
			try {
				fetchDetail();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				cdl.countDown();
			}
		});
		fetchDetail.start();
		cdl.await();
		return result;
	}


	@Override
	protected boolean checkArgs (JobParam args) {
		return true;
	}

	@Override
	public void destroy () {

	}
}
