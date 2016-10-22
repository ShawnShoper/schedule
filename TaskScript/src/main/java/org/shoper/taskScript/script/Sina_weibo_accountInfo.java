package org.shoper.taskScript.script;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.shoper.http.apache.proxy.ProxyServerPool;
import org.shoper.commons.StringUtil;
import org.shoper.http.apache.handle.AbuyunProxyResponseHandler;
import org.shoper.http.apache.HttpClient;
import org.shoper.http.apache.HttpClientBuilder;
import org.shoper.http.exception.HttpClientException;
import org.shoper.schedule.SystemContext;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.job.JobCaller;
import org.springframework.data.mongodb.core.MongoTemplate;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by admin on 2016-9-19.
 */
public class Sina_weibo_accountInfo extends JobCaller {
    private String personUrl="http://weibo.com/p/%persionId%/info?";
    private String orgUrl="http://weibo.com/%orgId%/about";
    int timeout = 20;
    TimeUnit timeUnit = TimeUnit.SECONDS;
    int retry = 3;
    private String charset ="UTF-8";
    private LinkedBlockingQueue<Map<String,Object>> ids=new LinkedBlockingQueue<>(10);
    private LinkedBlockingQueue<Map<String, Object>> datas = new LinkedBlockingQueue<>(10);
    AtomicBoolean datasOver=new AtomicBoolean(false);
    Thread getInfo;
    Thread saveDatas;
    private final static String SOURCE="新浪微博";
    private final static String CATEGORY="xinlangweibo";
    private final static String COOKIES="SUB=_2AkMggw5ff8NhqwJRmP0dyWPlbYRwww7EieLBAH7sJRMxHRl-yj83qkdTtRCmr4_5JU1j82zPReiPyen8uWYQaQ..; SUBP=0033WrSXqPxfM72-Ws9jqgMF55529P9D9W51TQzEIRIkDwcsmfaDIpDb; SINAGLOBAL=6163946574721.188.1474265446026; UOR=,,www.cnblogs.com; login_sid_t=88ee30e1415421024c0d0c7c3d94675b; YF-Ugrow-G0=b02489d329584fca03ad6347fc915997; YF-V5-G0=3717816620d23c89a2402129ebf80935; _s_tentry=-; Apache=9278882131880.207.1474976667664; ULV=1474976667718:4:4:2:9278882131880.207.1474976667664:1474938144980; YF-Page-G0=8fee13afa53da91ff99fc89cc7829b07";

//    static {
//        try {
//            mongoClient = new MongoClient(new ServerAddress("192.168.100.45", 27017));
//            collection = mongoClient.getDB("daq").getCollection("weibo_user");
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }
//
//    }

    public void getInfo() throws InterruptedException {
        DBCollection collection = SystemContext.getBean("mongoTemplate", MongoTemplate.class).getDb()
                .getCollection("weibo_user");
        DBCursor dbCursor=collection.find();
        while(dbCursor.hasNext()){
            JSONObject jsonObject=JSON.parseObject(dbCursor.next().toString());
            String verify = jsonObject.getString("verify");
            String id = jsonObject.getString("id");
            if ("verify_org".equals(verify)){
               orgInfo(id);
            }else {
                personInfo(id);
            }
        }
    }

    public void personInfo(String personId) throws InterruptedException {
        HttpClient httpClient=null;
        String url=personUrl.replace("%persionId%",personId);
        try {
            httpClient = HttpClientBuilder.custom().setUrl(url).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        Map<String,String> requestHeader = new HashMap<String, String>();
        requestHeader.put("Proxy-Switch-Ip","yes");
        httpClient.setRequestHeader(requestHeader);
        httpClient.setCookies(COOKIES);
        if(Objects.isNull(httpClient)) return;
        Document document= null;
        try {
            document = httpClient.getDocument();
            Elements elements=document.getElementsByTag("script");
            Map<String,Object> data=new HashMap<String,Object>();
            data.put("id",personId);
            data.put("source",SOURCE);
            data.put("category",CATEGORY);
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
                    //微博账号j基本信息
                    Elements element = doc.getElementsByClass("clearfix");
                    Elements elements1 = element.tagName("li");
                    for (Element element1 : elements1) {
                        String biaoti = element1.getElementsByAttributeValueContaining("class", "pt_title").text();
                        if ("昵称".equals(biaoti)) {
                            data.put("name",element1.getElementsByClass("pt_detail").text());
                        } else if ("所在地：".equals(biaoti)) {
                            data.put("address",element1.getElementsByClass("pt_detail").text());
                        } else if ("性别：".equals(biaoti)) {
                            data.put("sex",element1.getElementsByClass("pt_detail").text());
                        } else if ("生日：".equals(biaoti)) {
                            data.put("birthday",element1.getElementsByClass("pt_detail").text());
                        } else if ("博客：".equals(biaoti)) {
                            data.put("blog",element1.getElementsByTag("a").text());
                        } else if ("简介：".equals(biaoti)) {
                            data.put("jianjie",element1.getElementsByClass("pt_detail").text());
                        } else if ("注册时间：".equals(biaoti)) {
                            data.put("regist",element1.getElementsByClass("pt_detail").text());
                        } else if ("公司：".equals(biaoti)) {
                            Map<String,Object> comdata=new HashMap<String,Object>();
                            int i = 1;
                            for(Element element2:element1.getElementsByClass("pt_detail")) {
                                String company = "公司：" + element2.text();
                                comdata.put("company" + i, company.substring(company.indexOf("公司：")+3, company.indexOf("地区：")));
                                if (company.indexOf("地区：") > 0 && company.indexOf("职位：") > 0) {
                                    comdata.put("region" + i, company.substring(company.indexOf("地区：")+3, company.indexOf("职位：")));
                                    comdata.put("position" + i, company.substring(company.indexOf("职位：")+3));
                                } else if (company.indexOf("地区：") > 0) {
                                    comdata.put("region" + i, company.substring(company.indexOf("地区：")+3));
                                } else if (company.indexOf("职位：") > 0) {
                                    comdata.put("position" + i, company.substring(company.indexOf("职位：")+3));
                                }
                                i++;
                            }
                            data.put("company", comdata);
                        } else if ("大学：".equals(biaoti)) {
                            Elements elements2=element1.getElementsByClass("pt_detail");
                            Map<String,Object> college=new HashMap<String,Object>();
                            int i=1;
                            for (Element element2:elements2){
                               college.put("college"+i,element2.text());
                                i++;
                            }
                            data.put("college",college);
                        }else if ("标签：".equals(biaoti)) {
                            Map<String,Object> biaoqian=new HashMap<String,Object>();
                            int i=1;
                            for (Element element2:element1.getElementsByClass("pt_detail").get(0).getElementsByTag("a")){
                                biaoqian.put("label"+i,element2.text());
                                i++;
                            }
                            data.put("label",biaoqian);
                        }else if ("邮箱：".equals(biaoti)){
                            data.put("E-mail",element1.getElementsByClass("pt_detail").text());
                        }else if("QQ：".equals(biaoti)){
                            data.put("QQ",element1.getElementsByClass("pt_detail").text());
                        }else if("个性域名：".equals(biaoti)){
                            data.put("personalizedURL",element1.getElementsByTag("a").text());
                        }else if ("性取向：".equals(biaoti)){
                            data.put("sexual_orientation",element1.getElementsByClass("pt_detail").text());
                        }else if ("感情状况：".equals(biaoti)){
                            data.put("emotional_state",element1.getElementsByClass("pt_detail").text());
                        }
                    }
                    //找出等级
                    Elements elements8=doc.getElementsByClass("W_icon_level");
                    String lv=elements8.text();
                    if(!StringUtil.isEmpty(lv)) {
                        data.put("level", lv);
                    }
                    //找出粉丝 关注 微博数量
                    Elements elements2=doc.getElementsByClass("tb_counter");
                    for (Element element1:elements2){
                        for (Element element2:element1.children()){
                            for(Element element3:element2.children()){
                                for (Element element4:element3.children()){
                                    if ("关注".equals(element4.getElementsByClass("S_txt2").text())){
                                        data.put("focus",element4.getElementsByClass("W_f16").text());
                                    }else if ("粉丝".equals(element4.getElementsByClass("S_txt2").text())){
                                        data.put("fans",element4.getElementsByClass("W_f16").text());
                                    }else if ("微博".equals(element4.getElementsByClass("S_txt2").text())){
                                        data.put("weibo",element4.getElementsByClass("W_f16").text());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            while (!datas.offer(data,2,TimeUnit.SECONDS));


        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (HttpClientException e) {
            e.printStackTrace();
        }

    }

    public void orgInfo(String orgId) throws InterruptedException {
        HttpClient httpClient=null;
        String url=orgUrl.replace("%orgId%",orgId);
        try {
            httpClient = HttpClientBuilder.custom().setUrl(url).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        Map<String,String> requestHeader = new HashMap<String, String>();
        requestHeader.put("Proxy-Switch-Ip","yes");
        httpClient.setRequestHeader(requestHeader);
        httpClient.setResponseHandle(new AbuyunProxyResponseHandler());
        httpClient.setCookies(COOKIES);
        try {
            if(Objects.isNull(httpClient)) return;
            Document document=httpClient.getDocument();
            Elements elements=document.getElementsByTag("script");
            Map<String,Object> data=new HashMap<String,Object>();
            data.put("id",orgId);
            data.put("summary","");
            data.put("level","");
            data.put("connector","");
            data.put("tel","");
            data.put("E-mail","");
            data.put("website",url);
            data.put("updatetime",System.currentTimeMillis());
            data.put("frinedly_link",new ArrayList<String>());
            data.put("name","");
            data.put("pf_intro","");
            data.put("source",SOURCE);
            data.put("category",CATEGORY);
            data.put("verify","verify_org");
            System.out.println("开始获取："+orgId+" 的信息");
            for(Element e:elements){
                String pice=e.html();
                String json=pice.substring(pice.indexOf("{"), pice.lastIndexOf(")"));
                if(json.startsWith("{\"ns\":")){
                    JSONObject jsonObject=JSON.parseObject(json);
                    String html=jsonObject.getString("html");
                    if(StringUtil.isEmpty(html)){
                        continue;
                    }
                    Document doc= Jsoup.parse(html);
                    //找出等级
                    Elements elements1=doc.getElementsByAttributeValueContaining("class", "W_tog_hover");
                    for (Element element:elements1) {
                        String level=element.getElementsByTag("a").text();
                        if (!StringUtil.isEmpty(level)) {
                            String lv = level.replace("Lv.", "");
                            System.out.println("lv:" + lv);
                            data.put("level", lv);
                        }
                    }
                    //找出基本讯息
                    Elements elements2=doc.getElementsByClass("clearfix");
                    Elements elements3=elements2.tagName("li");
                    for(Element element:elements3){
                        String biaoti=element.getElementsByAttributeValueContaining("class","S_txt2").text();
                        if("联系人：".equals(biaoti)){
                            data.put("connector",element.getElementsByAttributeValueContaining("class","pt_detail").text());
                        }else if("电话：".equals(biaoti)){
                            data.put("tel",element.getElementsByAttributeValueContaining("class","pt_detail").text());
                        }else if("邮箱：".equals(biaoti)){
                            data.put("E-mail",element.getElementsByAttributeValueContaining("class","pt_detail").text());
                        }else if("友情链接：".equals(biaoti)){
                            Elements as=element.getElementsByTag("a");
                            List<String> links=new ArrayList<String>();
                            for (Element element1:as){
                                links.add(element1.attr("href"));
                            }
                            data.put("frinedly_link",links);
                        }
                    }
                    //找出简介
                    Elements elements4=doc.getElementsByClass("max_height");
                    String summary=elements4.text();
                    if(!StringUtil.isEmpty(summary)) {
                        data.put("summary", elements4.text());
                    }
                    //微博账号中文名
                    Elements elements5=doc.getElementsByClass("username");
                    String name=elements5.text();
                    if(!StringUtil.isEmpty(name)) {
                        data.put("name", elements5.text());
                    }
                    //中文名下的一个描述 不知道干嘛用的
                    Elements elements6=doc.getElementsByClass("pf_intro");
                    String pf_intro=elements6.text();
                    if(!StringUtil.isEmpty(pf_intro)) {
                        data.put("pf_intro", pf_intro);
                    }
                    //行业类别
                    Elements elements7=doc.getElementsByClass("detail");
                    //这里写个循环遍历是现在这个层只有一个元素，防止以后这个层里面又添加了其他的东西，保证能找正确
                    for(Element element:elements7){
                        String hangye=element.getElementsByClass("S_txt2").text();
                        if ("行业类别".equals(hangye)){
                            String sub_category=element.getElementsByTag("span").get(1).text();
                            data.put("sub_category",sub_category.substring(4).trim());
                        }
                    }
                }
            }
            System.out.println("结束获取："+orgId+" 的信息");
                while (!datas.offer(data, 2, TimeUnit.SECONDS)) ;
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (HttpClientException e) {
            e.printStackTrace();
        }
    }

    void saveDatas() throws InterruptedException {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (; ; ) {
            if (datasOver.get() && datas.isEmpty())
                break;
            Map<String, Object> data = datas.poll(1, timeUnit);
            if (data == null) continue;
            mapList.add(data);
            if (mapList.size() > 10) {
                saveDatas(mapList);
                mapList.clear();
            }
        }
        if (!mapList.isEmpty()) {
            saveDatas(mapList);
            mapList.clear();
        }
        logger.info("commonsDetails队列的数据还剩下",datas.size());
    }

    @Override
    protected JobResult call() throws SystemException, InterruptedException {

        CountDownLatch cdl = new CountDownLatch(2);
        getInfo = new Thread(() ->
        {
            try
            {
                getInfo();
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally
            {
                this.datasOver.set(true);
//				shopIDOver.set(true);
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
        getInfo.start();
        saveDatas.start();
        cdl.await();
        return result;
    }

    @Override
    protected boolean checkArgs(JobParam args) {


        return true;
    }

    @Override
    public void destroy() {
        if (Objects.nonNull(getInfo)&&getInfo.isAlive())
            getInfo.interrupt();

        if (Objects.nonNull(saveDatas)&&saveDatas.isAlive())
            saveDatas.interrupt();
        datas.clear();
        datas = null;

    }

    public static void main(String[] args)
            throws SystemException, InterruptedException
    {
       try {
          ProxyServerPool.importProxyServer(new File("D:/jungle/bigData/proxyip.ls"), Charset.forName("utf-8"));
      } catch (FileNotFoundException e) {
         e.printStackTrace();
    }
        JobCaller jobCaller = new Sina_weibo_accountInfo();
        JobParam jobParam = new JobParam();
        jobCaller.init(jobParam);
        jobCaller.run();
        jobCaller.destroy();

    }
}
