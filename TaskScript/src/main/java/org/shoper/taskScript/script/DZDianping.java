package org.shoper.taskScript.script;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.shoper.commons.StringUtil;
import org.shoper.commons.TimeUtil;

import org.shoper.http.apache.HttpClientBuilder;
import org.shoper.http.apache.handle.AbuyunProxyResponseHandler;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.job.JobCaller;
import org.shoper.http.apache.HttpClient;
import org.shoper.http.exception.HttpClientException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 大众点评网站数据
 *
 * @author ShawnShoper
 *
 */
public class DZDianping extends JobCaller
{
	// 大众点评 Domain
	String rootUrl = "http://www.dianping.com";
	// 大众点评搜索 URL,cityCode 城市代码, categoryCode 分类代码
	String searchUrl = "http://www.dianping.com/search/category/%cityCode%/%categoryCode%";
	String shopInfoUrl = "http://www.dianping.com/ajax/json/shop/wizard/BasicHideInfoAjaxFP?shopId=%shopId%";
	private String ID = "dazhongdianping_";
	AtomicBoolean scanList = new AtomicBoolean(false);
	private AtomicBoolean shopIDOver = new AtomicBoolean(false);
	private AtomicBoolean shopDetailOver = new AtomicBoolean(false);
	int timeout = 20;
	TimeUnit timeUnit = TimeUnit.SECONDS;
	int retry = 3;
	private String charset = "UTF-8";
	private LinkedBlockingQueue<String> shopIDs = new LinkedBlockingQueue<>(100);
	private LinkedBlockingQueue<Map<String, Object>> shopDetails = new LinkedBlockingQueue<>(10);
	Status status = Status.Category;
	Thread saveDatas;
	Thread shopList;
	Thread shopDetals;

	enum ExtractType
	{
		Category, Area, SubCategory, SubArea;
	}

	enum Status
	{
		Category, CategoryArea, CategorySubArea, SubCategory, SubCategoryArea, SubCategorySubArea, Area, SubArea;
	}

	/**
	 * 扫描所有抓取到的网站并且抓取里面的shopId
	 * @throws HttpClientException
	 * @throws InterruptedException
	 */
	private String Symbol;
	 void scanList() throws HttpClientException, InterruptedException, TimeoutException {
		List<String> urls = getAllLinks(searchUrl, ExtractType.Category);
		for (String url : urls) {
			HttpClient httpClient = null;
			try {
				httpClient = HttpClientBuilder.custom().setUrl(url).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
				Map<String,String> requestHeader = new HashMap<String, String>();
				requestHeader.put("Proxy-Switch-Ip","yes");
				httpClient.setRequestHeader(requestHeader);
				httpClient.setResponseHandle(new AbuyunProxyResponseHandler());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
				Document doc = httpClient.getDocument();
                if(Objects.isNull(doc)) continue;
				//判断这个分类下的所有信息数如果大于750就是分页50页。
				if (dataCount(doc) >= 750) {
					for (int index = 1; index < 51; ++index) {
						String pageurl = url +Symbol+ "p" + index;
						HttpClient hc = null;
						try {
							hc = HttpClientBuilder.custom().setUrl(pageurl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
						Map<String,String> requestHeader = new HashMap<String, String>();
						requestHeader.put("Proxy-Switch-Ip","yes");
						hc.setRequestHeader(requestHeader);
						hc.setResponseHandle(new AbuyunProxyResponseHandler());
						if(Objects.isNull(hc)) continue;
							Document currentDocument = null;
							currentDocument = hc.getDocument();
                            if(Objects.isNull(currentDocument)) continue;
							Elements shoplists = currentDocument.getElementsByAttributeValueContaining("class", "shop-all-list");

							Elements uls = shoplists.get(0).getElementsByTag("ul");
							Elements lis = uls.get(0).getElementsByTag("li");
							for (Element li : lis) {
								Elements pic = li.getElementsByClass("pic");
								Elements a_shopid = pic.get(0).getElementsByTag("a");
								if (StringUtil.isNull(a_shopid) || a_shopid.isEmpty())
									continue;
								a_shopid.stream().parallel().forEach(a ->
								{
									String shopid = a.attr("href").substring(a.attr("href").lastIndexOf("/") + 1);
									try {
										shopIDs.offer(shopid, 2, TimeUnit.SECONDS);
									} catch (InterruptedException e) {
									}
								});
							}
					}
				} else {
					int total = dataCount(doc) % 15 > 0 ? dataCount(doc) / 15 + 1 : dataCount(doc) / 15;
					for (int index = 1; index < total + 1; ++index) {
						String pageurl = url +Symbol +"p" + index;
						HttpClient hc = null;
						try {
							hc = HttpClientBuilder.custom().setUrl(pageurl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
							Map<String,String> requestHeader = new HashMap<String, String>();
							requestHeader.put("Proxy-Switch-Ip","yes");
							hc.setRequestHeader(requestHeader);
							hc.setResponseHandle(new AbuyunProxyResponseHandler());
							if(Objects.isNull(hc)) continue;
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}

							Document currentDocument = null;
							currentDocument = hc.getDocument();
                            if(Objects.isNull(currentDocument)) continue;
							Elements shoplists = currentDocument.getElementsByAttributeValueContaining("class", "shop-all-list");

							Elements uls = shoplists.get(0).getElementsByTag("ul");
							Elements lis = uls.get(0).getElementsByTag("li");
							for (Element li : lis) {
								Elements pic = li.getElementsByClass("pic");
								Elements a_shopid = pic.get(0).getElementsByTag("a");
								if (StringUtil.isNull(a_shopid) || a_shopid.isEmpty())
									continue;
								a_shopid.stream().parallel().forEach(a ->
								{
									String shopid = a.attr("href").substring(a.attr("href").lastIndexOf("/") + 1);
									try {
										shopIDs.offer(shopid, 2, TimeUnit.SECONDS);
									} catch (InterruptedException e) {
									}
								});
							}
					}
				}

		}
	}
	/**
	 * 获取所有连接,可能会循环递归遍历各个分类<br>
	 * Created by ShawnShoper 2016年5月25日
	 *
	 * @param rurl
	 * @param extractType
	 * @return
	 * @throws HttpClientException
	 * @throws InterruptedException
	 */
	 List<String> getAllLinks(String rurl, ExtractType extractType)
			throws HttpClientException, InterruptedException
	{
		long st = System.currentTimeMillis();
		List<String> allUrl = new ArrayList<>();
		HttpClient hc = null;
		try {
			hc = HttpClientBuilder.custom().setUrl(rurl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
			logger.info("url: {}",rurl);
		} catch (MalformedURLException e) {
			logger.info("出错的 rurl {}",rurl);
			e.printStackTrace();
		}
		Map<String,String> requestHeader = new HashMap<String, String>();
		requestHeader.put("Proxy-Switch-Ip","yes");
		hc.setRequestHeader(requestHeader);
		hc.setResponseHandle(new AbuyunProxyResponseHandler());
		if(Objects.isNull(hc)) return allUrl;
		try {
			Document doc = hc.getDocument();
            if(Objects.isNull(doc)) return allUrl;
			Integer dataCount = dataCount(doc);
			if (dataCount == null || dataCount == 0)
				return allUrl;
			if (dataCount > 750)
			{
				// 如果 dataCount 大于750,那么继续通过分类进行读取数据
				// 第一步,检查该地区下分类链接
				List<String> categoryQueue = scanCategory(doc, extractType);
				if (categoryQueue.isEmpty()) {
					allUrl.add(rurl);
					return allUrl;
				}
				ExtractType et = null;
				if (extractType.equals(ExtractType.Category))
					et = ExtractType.SubCategory;
				else if (extractType.equals(ExtractType.SubCategory))
					et = ExtractType.Area;
				else if (extractType.equals(ExtractType.Area))
					et = ExtractType.SubArea;
				else
				{
					return allUrl;
				}
				for (String u : categoryQueue){

					allUrl.addAll(getAllLinks(u, et));
				}
			}
		}catch (Exception e){
			e.printStackTrace();
			return allUrl;
		}
		TimeUtil.DiffTime dt = new TimeUtil.DiffTime();
		allUrl.add(rurl);
		return allUrl;

	}

	/**
	 * 获得页面分页，商户信息
	 * @throws InterruptedException
	 */
	private String scoretype;
	private  final String category="dazhongdianping" ;
	private String type;
	private String province;
	private String cityCode;
	private final String source = "大众点评";
	AtomicInteger available = new AtomicInteger(0);
	ExecutorService shopDetailServer = Executors.newFixedThreadPool(7);
	private void showDeltails() throws InterruptedException
	{
		roolabel:
		for (;;)
		{
			if (scanList.get() && shopIDs.isEmpty())
			{
				for (; ; ) {
					if (available.get() == 0) {
						break roolabel;
					} else {
						TimeUnit.MILLISECONDS.sleep(10);
						continue;
					}
				}
			}
			String shopID = shopIDs.poll(1, TimeUnit.SECONDS);
			if (StringUtil.isEmpty(shopID))
				continue;
			available.incrementAndGet();
			shopDetailServer.submit(() -> {
				try {
					Map<String, Object> datas = new HashMap<>();
					String shopUrl = shopInfoUrl.replace("%shopId%", shopID);
					String shopurl="http://www.dianping.com/shop/"+shopID;
					//获取二级分类
					HttpClient hc1 = null;
					try {
						hc1 = HttpClientBuilder.custom().setUrl(shopurl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
						Map<String,String> requestHeader = new HashMap<String, String>();
						requestHeader.put("Proxy-Switch-Ip","yes");
						hc1.setRequestHeader(requestHeader);
						hc1.setResponseHandle(new AbuyunProxyResponseHandler());
						if(Objects.isNull(hc1)) return;
						Document document=hc1.getDocument();
						//获取店铺第一个评分类型
						Elements elements1=document.getElementsByClass("brief-info");
						Elements elements2=elements1.get(0).getElementsByClass("item");
						String firstscore=elements2.get(2).text();
						if(firstscore.indexOf("口味：")>-1){
							scoretype="test";
						}else if(firstscore.indexOf("机器：")>-1){
							scoretype="machine";
						}else if(firstscore.indexOf("产品：")>-1){
							scoretype="product";
						}else if(firstscore.indexOf("效果：")>-1){
							scoretype="effect";
						}else if(firstscore.indexOf("技师：")>-1){
							scoretype="artificer";
						}else if(firstscore.indexOf("设计：")>-1){
							scoretype="design";
						}else if(firstscore.indexOf("设施：")>-1){
							scoretype="facilities";
						}else if(firstscore.indexOf("装备：")>-1){
							scoretype="equipment";
						}else if(firstscore.indexOf("片源：")>-1){
							scoretype="moviesource";
						}else if(firstscore.indexOf("视效：")>-1){
							scoretype="visual";
						}else if(firstscore.indexOf("项目：")>-1){
							scoretype="program";
						}else {
							scoretype="score1";
						}
						//获取店铺所在区域
						Elements elements=document.getElementsByClass("breadcrumb");
						String regoin=elements.get(0).children().get(1).text();
						datas.put("region",regoin);
						//获取店铺的子分类
						String sub_category=elements.get(0).children().get(2).text();
						datas.put("sub_category",sub_category);
					} catch (MalformedURLException e) {
						logger.info("出错的网页",shopurl);
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (TimeoutException e) {
						e.printStackTrace();
					} catch (HttpClientException e) {
						e.printStackTrace();
					}


					try {
						HttpClient hc2 = null;
						hc2 = HttpClientBuilder.custom().setUrl(shopUrl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
						Map<String,String> requestHeader = new HashMap<String, String>();
						requestHeader.put("Proxy-Switch-Ip","yes");
						hc2.setRequestHeader(requestHeader);
						hc2.setResponseHandle(new AbuyunProxyResponseHandler());
						String shopDetailJson = hc2.doGet();
                        if(StringUtil.isNull(shopDetailJson)) return;
						datas.put("cityCode",cityCode);
						datas.put("category", category);
						datas.put("source", source);
						datas.put("id", ID + shopID);
						datas.put("province",province);
						datas.put("type",type);
						datas.put("city",args.getJobName());
						datas.put("website", shopurl);
						datas.put("updatetime", System.currentTimeMillis());
						JSONObject jsonObject = JSON.parseObject(shopDetailJson);
						JSONObject msg = jsonObject.getJSONObject("msg");
						JSONObject shopInfo = msg.getJSONObject("shopInfo");
						String shopName = shopInfo.getString("shopName");
						datas.put("name", shopName);
						String address = shopInfo.getString("address");
						datas.put("address", address);
						String crossRoad = shopInfo.getString("crossRoad");
						datas.put("crossRoad", crossRoad);
						String phoneNo = shopInfo.getString("phoneNo");
						datas.put("phoneNo", phoneNo);
						String avgPrice = shopInfo.getString("avgPrice");
						datas.put("avgPrice", avgPrice);
						String score = shopInfo.getString("score");
						datas.put("score", score);
						String score1 = shopInfo.getString("score1");
						datas.put(scoretype, score1);
						String score2 = shopInfo.getString("score2");
						datas.put("enviroment", score2);
						String score3 = shopInfo.getString("score3");
						datas.put("service", score3);
						String businessHours = shopInfo.getString("businessHours");
						datas.put("businessHours", businessHours);
						String branchName = shopInfo.getString("branchName");
						datas.put("branchName", branchName);
						String hits = shopInfo.getString("hits");
						datas.put("hits", hits);
						String weeklyHits = shopInfo.getString("weeklyHits");
						datas.put("weeklyHits", weeklyHits);
						String phoneNo2 = shopInfo.getString("phoneNo2");
						datas.put("phoneNo2", phoneNo2);
						String power = shopInfo.getString("power");
						datas.put("power", power);
						String groupFlag = shopInfo.getString("groupFlag");
						datas.put("groupFlag", groupFlag);
						String district = shopInfo.getString("district");
						datas.put("district", district);
						String branchTotal = shopInfo.getString("branchTotal");
						datas.put("branchTotal", branchTotal);
						String glat = shopInfo.getString("glat");
						datas.put("lat",glat);
						String glng = shopInfo.getString("glng");
						datas.put("lng",glng);
						String defaultPic = shopInfo.getString("defaultPic");
						datas.put("pic",defaultPic);
						System.err.println("shopName:"+shopName);
						while (!shopDetails.offer(datas, 2, timeUnit)) ;
					} catch (MalformedURLException e) {
						logger.info("出错的网页",shopUrl);
						e.printStackTrace();
					}catch(Exception e){
						logger.info("出错的网页",shopUrl);
						e.printStackTrace();
					}
				} finally{
					available.decrementAndGet();
				}
			});
		}
	}


	/**
	 * 保存数据
	 */
	 void saveDatas() throws InterruptedException {

		List<Map<String, Object>> mapList = new ArrayList<>();
		for (; ; ) {
			if (shopDetailOver.get() && shopDetails.isEmpty())
				break;
			Map<String, Object> datas = shopDetails.poll(1, timeUnit);
			if (datas == null) continue;
			mapList.add(datas);
			if (mapList.size() > 20) {
				System.out.println("mapList.size()-------------------------------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"+mapList.size());
				for(Map<String,Object> map:mapList){
					System.out.println(map.get("name"));
				}
				saveDatas(mapList);
				mapList.clear();
			}
		}
		if (!mapList.isEmpty()) {
			for(Map<String, Object> map:mapList){
			}
			System.err.println("shopDetail还有数据量："+shopDetails.size());
			saveDatas(mapList);
			mapList.clear();
		}
	}
	/**
	 * 获取这个页面上的某些东西，这里是获取这个页面上的信息条数
	 * @param doc
	 * @return
	 */
	private Integer dataCount(Document doc)
	{
		//取页面某个元素里面的东西
		Elements div_breads = doc.getElementsByAttributeValueContaining("class",
				"J_bread");
		if (StringUtil.isNull(div_breads) || div_breads.isEmpty())
			return null;
		Elements dataNum_emts = div_breads.get(0).getElementsByClass("num");
		if (StringUtil.isNull(dataNum_emts) || dataNum_emts.isEmpty())
			return null;
		return Integer
				.valueOf(dataNum_emts.get(0).text().replaceAll("\\(|\\)", ""));
	}
	/**
	 * 扫描分类链接<br>  得到的是网址的List<String>
	 * Created by ShawnShoper 2016年5月20日
	 *
	 * @param
	 * @param extractType
	 * @return
	 */
	private List<String> scanCategory(Document doc, ExtractType extractType)
	{

		List<String> queue = new ArrayList<>();
		// 从传入的 URL 中读取所需分类的分类链接.

		Elements navigation_divs = doc.getElementsByAttributeValue("class",
				"navigation");
		if (StringUtil.isNull(navigation_divs) || navigation_divs.isEmpty())
			return queue;
		Elements navCategorys_div = navigation_divs.get(0)
				.getElementsByAttributeValueContaining("class", "nav-category");
		if (StringUtil.isNull(navCategorys_div) || navCategorys_div.isEmpty())
			return queue;
		for (Element element : navCategorys_div)
		{
			Elements h4_emts = element.getElementsByTag("h4");
			if (StringUtil.isNull(h4_emts) || h4_emts.isEmpty())
				continue;
			if ("分类:".equals(h4_emts.get(0).text()))
			{
				if (extractType == ExtractType.Category)
				{
					Element classfy_emt = element.getElementById("classfy");
					Elements as_emt = classfy_emt.getElementsByTag("a");
					// 读取各个分类链接
					if (StringUtil.isNull(as_emt) || as_emt.isEmpty())
						continue;
					as_emt.stream().parallel().forEach(a ->
					{
						String url = rootUrl + a.attr("href");
						queue.add(url);
					});
				} else if (extractType == ExtractType.SubCategory)
				{
					Element classfy_emt = element.getElementById("classfy-sub");
					if (classfy_emt == null)
						continue;
					Elements as_emt = classfy_emt.getElementsByTag("a");
					as_emt.remove(0);
					// 读取各个分类链接
					if (StringUtil.isNull(as_emt) || as_emt.isEmpty())
						continue;
					as_emt.stream().parallel().forEach(a ->
					{
						String url = rootUrl + a.attr("href");
						queue.add(url);
					});
				}
			}

		}
		return queue;

	}


	@Override
	protected JobResult call() throws SystemException, InterruptedException
	{
		CountDownLatch cdl = new CountDownLatch(3);

		shopList = new Thread(() ->
		{
			try
			{
				scanList();
			} catch (Exception e)
			{
				e.printStackTrace();
			} finally
			{
				this.scanList.set(true);
				cdl.countDown();
			}
		});
		shopDetals = new Thread(() ->
		{
			try
			{
				showDeltails();
			} catch (Exception e)
			{
				e.printStackTrace();
			} finally
			{
				shopDetailOver.set(true);
				cdl.countDown();
			}
		});
		saveDatas=new Thread(()->
		{
			try {
				saveDatas();
			} catch (InterruptedException e) {

			}finally {
				cdl.countDown();
			}
		}
		);
		shopList.start();
		shopDetals.start();
		saveDatas.start();
		cdl.await();
		return result;
	}

	/**
	 * 自定义初始化参数的方法 所有要初始化的参数都可以写在这个方法里面
	 * @param args
	 * @return
	 * @throws SystemException
	 */
	@Override
	protected JobParam customInit(JobParam args) throws SystemException
	{
		searchUrl = searchUrl.replace("%cityCode%", args.getJobCode())
				.replace("%categoryCode%", args.getCategory());
		type = args.getCategory_name().equals("美食")?"food":args.getCategory_name().equals("购物")?"shopping":"entertainment";
		province=args.getType();
		cityCode=args.getJobCode();
		Symbol=args.getQueryURL();
		return args;
	}
	@Override
	protected boolean checkArgs(JobParam args)
	{
		if (StringUtil.isAnyEmpty(args.getJobCode(), args.getJobName(),
				args.getCategory(), args.getCategory_name() ,args.getType()))
			return false;
		return true;
	}

	@Override
	public void destroy()
	{
		shopDetailServer.shutdownNow();
		if (Objects.nonNull(shopList)&&shopList.isAlive())
			shopList.interrupt();
		if (Objects.nonNull(shopDetals)&&shopDetals.isAlive())
			shopDetals.interrupt();
		if (Objects.nonNull(saveDatas)&&saveDatas.isAlive())
			saveDatas.interrupt();
		shopIDs.clear();
		shopIDs = null;
		shopDetails.clear();
		shopDetails = null;
	}
//	public static void main(String[] args)
//			throws SystemException, InterruptedException
//	{
//
////		//代理IP设置
//		try {
//			ProxyServerPool.importProxyServer(new File("D:/jungle/bigData/proxyip.ls"), Charset.forName("utf-8"));
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
//		JobCaller jobCaller = new DZDianping();
//		JobParam jobParam = new JobParam();
//		jobParam.setJobCode("213");
//		jobParam.setJobName("惠州");
//		jobParam.setCategory("30");
//		jobParam.setCategory_name("娱乐");
//		jobParam.setType("广东");
//		jobParam.setQueryURL("");
//		jobCaller.init(jobParam);
//		jobCaller.run();
//		jobCaller.destroy();
//	}
}
