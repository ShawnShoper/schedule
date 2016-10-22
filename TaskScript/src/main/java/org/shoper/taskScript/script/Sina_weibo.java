package org.shoper.taskScript.script;

import com.alibaba.fastjson.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.shoper.http.apache.HttpClient;
import org.shoper.http.apache.HttpClientBuilder;
import org.shoper.http.exception.HttpClientException;
import org.shoper.http.apache.handle.AbuyunProxyResponseHandler;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.prop.Category;
import org.shoper.schedule.provider.job.JobCaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sina 微博..<br>
 * http://www.weibo.com/shanghaicity<br>
 * http://weibo.com/p/1001061989772524<br>
 * http://weibo.com/u/1824363762<br>
 * http://weibo.com/339141999<br>
 * <p>
 * 4种形式的微博.<br>
 * 参数细分为 http://rootURL/type/jobID<br>
 *
 * @author ShawnShoper
 */
public class Sina_weibo extends JobCaller {

	private final String ROOTURL = "http://weibo.com/";
	private AtomicBoolean fetchDone = new AtomicBoolean(false);
	private AtomicBoolean parseDone = new AtomicBoolean(false);
	private AtomicBoolean saveDone = new AtomicBoolean(false);
	private String charset = "UTF-8";
	private Config config;
	private int timeout = 20;
	private TimeUnit timeoutUnit = TimeUnit.SECONDS;
	private int retry = 3;
	private volatile boolean incrementalCrawling = false;
	private final long limitTime = new Date().getTime();
	private volatile boolean overFetch = false;
	LinkedBlockingQueue<Document> analyzeQueue = new LinkedBlockingQueue<Document>(
			20);
	LinkedBlockingQueue<Map<String, Object>> saveQueue = new LinkedBlockingQueue<Map<String, Object>>(
			20);
	/**
	 * 生成40个固定的线程池..
	 */
	ExecutorService service = Executors.newFixedThreadPool(5);

	@Override
	public JobResult call () {
		initModel();
		// Do parse weibo...
		try {
			doParse();
		} catch (InterruptedException e) {
		}
		return result;
	}

	@Autowired
	MongoTemplate mongoTemplate;

	/**
	 * 判断本次抓取是增量还是全量
	 */

	private void initModel () {
		// TODO Auto-generated method stub
		long count = mongoTemplate
				.count(
						Query.query(
								Criteria.where("category").is("Weibo").and("fid").is(getArgs().getJobCode())),
						"datas"
				);
		if (count != 0)
			incrementalCrawling = true;
		if(incrementalCrawling)
			logger.info(getArgs().getJobCode()+"存在数据增量抓取..");
		else
			logger.info(getArgs().getJobCode()+"不存在数据全量抓取..");
	}

	private void doParse () throws InterruptedException {
		CountDownLatch cdl = new CountDownLatch(3);
		service.submit(() -> {
			try {
				fetchPage();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				fetchDone.set(true);
				cdl.countDown();
			}
		});
		service.submit(() -> {
			try {
				parsePage();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				parseDone.set(true);
				cdl.countDown();
			}
		});

		service.submit(() -> {
			try {
				saveData();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				saveDone.set(true);
				cdl.countDown();
			}
		});

		try {
			cdl.await();
			result.setSuccess(true);
		} catch (InterruptedException e) {
			result.setSuccess(false);
		} finally {
			result.setDone(true);
		}
		logger.info("Task over,update amount:[" + result.getUpdateCount().get()
							+ "],new add amount:[" + result.getSaveCount().get() + "]");
	}

	/**
	 * 保存数据
	 *
	 * @throws InterruptedException
	 */
	protected void saveData () throws InterruptedException {
		List<Map<String, Object>> datas = new ArrayList<>();
		for (; ; ) {
			if (parseDone.get() && saveQueue.isEmpty()) {
				save(datas);
				break;
			} else {
				// 获取saveQueue 队列的数据 进行处理
				Map<String, Object> doc = saveQueue.poll(5, timeoutUnit);
				if (doc == null)
					continue;
				datas.add(doc);
				if (datas.size() > 20)
					save(datas);
			}
		}
	}

	void save (List<Map<String, Object>> datas) {
		if (!datas.isEmpty()) {
			super.saveDatas(datas);
			datas.clear();
		}
	}

	/**
	 * 解析页面
	 *
	 * @throws InterruptedException
	 */
	protected void parsePage () throws InterruptedException {
		for (; ; ) {
			if ((fetchDone.get() && analyzeQueue.isEmpty()) || overFetch)
				break;
			else {
				// 获取analyzeQueue 队列的数据 进行处理
				Document doc = analyzeQueue.poll(1, timeoutUnit);
				if (doc == null)
					continue;
				service.submit(new Runnable() {
					@Override
					public void run () {
						try {
							parseElements(doc);
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		}
	}

	/**
	 * 解析微博 内容 list
	 *
	 * @throws InterruptedException
	 */
	void parseElements (Document doc) throws InterruptedException {
		Elements list = doc.getElementsByAttributeValue(
				"action-type",
				"feed_list_item"
		);
		for (Element feed : list) {
			String id = feed.attr("mid");
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("id", "Weibo_sina_" + id);
			data.put("url", args.getTargetURL());
			data.put("fid", args.getJobCode());
			data.put("category", Category.Weibo.getCode());
			data.put("category_name", Category.Weibo.getName());
			data.put("updatetime", System.currentTimeMillis());
			data.put("author", config.getOnick());
			// 处理 feed_content 节点,包含微博的主要内容数据
			{
				Elements feed_contents = feed.getElementsByAttributeValue(
						"node-type", "feed_content");
				if (feed_contents != null && feed_contents.size() > 0) {
					// WB_content
					Element feed_content = feed_contents.get(0);
					// WB 主体
					Elements wb_details = feed_content
							.getElementsByAttributeValueContaining(
									"class",
									"WB_detail"
							);
					if (wb_details != null && !wb_details.isEmpty()) {
						// 获取到 wb_detail
						Element wb_detail = wb_details.get(0);
						for (Element child : wb_detail.children()) {
							String className = child.attr("class");
							switch (className) {
								// 微博发布时间以及 VIA
								case "WB_from S_txt2":
									// 应该是2个节点,第一个 A 节点获取 date 属性,即可
									// 第二个节点是用户推送此条微博的 via
									Elements emts = child.children();
									if (emts.size() > 0) {
										Element date_emt = emts.get(0);
										String date_str = date_emt.attr("date");
										if (incrementalCrawling) {
											Calendar calendar = Calendar
													.getInstance();
											calendar.setTimeInMillis(limitTime);
											calendar.add(
													Calendar.DAY_OF_MONTH,
													-7
											);
											long limit = calendar
													.getTimeInMillis();
											long date = Long.valueOf(date_str);
											if (date < limit) {
												overFetch = true;
												return;
											}

										}

										data.put(
												"time",
												Long.valueOf(date_str)
										);
									}
									if (emts.size() > 1) {
										Element via_emt = emts.get(1);
										data.put("via", via_emt.text());
									}
									break;
								case "WB_text W_f14":
									// 微博正文
									data.put("content", child.text());
									break;
								case "WB_media_wrap clearfix":
									// 微博图片，视频
									// 获取所有图片地址...
									Elements imgs = child
											.getElementsByTag("img");
									List<String> imgURL = new ArrayList<String>();
									if (imgs != null && !imgs.isEmpty()) {
										for (Element img : imgs) {
											imgURL.add(img.attr("src"));
										}

									}
									data.put("orgin_images", imgURL);
									break;
								case "WB_feed_expand":
									// 这一步是获取转载等的东西..暂时不管...
									break;
								default:
									break;
							}
						}
					}
					{ // 转发,评论,赞
						Elements wb_handle = feed
								.getElementsByClass("WB_handle");
						Element handle_emt = wb_handle.get(0);
						Elements actions_emts = handle_emt
								.getElementsByAttribute("action-type");
						for (Element element : actions_emts) {
							String action_type = element.attr("action-type");
							String forward_count_str = "";
							String comment_count_str = "";
							String like_count_str = "";
							switch (action_type) {
								case "fl_forward":
									forward_count_str = element
											.getElementsByTag("em").get(1)
											.text();
									forward_count_str = forward_count_str
											.replaceAll("转发", "")
											.replace("万", "0000");
									break;
								case "fl_comment":
									comment_count_str = element
											.getElementsByTag("em").get(1)
											.text();
									comment_count_str = comment_count_str
											.replaceAll("评论", "")
											.replace("万", "0000");
									break;
								case "fl_like":
									like_count_str = element
											.getElementsByTag("em").get(0)
											.text().replace("ñ","");
									like_count_str = like_count_str
											.replaceAll("赞", "")
											.replace("万", "0000");
									break;
								default:
									break;
							}
							data.put(
									"forward_count",
									Long.valueOf(forward_count_str.isEmpty()
														 ? "0"
														 : forward_count_str)
							);
							data.put(
									"comment_count",
									Long.valueOf(comment_count_str.isEmpty()
														 ? "0"
														 : comment_count_str)
							);
							data.put(
									"like_count",
									Long.valueOf(like_count_str.isEmpty()
														 ? "0"
														 : like_count_str)
							);
						}
					}
				}
			}
			for (; ; ) {
				result.getHandleCount().incrementAndGet();
				if (saveQueue.offer(data, 5, timeoutUnit))
					break;
			}
		}
	}

	/**
	 * 读取列表页
	 *
	 * @throws InterruptedException
	 */
	protected void fetchPage () throws InterruptedException {
		// 读取首页获取相关参数...

		config = injectConfig();
		if (config == null || config.getDomain() == null) {
			logger.info(
					"Get [config] or [domain] failed....please check cookies or rules");
			return;
		}
		final String query = "http://weibo.com/p/aj/v6/mblog/mbloglist?domain=%domain%&is_all=1&pre_page=%prePage%&page=%page%&pagebar=%pageBar%&id=%id%";
		int page = 1;
		int prePage = 0;
		int pageBar = 0;
		for (; ; ) {
			if (overFetch) return;
			String q = query.replace("%domain%", config.getDomain())
					.replace("%prePage%", prePage + "")
					.replace("%page%", page + "")
					.replace("%pageBar%", pageBar + "")
					.replace("%id%", args.getJobCode());
			logger.info("Access url {}", q);

			try {
				HttpClient hc = null;
				try {
					hc = HttpClientBuilder.custom().setUrl(q).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeoutUnit).setRetry(retry).setCharset(charset).build();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				Map<String,String> requestHeader = new HashMap<String, String>();
				requestHeader.put("Proxy-Switch-Ip","yes");
				hc.setRequestHeader(requestHeader);
				hc.setResponseHandle(new AbuyunProxyResponseHandler());
				hc.setCookies(args.getCookies());
				String json = hc.doGet();
				if (json == null) {
					logger.info("读取数据异常,请检查网络.....");
					break;
				} else {
					Document doc = null;
					for (; ; ) {
						JSONObject jsonObject = JSONObject.parseObject(json);
						String data = jsonObject.getString("data");
						doc = Jsoup.parse(data);
						if (analyzeQueue.offer(doc, timeout, timeoutUnit))
							break;
					}
					// check has more page
					if (!hasMorePage(doc)) {
						logger.info("Docment has no more page,so quit...");
						break;
					}
				}
				// 模拟人类看微博的习惯..一页需要看几秒甚至几分钟
//				TimeUnit.SECONDS.sleep((int) (Math.random() * 1) + 10);
				// 根据 weibo 的参数规则
				// page no pre_page page pagebar
				// 1 0 1 0
				// 1 1 1 0
				// 1 1 1 1
				//
				// 2 0 2 0
				// 2 2 2 0
				// 2 2 2 1
				//
				// 3 0 3 0
				// 3 3 3 0
				// 3 3 3 1
				if (pageBar == 1) {
					page++;
					prePage = 0;
					pageBar = 0;
				} else {
					if (prePage == page)
						pageBar++;
					prePage = page;
				}
			}  catch (HttpClientException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 检查是否有下一页
	 *
	 * @param document
	 * @return
	 */
	boolean hasMorePage (Document document) {
		Elements lazyload_emts = document
				.getElementsByAttributeValue("node-type", "lazyload");
		if (lazyload_emts != null && !lazyload_emts.isEmpty())
			return true;
		Elements page_emts = document.getElementsByAttributeValue(
				"node-type",
				"feed_list_page"
		);
		if (page_emts == null || page_emts.isEmpty())
			return false;
		Elements page_as = page_emts.get(0)
				.getElementsByAttributeValue("bpfilter", "page");
		if (page_as == null || page_as.isEmpty())
			return false;
		if ("下一页".equals(page_as.get(page_as.size() - 1).text())) {
			return true;
		}
		return false;
	}

	/**
	 * WeiBo CONFIG
	 *
	 * @author ShawnShoper
	 */
	class Config {
		private String onick;
		private String domain;

		public String getOnick () {
			return onick;
		}

		public void setOnick (String onick) {
			this.onick = onick;
		}

		public String getDomain () {
			return domain;
		}

		public void setDomain (String domain) {
			this.domain = domain;
		}

	}

	/**
	 * 获取 domain 参数<br>
	 * 通过接口获取微博数据必须的参数..
	 *
	 * @return
	 */
	private Config injectConfig () {
		String domain = null;
		String onick = null;
		String url = ROOTURL + (args.getType() == null
				? ""
				: args.getType() + "/") + args.getJobCode();
		try {
			HttpClient hc = null;
			try {
				hc = HttpClientBuilder.custom().setUrl(url).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeoutUnit).setRetry(retry).setCharset(charset).build();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			Map<String,String> requestHeader = new HashMap<String, String>();
			requestHeader.put("Proxy-Switch-Ip","yes");
			hc.setRequestHeader(requestHeader);
			hc.setResponseHandle(new AbuyunProxyResponseHandler());
			hc.setCookies(args.getCookies());
			Document main = hc.getDocument();
			Elements scripts = main.getElementsByTag("script");
			for (Element script : scripts) {
				// script.html()方法很重，需要缓存数据...
				// 禁止多次调用同一个对象的 html();
				String sc = script.html();
				// 检查第一行是否是var $CONFIG = {};如果第一行是该数据,那么就是profile 配置项
				StringTokenizer stringTokenizer = new StringTokenizer(
						sc,
						"\r\n"
				);
				boolean checked = false;
				while (stringTokenizer.hasMoreTokens()) {
					String token = stringTokenizer.nextToken();
					if (!checked && "var $CONFIG = {};".equals(token)) {
						checked = true;
					} else {
						if (token.startsWith("$CONFIG['domain']="))

							domain = token.substring(
									"$CONFIG['domain']=".length() + 1,
									token.length() - 3
							);
						else if (token.startsWith("$CONFIG['onick']="))
							onick = token.substring(
									"$CONFIG['onick']=".length() + 1,
									token.length() - 3
							);
					}
				}
				if (checked)
					break;
			}
		} catch (HttpClientException e) {
			e.printStackTrace();
		}  catch (InterruptedException e) {
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		Config config = new Config();
		config.setDomain(domain);
		config.setOnick(onick);
		return config;
	}

	@Override
	public void destroy () {
		service.shutdownNow();
	}

	/**
	 * 验证参数<br>
	 * 如果验证失败,直接返回null<br>
	 */
	@Override
	public boolean checkArgs (JobParam args) {
		System.out.println(args);
		if (args.getJobCode() == null || args.getJobName() == null
//				|| args.getCookies() == null
				)
			return false;
		return true;
	}
}
