package org.shoper.taskScript.script;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.Jsoup;
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
import org.shoper.http.apache.HttpClient;
import org.shoper.http.apache.HttpClientBuilder;
import org.shoper.http.exception.HttpClientException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by xp7.82414 on 2016/9/14.
 */
public class MFW_Hotelcommons extends JobCaller {

    static String charset = "UTF-8";
    public static String jdurl ="http://www.mafengwo.cn/";
    static int timeout = 20;
    static int retry =3;
    static TimeUnit timeUnit = TimeUnit.SECONDS;
    private String cityUrl="http://www.mafengwo.cn/hotel/ajax.php?iMddId=%id%&sSortType=comment&sAction=getPoiList5&iPage=";
    private String hotelUrl="http://www.mafengwo.cn/hotel/info/comment_list?poi_id=%id%&keyword_index=-1&page=";
    private  String hotelnameUrl="http://www.mafengwo.cn/hotel/%id%.html";
    private LinkedBlockingQueue<String> hotelIDs = new LinkedBlockingQueue<>(20);
    private LinkedBlockingQueue<Map<String, Object>> commonsDetails = new LinkedBlockingQueue<>(20);
    AtomicBoolean scanList = new AtomicBoolean(false);
    AtomicBoolean hotelDetailOver=new AtomicBoolean(false);
    Thread getHotelIDs;
    Thread getCommons;
    Thread saveDatas;

    public static void main(String[] args) throws InterruptedException, SystemException {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");

        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "disable");

        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "disable");

        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "disable");
        try {
            ProxyServerPool.importProxyServer(new File("D:/jungle/bigData/proxyip.ls"), Charset.forName("utf-8"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        JobCaller jobCaller = new MFW_Hotelcommons();
        JobParam jobParam = new JobParam();
        jobParam.setJobCode("12112");
        jobParam.setJobName("雅安");
        jobCaller.init(jobParam);
        jobCaller.run();
        jobCaller.destroy();
    }

    /**
     * 获取这个城市的酒店总共多少页
     * @return
     * @throws InterruptedException
     */
    int getTotalPage() throws InterruptedException {
        Integer pagel=1;
        try {
            hoteurl=cityUrl+1;
            //String hoteurl="http://www.mafengwo.cn/hotel/ajax.php?iMddId=10035&sSortType=comment&sAction=getPoiList5&iPage=24";
            HttpClient httpClient = null;
            httpClient = HttpClientBuilder.custom().setUrl(hoteurl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
            Map<String,String> requestHeader = new HashMap<String, String>();
            requestHeader.put("Proxy-Switch-Ip","yes");
            httpClient.setRequestHeader(requestHeader);
            httpClient.setResponseHandle(new AbuyunProxyResponseHandler());
            String doc = httpClient.doGet();
            JSONObject jsonObject = JSONObject.parseObject(doc);
            String htmlJson = jsonObject.getString("html");
            Document dc = Jsoup.parse(htmlJson);
            Elements pagehotel =dc.getElementsByClass("page-hotel");
            for (Element pagehou : pagehotel) {
                //判断多少页
                Elements elements = pagehou.getElementsByClass("count");
                String totals=elements.get(0).child(0).text();
                pagel = Integer.parseInt(totals);

            }
        } catch (MalformedURLException e) {
            e.printStackTrace();

        } catch (HttpClientException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return pagel;
    }



    String hoteurl;
    /**
     * 获取所有酒店的ID
     */
    void setJdurl() throws InterruptedException{

        int total =getTotalPage();//共多少页
        System.out.println("总共有"+total+"页");
        for(int i=1;i<total+1;i++) {
            try {
                hoteurl=cityUrl+i;
                //String hoteurl="http://www.mafengwo.cn/hotel/ajax.php?iMddId=10035&sSortType=comment&sAction=getPoiList5&iPage=24";
                org.shoper.http.apache.HttpClient httpClient = null;
                httpClient = HttpClientBuilder.custom().setUrl(hoteurl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
                Map<String,String> requestHeader = new HashMap<String, String>();
                requestHeader.put("Proxy-Switch-Ip","yes");
                httpClient.setRequestHeader(requestHeader);
                httpClient.setResponseHandle(new AbuyunProxyResponseHandler());
                String doc = httpClient.doGet();
                JSONObject jsonObject = JSONObject.parseObject(doc);
                String htmlJson = jsonObject.getString("html");
                Document dc = Jsoup.parse(htmlJson);
                Elements info = dc.getElementsByClass("title");
                for (Element h3 : info) {
                    Elements a = h3.getElementsByTag("h3");
                    for (Element hr : a) {
                        Elements hrefs = hr.getElementsByTag("a");
                        String href = hrefs.attr("href");
                        String hotelID=href.substring(7,href.indexOf(".html")+0);
                        System.out.println("hotelID:"+hotelID);
                        while (!hotelIDs.offer(hotelID,2, TimeUnit.SECONDS));
                    }
                }
            } catch (HttpClientException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }catch (Exception e) {
                System.out.println("出错的url：："+hoteurl);
                e.printStackTrace();
            }
        }
    }

    private String city;
    private static final String SOURCE = "蚂蜂窝";
    private static final String CATEGORY="mafengwo" ;
    private static final String TYPE="hotel_comment";
    private String province;
    AtomicInteger available = new AtomicInteger(0);
    ExecutorService hotelDetailServer = Executors.newFixedThreadPool(5);
    String hotelcommonsurl;
    /**
     * 获取酒店评论数据
     * @throws HttpClientException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     * @throws IOException
     */
    void pageurl() throws InterruptedException{
        roolabel:
        for(;;){
            if (scanList.get() && hotelIDs.isEmpty())
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
            String hotelID = hotelIDs.poll(1, TimeUnit.SECONDS);
            if (StringUtil.isEmpty(hotelID))
                continue;
            available.incrementAndGet();
            hotelDetailServer.submit(() -> {
                try {
                    //取酒店名字
                    String name =null;
                    String hotelcommonsurl = hotelnameUrl.replace("%id%", hotelID);
                    HttpClient hc = null;
                    hc = HttpClientBuilder.custom().setUrl(hotelcommonsurl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
                    Map<String, String> requestHeader1 = new HashMap<String, String>();
                    requestHeader1.put("Proxy-Switch-Ip", "yes");
                    hc.setRequestHeader(requestHeader1);
                    hc.setResponseHandle(new AbuyunProxyResponseHandler());
                    if (Objects.isNull(hc)) return;
                    Document document=hc.getDocument();
                    name =document.getElementsByClass("main-title").text();
                    try {
                        int page=0;
                        for(;;) {
                            page++;
                            String pageurl = hotelUrl.replace("%id%", hotelID) + page;
                            HttpClient httpClient = null;
                            httpClient = HttpClientBuilder.custom().setUrl(pageurl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
                            Map<String, String> requestHeader = new HashMap<String, String>();
                            requestHeader.put("Proxy-Switch-Ip", "yes");
                            httpClient.setRequestHeader(requestHeader);
                            httpClient.setResponseHandle(new AbuyunProxyResponseHandler());
                            String doc = httpClient.doGet();//接受网页
                            if (StringUtil.isEmpty(doc)) continue ;
                            JSONObject jsonObject = JSONObject.parseObject(doc);//解析HTML转换
                            String htmlJson = jsonObject.getString("html");
                            if(StringUtil.isEmpty(htmlJson)){
                                System.err.println("循环结束");
                                return;
                            }
                            Document dc = Jsoup.parse(htmlJson);
                            Elements comm = dc.getElementsByClass("comm-item");
                            for (Element items : comm) {
                                Elements item = items.getElementsByClass("comm-item");
                                for (Element users : item) {

                                    Map<String, Object> data = new HashMap<String, Object>();
                                    data.put("hotelname",name);
                                    data.put("hotelID",hotelID);
                                    data.put("city", city);
                                    data.put("source", SOURCE);
                                    data.put("category", CATEGORY);
                                    data.put("type", TYPE);
                                    data.put("province",province);
                                    data.put("updatetime", System.currentTimeMillis());
                                    data.put("like", "");
                                    data.put("website",pageurl);
                                    data.put("content", "");
                                    data.put("username","");
                                    Elements user = users.getElementsByClass("user");
                                    String username = user.get(0).getElementsByClass("name").text();
                                    data.put("username",username);
                                    //得到用户ID
                                    for (Element a :user ){
                                        Elements hrefs =a.getElementsByTag("a");
                                        String href = hrefs.attr("href");
                                        String userID=href.substring(28,href.indexOf("&"));
                                        //这是评论ID
                                        data.put("commentID",href.substring(href.indexOf("&c=")+3));
                                        data.put("userID",userID);
                                        data.put("id","mafengwo_"+href.substring(href.indexOf("&c=")+3));
                                        String userurl="http://www.mafengwo.cn/u/%id%.html".replace("%id%",userID);
                                        try {
                                            org.shoper.http.apache.HttpClient httpClient2 = null;
                                            httpClient2 = HttpClientBuilder.custom().setUrl(userurl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
                                            Map<String,String> requestHeader2 = new HashMap<String, String>();
                                            requestHeader2.put("Proxy-Switch-Ip","yes");
                                            httpClient2.setRequestHeader(requestHeader2);
                                            httpClient2.setResponseHandle(new AbuyunProxyResponseHandler());
                                            if (Objects.isNull(httpClient2)) continue ;
                                            Document dc1 =httpClient2.getDocument();
                                            Elements tags =dc.getElementsByClass("MAvaInfo");
                                            if (tags==null){
                                            }else if (tags !=null)
                                                for (Element MAvaPlaces : tags) {
                                                    String MAvaPlace = MAvaPlaces.getElementsByClass("MAvaPlace").text();
                                                    if (MAvaPlace.isEmpty()) {
                                                    } else {
                                                        String[] aa = MAvaPlace.split(":");
                                                        for (int i = 0; i < aa.length; i++) {
                                                            data.put("userLocation", aa[i].substring(3));
                                                            System.out.println(aa[i].substring(3));
                                                        }
                                                    }
                                                }
                                                } catch (HttpClientException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    String like = users.getElementsByClass("like").text();
                                    data.put("like", like);
                                    String txt = users.getElementsByClass("txt").text();
                                    data.put("content", txt);
                                    String times = users.getElementsByClass("time").text();
                                    SimpleDateFormat sdf =new SimpleDateFormat("yyyy-MM-dd");
                                    Date time =sdf.parse(times);
                                    data.put("createtime", time.getTime());
                                    while (!commonsDetails.offer(data, 2, TimeUnit.SECONDS)) ;
                                }
                            }
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }catch(Exception e){
                    System.out.println("出错的url："+hotelcommonsurl);
                    e.printStackTrace();
                } finally {
                    available.decrementAndGet();
                }
            });
        }
    }

    void saveDatas() throws InterruptedException{
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (; ; ) {
            if (hotelDetailOver.get() && commonsDetails.isEmpty())
                break;
            Map<String, Object> datas = commonsDetails.poll(1, timeUnit);
            if (datas == null) continue;
            mapList.add(datas);
            if (mapList.size() > 20) {
                saveDatas(mapList);
                mapList.clear();
            }
        }
        if (!mapList.isEmpty()) {
            saveDatas(mapList);
            mapList.clear();
        }
        logger.info("commonsDetails队列的数据还剩下",commonsDetails.size());
    }

    @Override
    protected JobParam customInit(JobParam args) throws SystemException
    {
        cityUrl = cityUrl.replace("%id%", args.getJobCode());
        city=args.getJobName();
        province=args.getType();
        return args;
    }

    @Override
    protected JobResult call() throws SystemException, InterruptedException {
        CountDownLatch cdl = new CountDownLatch(3);
        getHotelIDs = new Thread(() ->
        {
            try
            {
                setJdurl();
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
                pageurl();
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
    protected boolean checkArgs(JobParam args) {
        if (StringUtil.isAnyEmpty(args.getJobCode(),args.getJobName(),args.getType()))
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

