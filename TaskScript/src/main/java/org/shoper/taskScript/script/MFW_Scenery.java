package org.shoper.taskScript.script;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.shoper.commons.StringUtil;
import org.shoper.http.apache.handle.AbuyunProxyResponseHandler;
import org.shoper.http.apache.proxy.ProxyServerPool;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.job.JobCaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.shoper.http.apache.HttpClient;
import org.shoper.http.apache.HttpClientBuilder;
import org.shoper.http.exception.HttpClientException;

/**
 * Created by xp7.82414 on 2016/9/6.
 */
public class MFW_Scenery extends JobCaller {
    private static String url ="http://www.mafengwo.cn";
    private static String ID ="mafengwo_";
    private static String dateurl = "http://www.mafengwo.cn/jd/%cityID%/gonglve.html";
    private static  String jdurl ="http://www.mafengwo.cn/poi/%id%.html";
    private static String charset = "UTF-8";
    private static final int timeout = 20;
    private static TimeUnit timeUnit = TimeUnit.SECONDS;
    private final int retry = 3;
    private AtomicBoolean sceneryIDOver = new AtomicBoolean(false);
    private AtomicBoolean sceneryDetailOver = new AtomicBoolean(false);
    private static LinkedBlockingQueue<String> sceneryIDs = new LinkedBlockingQueue<>(100);
    private LinkedBlockingQueue<Map<String, Object>> sceneryDetails = new LinkedBlockingQueue<>(100);
    Thread sceneryDetals;
    Thread sceneryList;
    Thread saveDatas;
    String cityName;


    /**
     * 得到所有景点的url
     * @throws InterruptedException
     */
    private void getHoteList() throws InterruptedException, TimeoutException, HttpClientException {
        HttpClient hc = null;
        try {
            hc = HttpClientBuilder.custom().setUrl(dateurl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        Map<String,String> requestHeader = new HashMap<String, String>();
        requestHeader.put("Proxy-Switch-Ip","yes");
        hc.setRequestHeader(requestHeader);
        hc.setResponseHandle(new AbuyunProxyResponseHandler());
        Document doc = hc.getDocument();
        if(Objects.isNull(doc)) return;
        Elements jdnames = doc.getElementsByClass("list");
        Elements jdname = jdnames.get(0).getElementsByTag("ul");
        Elements jdne = jdname.get(0).getElementsByTag("a");
        for (Element jdn : jdne) {
            String jd = jdn.attr("href");
            String jda =url+jd;
                while (!sceneryIDs.offer(jda, 2, TimeUnit.SECONDS)) ;
        }
    }

    /**
     * 每个景区的数据
     */
    private final String category ="mafengwo";
    private final String source ="蚂蜂窝";
    private String province;
    AtomicInteger available = new AtomicInteger(0);
    ExecutorService sceneryDetailServer = Executors.newFixedThreadPool(3);
    private void getHotelDetail() throws InterruptedException, IllegalAccessException, HttpClientException {
        rootLabel:
        for (; ; ) {
            if (sceneryIDOver.get() && sceneryIDs.isEmpty()) {
                for (; ; ) {
                    if (available.get() == 0) {
                        break rootLabel;
                    } else {
                        TimeUnit.MILLISECONDS.sleep(10);
                        continue;
                    }
                }
            }
                final String hotel_id = sceneryIDs.poll(1, TimeUnit.SECONDS);
            if (StringUtil.isEmpty(hotel_id))
                continue;
            available.incrementAndGet();
            sceneryDetailServer.submit(() -> {
                try{
                    HttpClient hc = null;
                    try {
                        hc = HttpClientBuilder.custom().setUrl(hotel_id).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    Map<String,String> requestHeader = new HashMap<String, String>();
                    requestHeader.put("Proxy-Switch-Ip","yes");
                    hc.setRequestHeader(requestHeader);
                    hc.setResponseHandle(new AbuyunProxyResponseHandler());
                    Document dc = null;
                    try {
                        dc = hc.getDocument();
                    } catch (HttpClientException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    }
                    Map<String, Object> data = new HashMap<String, Object>();
                    data.put("id", ID );
                    data.put("category",category);
                    data.put("website",hotel_id);
            data.put("name", "");
            data.put("lat", "");
            data.put("lng", "");
            data.put("address", "");
            data.put("spot", "");
            data.put("ticket", "");
            data.put("Traffic", "");
            data.put("Reservations", "");
            data.put("tel", "");
            data.put("hours", "");
                    data.put("updatetime", System.currentTimeMillis());
                    data.put("city",cityName);
                    data.put("source",source);
                    data.put("province",province);
                    //获取景区名字
            Elements names = dc.getElementsByClass("s-title");
            for (Element emt : names) {
                Elements name = emt.getElementsByTag("h1");
                String meant = name.text();
                data.put("name", meant);

            }

        //获取景区的经纬度
        {
            Elements e = dc.getElementsByTag("script").eq(0);
            Element em = null;
            for(Element emt : e){
                if(emt.hasAttr("type")){
                    em=emt;
                }
            }
            {
                Pattern pattern = Pattern.compile("lat\":(\\d+\\.\\d+)");
                Matcher matcher = pattern.matcher(em.html());
                if (matcher.find())
                    data.put("lat",matcher.group(1));

            }
            {
                Pattern pattern = Pattern.compile("lng\":(\\d+\\.\\d+)");
                Matcher matcher = pattern.matcher(em.html());
                if (matcher.find())
                    data.put("lng",matcher.group(1));

            }
        }

//景点地址

            Elements wrapper = dc.select(".wrapper[data-cs-p=景点位置]");
            for (Element element : wrapper) {
                data.put("address", element.select(".r-title div").text().trim());

            }

        //获取景区简介

            Elements elements = dc.getElementsByClass("intro");
            for (Element intro : elements) {
                Elements name = intro.getElementsByTag("p").eq(0);
                String na = name.text();
                data.put("spot", na);

            }
                    {
                        Elements means = dc.getElementsByClass("intro");
                        Elements mean = means.get(0).getElementsByTag("dd");
                        for(Element mea : mean){
                            String met =mea.getElementsByClass("label").text();
                            if(met.equals("电话")){
                                data.put("tel",mea.getElementsByTag("p").text());
                            }else if(met.equals("交通")){
                                data.put("Traffic",mea.getElementsByTag("span").get(1).text());
                            }else if(met.equals("门票")){
                                data.put("ticket",mea.getElementsByTag("span").get(1).text());
                            }else if(met.equals("开放时间")){
                                data.put("hours",mea.getElementsByTag("span").get(1).text());
                            }
                        }
                    }



                    try {

                            while (!sceneryDetails.offer(data, 2, timeUnit)) ;

                } catch (InterruptedException e) {
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
    public void saveDatas() throws InterruptedException {

        String category_name=args.getCategory_name();
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (; ; ) {
            if (sceneryDetailOver.get() && sceneryDetails.isEmpty()) {
                break;
            }
                Map<String, Object> datas = sceneryDetails.poll(1, timeUnit);

            if (datas == null) continue;
            mapList.add(datas);
            if (mapList.size() > 10) {
                saveDatas(mapList);
                mapList.clear();
            }
        }
        if (!mapList.isEmpty()) {
            saveDatas(mapList);
            mapList.clear();
        }


    }

    @Override
    protected JobResult call() throws SystemException, InterruptedException {
        CountDownLatch cdl = new CountDownLatch(3);

        sceneryList = new Thread(() ->
        {
            try
            {
                getHoteList();
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally
            {
                this.sceneryIDOver.set(true);
                cdl.countDown();
            }
        });
        sceneryDetals = new Thread(() ->
        {
            try
            {
                getHotelDetail();
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally
            {
                sceneryDetailOver.set(true);
                cdl.countDown();
            }
        });
        saveDatas=new Thread(()->
        {
            try {
                saveDatas();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {

                cdl.countDown();
            }
        }
        );
        sceneryList.start();
        sceneryDetals.start();
        saveDatas.start();
        cdl.await();
        return result;
    }
    protected JobParam customInit(JobParam args) throws SystemException
    {
        dateurl = dateurl.replace("%cityID%", args.getJobCode());
        cityName=args.getJobName();
        province=args.getType();
        return args;
    }
    @Override
    protected boolean checkArgs(JobParam args) {
        if (StringUtil.isAnyEmpty(args.getJobCode(), args.getJobName()))
            return false;
        return true;
    }

    @Override
    public void destroy() {
        sceneryDetailServer.shutdownNow();
        if (Objects.nonNull(sceneryList)&&sceneryList.isAlive())
            sceneryList.interrupt();
        if (Objects.nonNull(sceneryDetals)&&sceneryDetals.isAlive())
            sceneryDetals.interrupt();
        if (Objects.nonNull(saveDatas)&&saveDatas.isAlive())
            saveDatas.interrupt();
        sceneryIDs.clear();
        sceneryIDs = null;
        sceneryDetails.clear();
        sceneryDetails = null;
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
        JobCaller jobCaller = new MFW_Scenery();
        JobParam jobParam = new JobParam();
        jobParam.setJobCode("10442");
        jobParam.setJobName("拉萨");

        jobParam.setType("西藏");
        jobCaller.init(jobParam);
        jobCaller.run();
        jobCaller.destroy();
    }
}





