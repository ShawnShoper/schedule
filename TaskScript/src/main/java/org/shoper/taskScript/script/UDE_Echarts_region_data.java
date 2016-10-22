package org.shoper.taskScript.script;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.shoper.commons.StringUtil;
import org.shoper.http.apache.HttpClient;
import org.shoper.http.apache.HttpClientBuilder;
import org.shoper.http.exception.HttpClientException;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.job.JobCaller;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by ShawnShoper on 16/9/27.
 */
public class UDE_Echarts_region_data extends JobCaller {
	private String[] citys = new String[]{"北京市市辖区", "上海市市辖区", "天津市市辖区", "重庆市市辖区", "香港特别行政区", "澳门特别行政区", "台湾省", "河北省", "山西省", "内蒙古自治区", "辽宁省", "吉林省", "黑龙江省", "江苏省", "浙江省", "安徽省", "福建省", "江西省", "山东省", "河南省", "湖北省", "湖南省", "广东省", "广西壮族自治区", "海南省", "四川省", "贵州省", "云南省", "西藏自治区", "陕西省", "甘肃省", "青海省", "宁夏回族自治区", "新疆维吾尔自治区"};
	private String url = "http://restapi.amap.com/v3/config/district?subdistrict=%sub%&extensions=all&level=city&key=9d4f5c2078ba12cb9d9d09c4e81c95d0&s=rsv3&output=json&keywords=%city%&callback=jsonp_704093_&platform=JS&logversion=2.0&sdkversion=1.3&appname=http%3A%2F%2Fecomfe.github.io%2Fecharts-map-tool%2F&csid=2001BCD7-571A-4B21-9380-00268EF05826";

	public static void main (String[] args) throws InterruptedException, SystemException {
		JobCaller jobCaller = new UDE_Echarts_region_data();
		jobCaller.run();
		jobCaller.destroy();
	}

	@Override
	protected JobResult call () throws SystemException, InterruptedException {
		fetchData();
		return result;
	}

	private Map<String, String> map = new HashMap<>();

	private void fetchData () {
		for (String city : citys) {
			List<Map<String, String>> data = fetch(city);
			data.stream().forEach(m -> {
				try {
					FileOutputStream fos = new FileOutputStream("/Users/ShawnShoper/Desktop/ude/" + m.get("adcode") + ".json");
					fos.write(m.get("content").getBytes());
					fos.flush();
					fos.close();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			});
		}
		try {
			FileOutputStream fos = new FileOutputStream("/Users/ShawnShoper/Desktop/data");

			map.forEach((k, v) -> {
				try {
					fos.write((k+"\t"+v+"\n").getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<Map<String, String>> fetch (String str) {
		List<Map<String, String>> json = new ArrayList<>();
		try {
			HttpClient httpClient = HttpClientBuilder.custom().setUrl(url.replace("%sub%", "1").replace("%city%", StringUtil.urlEncode(str))).build();
			String content = httpClient.doGet();
			content = content.replace("jsonp_704093_(", "");
			content = content.substring(0, content.length() - 1);
			JSONObject jsonObject = JSONObject.parseObject(content);
			JSONArray jsonArray = jsonObject.getJSONArray("districts");
			if (!jsonArray.isEmpty()) {
				JSONObject jsonObject1 = jsonArray.getJSONObject(0);
				HttpClient httpClient1 = HttpClientBuilder.custom().setUrl(url.replace("%sub%", "0").replace("%city%", StringUtil.urlEncode(str))).build();
				Map<String, String> data = new HashMap<>();
				data.put("adcode", jsonObject1.getString("adcode"));
				data.put("content", httpClient1.doGet());
				map.put(jsonObject1.getString("adcode"), jsonObject1.getString("name"));
				json.add(data);
				JSONArray jsonArray1 = jsonObject1.getJSONArray("districts");
				if (!jsonArray.isEmpty()) {
					for (int i = 0; i < jsonArray1.size(); i++) {
						if (!"biz_area".equals(jsonArray1.getJSONObject(i).getString("level"))) {
							json.addAll(fetch(jsonArray1.getJSONObject(i).getString("name")));
						}
					}
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (HttpClientException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		return json;
	}

	@Override
	protected boolean checkArgs (JobParam args) {
		return true;
	}

	@Override
	public void destroy () {

	}
}
