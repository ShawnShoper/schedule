package org.shoper.taskScript.script;

import org.shoper.commons.StringUtil;
import org.shoper.http.apache.HttpClient;
import org.shoper.http.apache.HttpClientBuilder;
import org.shoper.http.exception.HttpClientException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by ShawnShoper on 16/9/28.
 */
public class BigData_palmyou_crawl_home {
	public static void main (String[] args) throws IOException, InterruptedException, TimeoutException, HttpClientException {
		Map<String, String> heards = new HashMap<>();
		heards.put("Cookie", "JSESSIONID=4F7B698949EBE86145677822FC1A55D3; DWRSESSIONID=78ZBXkgv8atG1HwdjcYQIIriHtl; usertype=0; typename=\"\"; ccode=lxsdyc; remember=true; aliyungf_tc=AQAAAAIZE2GEEAAAi1dVRXL2HKGJmjgD; JSESSIONID=15B8DC43F8762A9BEDA2EB73CC1CD98C; Hm_lvt_1b1f059ea2c708661a7c62b98791ca71=1475028930; Hm_lpvt_1b1f059ea2c708661a7c62b98791ca71=1475028930; Hm_lvt_8fed1432e6c3c3c8f80a8016890c19bf=1474973638; Hm_lpvt_8fed1432e6c3c3c8f80a8016890c19bf=1475028954");
		String url = "http://bigdata.palmyou.com/ntsms/homeGetOverseasData.action";
		{
			Map<String, String> data = new HashMap<>();
			data.put("overseas", "Outbound");
			HttpClient httpClient = HttpClientBuilder.custom().setUrl(url).setFormDatas(data).build();
			httpClient.setRequestHeader(heards);
			FileOutputStream fos = new FileOutputStream("/Users/ShawnShoper/Desktop/palmyou/outbound");
			String html = httpClient.post();
			if (!StringUtil.isEmpty(html))
				fos.write(html.getBytes());
			fos.flush();
			fos.close();
		}
		{
			Map<String, String> data = new HashMap<>();


			data.put("overseas", "Inbound");
			HttpClient httpClient = HttpClientBuilder.custom().setUrl(url).setFormDatas(data).build();
			httpClient.setRequestHeader(heards);
			FileOutputStream fos = new FileOutputStream("/Users/ShawnShoper/Desktop/palmyou/inbound");
			String html = httpClient.post();
			if (!StringUtil.isEmpty(html))
				fos.write(html.getBytes());
			fos.flush();
			fos.close();
		}
		{
			Map<String, String> data = new HashMap<>();
			data.put("overseas", "DomesticRev");
			HttpClient httpClient = HttpClientBuilder.custom().setUrl(url).setFormDatas(data).build();
			httpClient.setRequestHeader(heards);
			FileOutputStream fos = new FileOutputStream("/Users/ShawnShoper/Desktop/palmyou/domesticRev");
			String html = httpClient.post();
			if (!StringUtil.isEmpty(html))
				fos.write(html.getBytes());
			fos.flush();
			fos.close();
		}
	}
}
