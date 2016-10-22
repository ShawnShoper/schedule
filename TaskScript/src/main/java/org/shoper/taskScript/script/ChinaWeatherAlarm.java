package org.shoper.taskScript.script;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.shoper.http.apache.HttpClient;
import org.shoper.http.apache.HttpClientBuilder;
import org.shoper.http.exception.HttpClientException;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.job.JobCaller;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ShawnShoper on 16/9/19.
 */
public class ChinaWeatherAlarm extends JobCaller {
	String alarmURL = "http://product.weather.com.cn/alarm/grepalarm_cn.php?_=1474335780760";
	String contentURL = "http://product.weather.com.cn/alarm/webdata/%file%.html?_=%t%";
	String kindURL = "http://www.weather.com.cn/data/alarminfo/%kind%.html?_=%t%";
	List<Map<String, Object>> alarmInfos = new ArrayList<>();
	List<Map<String, Object>> saveInfos = new ArrayList<>();
	@Override
	protected JobResult call () throws SystemException, InterruptedException {
		fetchWeatherAlarm();
		fetchWeatherAlarmData();
		saveDatas(saveInfos,"china_weather_alarm");
		return result;
	}

	public static void main (String[] args) throws InterruptedException, SystemException {
		JobCaller jobCaller = new ChinaWeatherAlarm();
		jobCaller.run();
		jobCaller.destroy();

	}

	public void fetchWeatherAlarmData () {
		alarmInfos.stream().forEach(map -> {
			String id = String.valueOf(map.get("id"));
			String kind = id.split("-")[2];
			String time = id.split("-")[1];
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			try {
				map.put("time",simpleDateFormat.parse(time).getTime());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			//内容
			{
				try {
					HttpClient httpClient = HttpClientBuilder.custom().setUrl(contentURL.replace("%file%", id).replace("%t%", System.currentTimeMillis() + "")).setCharset("utf-8").build();
					String content_js = httpClient.doGet();
					String json = content_js.substring("var alarminfo=".length());
					JSONObject content_json = JSONObject.parseObject(json);
					content_json.forEach((k, v) -> map.put(k, v));
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (TimeoutException e) {
					e.printStackTrace();
				} catch (HttpClientException e) {
					e.printStackTrace();
				}
			}
			{
				try {
					HttpClient httpClient = HttpClientBuilder.custom().setUrl(kindURL.replace("%kind%", kind).replace("%t%", System.currentTimeMillis() + "")).setCharset("utf-8").build();
					String content_js = httpClient.doGet();
					content_js = content_js.substring("var alarmfyzn=".length());
					JSONArray json = JSONObject.parseArray(content_js);
					map.put("defense_guide", json.get(2));
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (HttpClientException e) {
				} catch (TimeoutException e) {
					e.printStackTrace();
				}
			}
			saveInfos.add(map);
		});
	}

	public void fetchWeatherAlarm () {
		try {
			HttpClient httpClient = HttpClientBuilder.custom().setUrl(alarmURL).setCharset("utf-8").build();
			String json = httpClient.doGet().substring("var alarminfo=".length());
			json = json.substring(0, json.length() - 1);
			JSONObject jsonObject = JSONObject.parseObject(json);
			JSONArray datas = jsonObject.getJSONArray("data");
			for (int i = 0; i < datas.size(); i++) {
				Map<String, Object> map = new HashMap<>();
				JSONArray object = datas.getJSONArray(i);
				map.put("name", object.getString(0));
				map.put("website", "http://www.weather.com.cn/alarm/newalarmcontent.shtml?file=" + object.getString(1));
				map.put("id", object.getString(1).substring(0, object.getString(1).indexOf(".")));
				alarmInfos.add(map);
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
	}


	@Override
	protected boolean checkArgs (JobParam args) {
		return true;
	}

	@Override
	public void destroy () {

	}
}
