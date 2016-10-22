package org.shoper.taskScript.script;


import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.shoper.commons.MD5Util;
import org.shoper.commons.StringUtil;
import org.shoper.http.apache.HttpClientBuilder;
import org.shoper.http.apache.handle.AbuyunProxyResponseHandler;
import org.shoper.http.exception.HttpClientException;
import org.shoper.http.httpClient.HttpClient;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.job.JobCaller;

import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 开发人:唐元林
 * 数据来源：途牛酒店评论
 * Created by xp7.82414 on 2016/9/20.
 */
public class Tuniu_Hotelcommons extends JobCaller{
    //    private String hoteUrl = "http://hotel.tuniu.com/detail/597174?";
//    private String pageUrl = "http://hotel.tuniu.com/ajax/remarkQuery?hotelId=597174&p=22";
    private String charset = "UTF-8";
    final int timeout = 20;
    final int retry = 3;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private LinkedBlockingQueue<String> hotelIDs = new LinkedBlockingQueue<>(50);
    private LinkedBlockingQueue<Map<String, Object>> commonsDetails = new LinkedBlockingQueue<>(100);
    AtomicBoolean scanList = new AtomicBoolean(false);
    AtomicBoolean hotelDetailOver=new AtomicBoolean(false);
    Thread getHotelIDs;
    Thread getCommons;
    Thread saveDatas;
    String satisfaction;

    //private String cityUrl="http://hotel.tuniu.com/yii.php?r=/hotel/hotel/list_iframe&city=%cityName%&checkindate=%checkindate%&checkoutdate=%checkoutdate%&page=";
    private String cityUrl="http://hotel.tuniu.com/yii.php?r=/hotel/hotel/list_iframe&city=%cityName%&checkindate=%checkindate%&checkoutdate=%checkoutdate%&page=";

    public static void main(String[] args) {
        Tuniu_Hotelcommons t = new Tuniu_Hotelcommons();
        // t.setHoteUrl();
//        t.setJdUrl();
    }
    //获取城市所有酒店的ID
    void setJdUrl(){
        int page =1;
        for (; ; ) {
            String storeurl =cityUrl+page++;
            try {
                org.shoper.http.apache.HttpClient httpClient = null;
                httpClient = HttpClientBuilder.custom().setUrl(storeurl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
                Map<String,String> requestHeader = new HashMap<String, String>();
                requestHeader.put("Proxy-Switch-Ip","yes");
                httpClient.setRequestHeader(requestHeader);
                httpClient.setResponseHandle(new AbuyunProxyResponseHandler());
                Document dc = httpClient.getDocument();
                if(StringUtil.isNull(dc)) return;
                Elements wrap = dc.getElementsByClass("hotel-list");
                Elements hotel = wrap.get(0).getElementsByClass("nameAndIcon");
                for (Element h2 : hotel) {
                    Elements connects = h2.getElementsByTag("a");
                    String connect = connects.attr("href");
                    while (!hotelIDs.offer(connect.substring(30,connect.length()-47),2,TimeUnit.SECONDS));
                }
            } catch (HttpClientException e) {
                e.printStackTrace();
            }catch(Exception e){

            }
        }
    }

    /**
     * 获取酒店名字
     */
    String hotelnameUrl(String hotelid) throws MalformedURLException, InterruptedException, TimeoutException, HttpClientException {
        String hotelname;
        String monicker ="http://hotel.tuniu.com/detail/%id%".replace("%id%",hotelid);
        org.shoper.http.apache.HttpClient httpClient = null;
        httpClient = HttpClientBuilder.custom().setUrl(monicker).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
        Map<String,String> requestHeader = new HashMap<String, String>();
        requestHeader.put("Proxy-Switch-Ip","yes");
        httpClient.setRequestHeader(requestHeader);
        httpClient.setResponseHandle(new AbuyunProxyResponseHandler());
        Document dc = httpClient.getDocument();
        Elements names =dc.getElementsByClass("nameAndIcon");
        return hotelname =names.get(0).getElementsByAttributeValue("class","name").text();

    }


    private String city;
    private String province;
    private static final String SOURCE = "途牛";
    private static final String CATEGORY="tuniu" ;
    private static final String TYPE="hotel_comment";
    private String hotelname;
    AtomicInteger available = new AtomicInteger(0);
    ExecutorService hotelDetailServer = Executors.newFixedThreadPool(3);
    //获取所有酒店评论
    void setHoteUrl() throws InterruptedException {
        roolabel:
        for (; ; ) {
            if (scanList.get() && hotelIDs.isEmpty()) {
                for (; ; ) {
                    if (available.get() == 0) {
                        break roolabel;
                    } else {
                        TimeUnit.MILLISECONDS.sleep(10);
                        continue;
                    }
                }
            }
            String hotelID = hotelIDs.poll(1, TimeUnit.SECONDS);
            if (StringUtil.isEmpty(hotelID))
                continue;
            available.incrementAndGet();
            hotelDetailServer.submit(() -> {
                try {
                    int page = 0;
                    for (; ; ) {
                        page++;
                        Map<String,Object> datas=new HashMap<String,Object>();
                        datas.put("use", "");
                        datas.put("Traffic_location", "");
                        datas.put("environmental_health", "");
                        datas.put("quality_of_service", "");
                        datas.put("evaluates", "");
                        datas.put("via", "");
                        datas.put("check", "");
                        datas.put("hotelID",hotelID);
                        datas.put("source", SOURCE);
                        datas.put("category", CATEGORY);
                        datas.put("type", TYPE);
                        datas.put("city", city);
                        datas.put("province",province);
                        datas.put("hotelname","");
                        //取酒店名字
                        hotelname=hotelnameUrl(hotelID);
                        datas.put("hotelname",hotelname);
                        String pageUrl = "http://hotel.tuniu.com/ajax/remarkQuery?hotelId=%id%&p=".replace("%id%",hotelID) + page;
                        datas.put("website",pageUrl);
                        try{
                            org.shoper.http.apache.HttpClient httpClient = null;        //修改
                            httpClient = HttpClientBuilder.custom().setUrl(pageUrl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
                            Map<String,String> requestHeader = new HashMap<String, String>();
                            requestHeader.put("Proxy-Switch-Ip","yes");
                            httpClient.setRequestHeader(requestHeader);
                            httpClient.setResponseHandle(new AbuyunProxyResponseHandler());
                            Document dc = httpClient.getDocument();
                            String divs =dc.getElementsByAttributeValue("style","margin: 20px 0 0 112px;font-size:14px").text();
                            if(divs.equals("暂无点评。")){
                                System.err.println("当前循环结束");
                                return;
                            }
                            System.out.println(hotelID+"的当前页数是"+page);
                            Elements u5 = dc.getElementsByClass("u5");
                            for (Element usts : u5){
                                if (usts!=null){
                                    Elements names = usts.getElementsByClass("a1");
                                    String name = names.get(0).getElementsByClass("b2").text();
                                    String use = names.get(0).getElementsByClass("b3").text();
                                    datas.put("username",name);
                                    datas.put("use",use);
                                    //评论来自
                                    //位置评价
                                    Elements satisfactiont = usts.getElementsByClass("a2");
                                    for (Element satis :satisfactiont){
                                        Elements sati =satis.getElementsByClass("b2");
                                        if (sati!=null){
                                            Elements Traffic_locations =sati.get(0).getElementsByTag("p");
                                            //交通
                                            String Traffic_location =Traffic_locations.get(0).getElementsByClass("remark_words_title").eq(0).text();
                                            if (Traffic_location.isEmpty()){

                                            }else {
                                                String[] aa = Traffic_location.split(":");
                                                for (int i = 0; i < aa.length; i++) {
                                                    datas.put("Traffic_location",aa[i].substring(5));
//                                    System.out.println(aa[i].substring(5));
                                                }
                                            }
                                            //环境
                                            String environmental_health =Traffic_locations.get(0).getElementsByClass("remark_words_title").eq(1).text();
                                            if (environmental_health.isEmpty()){

                                            }else {
                                                String[] aa = environmental_health.split(":");
                                                for (int i = 0; i < aa.length; i++) {
                                                    datas.put("environmental_health",aa[i].substring(5));
                                                }
                                            }
                                            //设施
                                            String facilities =Traffic_locations.get(0).getElementsByClass("remark_words_title").eq(2).text();
                                            if (facilities.isEmpty()){

                                            }else {
                                                String[] aa = facilities.split(":");
                                                for (int i = 0; i < aa.length; i++) {
                                                    datas.put("facilities",aa[i].substring(5));
                                                }
                                            }
                                            //服务质量
                                            String quality_of_service =Traffic_locations.get(0).getElementsByClass("remark_words_title").eq(3).text();
                                            if (quality_of_service.isEmpty()){

                                            }else {
                                                String[] aa = quality_of_service.split(":");
                                                for (int i = 0; i < aa.length; i++) {
                                                    datas.put("quality_of_service",aa[i].substring(5));
                                                }
                                            }
                                            //评论
                                            if(satisfactiont.size()==2){
                                                datas.put("evaluates",satisfactiont.get(1).text());
                                            }else if(satisfactiont.size()<2){
                                                String satisfaction =satisfactiont.text();
                                                datas.put("evaluates",satisfaction);
                                            }
                                        }
                                        Elements b4 = usts.getElementsByClass("b4");
                                        String div = b4.get(0).getElementsByAttributeValue("style", "float: left;color: #43b313;").text();
                                        datas.put("via", div);
                                        //入住时间
                                        String checks = b4.get(0).getElementsByAttributeValue("style", "float: right;").text();
                                        if (checks!=null);
                                        String checkt =checks.substring(0,checks.indexOf("入"));
                                        SimpleDateFormat sdf =new SimpleDateFormat("yyyy年MM月dd日");
                                        Date checkk =sdf.parse(checkt);
                                        long check=checkk.getTime();
                                        datas.put("checkin", check);
                                        //生成唯一的标识
                                        if (!(StringUtil.isNull(hotelID)&&StringUtil.isNull(hotelname)&&StringUtil.isNull(use)&&StringUtil.isNull(checks)&&StringUtil.isNull(satisfaction))) {
                                            String token = MD5Util.GetMD5Code(hotelIDs+name+use+check+div+satisfaction);
                                            datas.put("naid", /*"tuniu_"+*/ token);
                                            System.out.println("存进去的id是"+datas.get("naid"));
                                            while (!commonsDetails.offer(datas, 2, TimeUnit.SECONDS)) ;
                                        }
                                    }
                                }
                            }
                        } catch (HttpClientException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (HttpClientException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                } finally {
                    available.decrementAndGet();
                }
            });
        }
    }
    void saveDatas() throws InterruptedException {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (; ; ) {
            if (hotelDetailOver.get() && commonsDetails.isEmpty())
                break;
            Map<String, Object> datas = commonsDetails.poll(1, timeUnit);
            if (datas == null) continue;
            System.out.println("取出来的id是："+datas.get("naid"));
            mapList.add(datas);
            if (mapList.size() > 20) {
                saveDatas(mapList,"jd");
                mapList.clear();
            }
        }
        if (!mapList.isEmpty()) {
            saveDatas(mapList,"jd");
            mapList.clear();
        }
        logger.info("commonsDetails队列的数据还剩下",commonsDetails.size());
    }

    @Override
    protected JobResult call() throws SystemException, InterruptedException {
        CountDownLatch cdl = new CountDownLatch(3);
        getHotelIDs = new Thread(() ->
        {
            try
            {
                setJdUrl();
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally
            {
                this.scanList.set(true);
                cdl.countDown();
            }
        });
        getCommons = new Thread(() ->
        {
            try
            {
                setHoteUrl();
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally
            {
                hotelDetailOver.set(true);
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
        getHotelIDs.start();
        getCommons.start();
        saveDatas.start();
        cdl.await();
        return result;
    }

    @Override
    protected JobParam customInit(JobParam args) throws SystemException
    {
        cityUrl = cityUrl.replace("%cityName%", args.getJobName()).replace("%checkindate%","2016-10-1").replace("%checkoutdate%","2016-10-2");
        city=args.getJobName();
        province=args.getType();
        return args;
    }

    @Override
    protected boolean checkArgs(JobParam args) {
        if (StringUtil.isAnyEmpty(args.getJobName(),args.getType()))
            return false;
        return true;
    }

    @Override
    public void destroy() {
        hotelDetailServer.shutdownNow();
        if (Objects.nonNull(getHotelIDs)&&getHotelIDs.isAlive())
            getHotelIDs.interrupt();
        if (Objects.nonNull(getCommons)&&getCommons.isAlive())
            getCommons.interrupt();
        if (Objects.nonNull(saveDatas)&&saveDatas.isAlive())
            saveDatas.interrupt();
        hotelIDs.clear();
        hotelIDs = null;
        commonsDetails.clear();
        commonsDetails = null;
    }
}
