package org.shoper.taskScript.script.HDFSImport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
/**
 * 新浪用户列表抓取
 * Created by jungle on 2016-9-29.
 */
public class Sina_weibo_accountList_HDFS extends JobCaller {
    //每一页都有3个小页定义page为页数 第一小页是%prepage%=page-1 %page%=page；第二小页和第三小页都是 %prepage%=page %page%=page 第三小页的网址有点不一样多了一个pagebar=1的参数
    private  int page;
    private String page1="http://d.weibo.com/p/aj/v6/mblog/mbloglist?domain=102803_ctg1_2588_-_ctg1_2588&pre_page=%prepage%&page=%page%&pl_name=Pl_Core_NewMixFeed__5&id=102803_ctg1_2588_-_ctg1_2588&script_uri=/102803_ctg1_2588_-_ctg1_2588&feed_type=1";
    private String page2="http://d.weibo.com/p/aj/v6/mblog/mbloglist?domain=102803_ctg1_2588_-_ctg1_2588&pagebar=1&pre_page=%prepage%&page=%page%&pl_name=Pl_Core_NewMixFeed__5&id=102803_ctg1_2588_-_ctg1_2588&script_uri=/102803_ctg1_2588_-_ctg1_2588&feed_type=1";
    private AtomicBoolean scanPersonIdInfo = new AtomicBoolean(false);
    private LinkedBlockingQueue<String> informations = new LinkedBlockingQueue<>(30);
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
        System.setProperty("hadoop.home.dir", "D:\\hadoop");
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs://192.168.100.45:9000");
        try {
            fs = FileSystem.get(conf);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        String time=Integer.toString(Hours)+"-"+Integer.toString(Mins)+"-"+Integer.toString(seconds);
        Path path = new Path(File.separator+"weibo"+File.separator+"sina"+File.separator+"user_list"+File.separator+year+File.separator+month+File.separator+day+ File.separator+time);
        try {
            if (fs.exists(path))
                fs.delete(path, true);
            FSDataOutputStream fsout = fs.create(path, true);
            for (; ; ) {
                if (scanPersonIdInfo.get() && informations.isEmpty())
                    break;
                String data = informations.poll(1, timeUnit);
                if (data == null) continue;
                String infomation=data+"\n";
                fsout.write(infomation.substring(0, infomation.length()).getBytes());
            }
            fsout.flush();
            fsout.close();
            fs.close();
        } catch (IOException e) {
            e.printStackTrace();
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
                            String information1 = ++line + "\001" + data1.replace("\t", "").replace("\n", "").replace("\r", "") ;
                            while (!informations.offer(information1, 2, TimeUnit.SECONDS)) ;
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
                            String information2 = ++line + "\001" + data2.replace("\t", "").replace("\n", "").replace("\r", "");
                            while (!informations.offer(information2, 2, TimeUnit.SECONDS)) ;
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
                            String information3 = ++line + "\001" + data3.replace("\t", "").replace("\n", "").replace("\r", "");
                            while (!informations.offer(information3, 2, TimeUnit.SECONDS)) ;
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

//		//代理IP设置
        try {
            ProxyServerPool.importProxyServer(new File("D:/jungle/bigData/proxyip.ls"), Charset.forName("utf-8"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        JobCaller jobCaller = new Sina_weibo_accountList_HDFS();
        JobParam jobParam = new JobParam();
        jobCaller.init(jobParam);
        jobCaller.run();
        jobCaller.destroy();
    }
}
