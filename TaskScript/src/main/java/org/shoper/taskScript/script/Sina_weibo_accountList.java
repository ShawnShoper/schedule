package org.shoper.taskScript.script;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.hadoop.fs.FileSystem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.omg.CORBA.SystemException;
import org.shoper.commons.StringUtil;
import org.shoper.http.apache.HttpClient;
import org.shoper.http.apache.HttpClientBuilder;
import org.shoper.http.apache.proxy.ProxyServerPool;
import org.shoper.http.exception.HttpClientException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.job.JobCaller;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * 新浪用户列表抓取
 * Created by jungle on 2016-9-29.
 */
public class Sina_weibo_accountList extends JobCaller {
    //每一页都有3个小页定义page为页数 第一小页是%prepage%=page-1 %page%=page；第二小页和第三小页都是 %prepage%=page %page%=page 第三小页的网址有点不一样多了一个pagebar=1的参数
    private  int page;
    private String page1="http://d.weibo.com/p/aj/v6/mblog/mbloglist?domain=102803_ctg1_2588_-_ctg1_2588&pre_page=%prepage%&page=%page%&pl_name=Pl_Core_NewMixFeed__5&id=102803_ctg1_2588_-_ctg1_2588&script_uri=/102803_ctg1_2588_-_ctg1_2588&feed_type=1";
    private String page2="http://d.weibo.com/p/aj/v6/mblog/mbloglist?domain=102803_ctg1_2588_-_ctg1_2588&pagebar=1&pre_page=%prepage%&page=%page%&pl_name=Pl_Core_NewMixFeed__5&id=102803_ctg1_2588_-_ctg1_2588&script_uri=/102803_ctg1_2588_-_ctg1_2588&feed_type=1";
    private AtomicBoolean scanPersonIdInfo = new AtomicBoolean(false);
    private LinkedBlockingQueue<Map<String,Object>> informations = new LinkedBlockingQueue<>(30);
    private Thread getPersonIdInfo ;
    private Thread saveIdData;
    private String cookies;
    private int  year;
    private int month;
    private int day;
    private int Hours;
    private int Mins;
    private int seconds;
    private FileSystem fs = null;
    private int timeout = 20;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private int retry = 3;
    private String charset = "UTF-8";
    @Override
    protected JobResult call() throws SystemException, InterruptedException {
        CountDownLatch cdl = new CountDownLatch(2);

        getPersonIdInfo=new Thread(()->{
            try{
                getPersonIdInfo();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                scanPersonIdInfo.set(true);
                cdl.countDown();
            }
        });

        saveIdData=new Thread(()->{
            try {
                saveIdData();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                cdl.countDown();
            }
        });

        getPersonIdInfo.start();
        saveIdData.start();
        cdl.await();
        return result;
    }

    private void saveIdData() throws InterruptedException {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (; ; ) {
            if (scanPersonIdInfo.get() && informations.isEmpty())
                break;
            Map<String, Object> datas = informations.poll(1, timeUnit);
            if (datas == null) continue;
            mapList.add(datas);
            if (mapList.size() > 20) {
                System.out.println("mapList.size()-------------------------------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"+mapList.size());
                for(Map<String,Object> map:mapList){
                    System.out.println(map.get("name"));
                }
                saveDatas(mapList,"jd");
                mapList.clear();
            }
        }
        if (!mapList.isEmpty()) {
            for(Map<String, Object> map:mapList){
            }
            saveDatas(mapList,"jd");
            mapList.clear();
        }
    }

    /**
     * 开始循环找出信息
     * @param document
     * @param url
     * @throws InterruptedException
     */
    void getInformation(Document document,String url) throws InterruptedException {
        //開始循環找出 信息
        Elements WB_detail=document.getElementsByClass("WB_detail");
        for(Element element:WB_detail){
            Map<String,Object> data=new HashMap<String,Object>();
            data.put("webSite",url);
            //用戶名
            Elements S_txt1=element.getElementsByClass("S_txt1");
            String href=S_txt1.attr("href");
            String id=getPersonId(href);
            data.put("id",id);
            data.put("name",S_txt1.text());
            Elements is=element.getElementsByTag("i");
            for (Element element1:is){
                Elements icon_approve=element1.getElementsByClass("icon_approve");
                String verify=icon_approve.attr("title");
                if("微博个人认证".equals(verify)){
                    data.put("verify","verify_personal");
                    data.put("verify_name",verify);
                }else{
                    data.put("verify","verify_none");
                    data.put("verify_name",verify);
                }
            }
            Elements as=element.getElementsByTag("a");
            vip:
            for(Element a:as){
                String vip=a.attr("title");
                System.out.println("vip:"+vip);
                if("微博会员".equals(vip)){
                    data.put("vip","true");
                    break vip;
                }else{
                    data.put("vip","false");
                }
            }
            System.out.println(data.get("vip") +","+ data.get("id") +"," + data.get("verify") +"," + data.get("verify_name"));
            while (!informations.offer(data, 2, TimeUnit.SECONDS)) ;
        }
    }

    private void getPersonIdInfo() {
        int line=0;
        for(;;){
            String url1 = page1.replace("%prepage%",  Integer.toString(page-1)).replace("%page%",Integer.toString(page));
            //String url1="http://d.weibo.com/p/aj/v6/mblog/mbloglist?domain=102803_ctg1_2588_-_ctg1_2588&pre_page=1&page=1&pl_name=Pl_Core_NewMixFeed__5&id=102803_ctg1_2588_-_ctg1_2588&script_uri=/102803_ctg1_2588_-_ctg1_2588&feed_type=1";
            String url2 = page1.replace("%prepage%",  Integer.toString(page)).replace("%page%",Integer.toString(page));
            String url3= page2.replace("%prepage%",  Integer.toString(page)).replace("%page%",Integer.toString(page));
            //获取第一小页
            try {
                HttpClient hc1 = null;
                hc1 = HttpClientBuilder.custom().setUrl(url1).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
                Map<String,String> requestHeader1 = new HashMap<String, String>();
                requestHeader1.put("Proxy-Switch-Ip","yes");
                hc1.setRequestHeader(requestHeader1);
                hc1.setCookies(cookies);
                String json1=hc1.doGet();
                if (!Objects.isNull(json1)) {
                    JSONObject jsonObject1 = JSON.parseObject(json1);
                    if(!Objects.isNull(jsonObject1)) {
                        String data1 = jsonObject1.getString("data");
                        if (!StringUtil.isNull(data1)) {
                            Document document1 = Jsoup.parse(data1);
                            Elements elements = document1.getElementsByClass("WB_info");
                            String e = elements.html();
                            //这里判断第一个小页是否为空如果为空 分页就结束跳出循环
                            if (StringUtil.isEmpty(e)) {
                                System.out.println("分页查询结束了");
                                return;
                            }
                            //開始循環找出 信息
                            getInformation( document1,url1);
                        }
                    }
                }
                //获取第二小页
                HttpClient hc2 = null;
                hc2 = HttpClientBuilder.custom().setUrl(url2).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
                Map<String,String> requestHeader2 = new HashMap<String, String>();
                requestHeader2.put("Proxy-Switch-Ip","yes");
                hc2.setRequestHeader(requestHeader2);
                hc2.setCookies(cookies);
                String json2=hc2.doGet();
                if (!Objects.isNull(json2)) {
                    JSONObject jsonObject2 = JSON.parseObject(json2);
                    if(!Objects.isNull(jsonObject2)) {
                        String data2 = jsonObject2.getString("data");
                        if (!StringUtil.isNull(data2)) {
                            Document document2 = Jsoup.parse(data2);
                            Elements elements = document2.getElementsByClass("WB_info");
                            String e = elements.html();
                            //这里判断第一个小页是否为空如果为空 分页就结束跳出循环
                            if (StringUtil.isEmpty(e)) {
                                System.out.println("分页查询结束了");
                                return;
                            }
                            //開始循環找出 信息
                            getInformation( document2,url2);
                        }
                    }
                }
                //获取第三小页
                HttpClient hc3 = null;
                hc3 = HttpClientBuilder.custom().setUrl(url3).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
                Map<String,String> requestHeader3 = new HashMap<String, String>();
                requestHeader3.put("Proxy-Switch-Ip","yes");
                hc3.setRequestHeader(requestHeader3);
                hc3.setCookies(cookies);
                String json3=hc3.doGet();
                if (!Objects.isNull(json3)) {
                    JSONObject jsonObject3 = JSON.parseObject(json3);
                    if (!Objects.isNull(jsonObject3)) {
                        String data3 = jsonObject3.getString("data");
                        if(!StringUtil.isEmpty(data3)) {
                            Document document3 = Jsoup.parse(data3);
                            Elements elements = document3.getElementsByClass("WB_info");
                            String e = elements.html();
                            //这里判断小页是否为空如果为空 分页就结束跳出循环
                            if (StringUtil.isEmpty(e)) {
                                System.out.println("分页查询结束了");
                                return;
                            }
                            //開始循環找出 信息
                            getInformation( document3,url3);
                        }
                    }
                }
                page++;
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

    }


    /**
     * 获取用户的ID
     * @param href
     * @return
     * @throws InterruptedException
     */
    String getPersonId(String href) throws InterruptedException {
        String id=null;
        HttpClient hc = null;
        try {
            hc = HttpClientBuilder.custom().setUrl(href).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
            Map<String,String> requestHeader = new HashMap<String, String>();
            requestHeader.put("Proxy-Switch-Ip","yes");
            hc.setRequestHeader(requestHeader);
            hc.setCookies(cookies);
            Document document=hc.getDocument();
            Elements elements=document.getElementsByTag("script");
            for(Element e:elements) {
                String pice = e.html();
                String json = pice.substring(pice.indexOf("{"), pice.lastIndexOf(")"));
                if (json.startsWith("{\"ns\":")) {
                    JSONObject jsonObject = JSON.parseObject(json);
                    String html = jsonObject.getString("html");
                    if (StringUtil.isEmpty(html)) {
                        continue;
                    }
                    Document doc = Jsoup.parse(html);
                    Elements tb_tab = doc.getElementsByClass("tb_tab");
                    for (Element element : tb_tab) {
                        Elements as = element.getElementsByTag("a");
                        for (Element element1 : as) {
                            if ("他的主页".equals(element1.text())) {
                                String href1 = element1.attr("href");
                                id = href1.substring(href1.indexOf("/p/")+3, href1.lastIndexOf("/"));
                                break;
                          }
                        }
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (HttpClientException e) {
            e.printStackTrace();
        }
        System.out.println(id);
        return id;
    }


    protected JobParam customInit(JobParam args) throws SystemException
    {
        Calendar calendar = Calendar.getInstance();
        Hours=calendar.get(Calendar.HOUR_OF_DAY);
        Mins=calendar.get(Calendar.MINUTE);
        seconds=calendar.get(Calendar.SECOND);
        LocalDate now = LocalDate.now();
        page=1;
        year=now.getYear();
        month=now.getMonthValue();
        day=now.getDayOfMonth();
        cookies=args.getCookies();
        return args;
    }

    @Override
    protected boolean checkArgs(JobParam args) {
        if (StringUtil.isAnyEmpty(args.getCookies())){
            return false;
        }
        return true;
    }

    @Override
    public void destroy() {

    }
    public static void main(String[] args)
            throws SystemException, InterruptedException
    {

		//代理IP设置
      try {
          ProxyServerPool.importProxyServer(new File("D:/jungle/bigData/proxyip.ls"), Charset.forName("utf-8"));
    } catch (FileNotFoundException e) {
          e.printStackTrace();
     }
//      JobCaller jobCaller = new Sina_weibo_accountList();
//      JobParam jobParam = new JobParam();
//        jobParam.setCookies("SINAGLOBAL=6163946574721.188.1474265446026; UOR=,,login.sina.com.cn; SSOLoginState=1475976596; wvr=6; YF-Page-G0=416186e6974c7d5349e42861f3303251; _s_tentry=weibo.com; Apache=2206053712550.7974.1475976636785; ULV=1475976636866:6:1:1:2206053712550.7974.1475976636785:1475200820191; SCF=AoHagnt3Q8B9d3Oy_H1p7H0J17M9QKlUcaglAO8yZTKoaAhic0RpL48iqQTKaGaq5QbuVs5P4IRSt7qlFn5Uulo.; SUB=_2A256-ohgDeTxGeRI7FIZ8SbLzDmIHXVWcf6orDV8PUNbmtBeLUb8kW8OjbzDfdZnmWklfC_zceuAM0rDtA..; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9Whv2V.YCEEhj28jTbhayblL5JpX5KMhUgL.FozcS05ReKnNS0-2dJLoIERLxKqL1h.L12zLxKqL1-eLB.2LxKML1KBLBKnLxKqL1hnLBoMESoM71h2RS0Mf; SUHB=0U14LReYgOw6ae; ALF=1507863471");
//      jobCaller.init(jobParam);
//      jobCaller.run();
//      jobCaller.destroy();
        String url="http://weibo.com/jianghaisama?refer_flag=1028035010_";
        Sina_weibo_accountList sw=new Sina_weibo_accountList();
        sw.getPersonId(url);

    }
}

