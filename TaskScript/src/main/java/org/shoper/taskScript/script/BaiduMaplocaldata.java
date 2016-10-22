package org.shoper.taskScript.script;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.lang.StringUtils;
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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;



/**
 * 开发人:唐元林
 * 来源：百度派出所地图
 * Created by xp7.82414 on 2016/10/8.
 */
public class BaiduMaplocaldata extends JobCaller{
    private String charset = HttpClient.UTF_8;
    final int timeout = 20;
    final int retry = 3;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private String cityName ="http://map.baidu.com/?newmap=1&reqflag=pcmap&biz=1&from=webmap&da_par=direct&qt=con&from=webmap&c=%cityName%&wd=wd=%E5%B9%BF%E4%B8%9C%E7%9C%81%20%E6%B4%BE%E5%87%BA%E6%89%80&pn=";
    Thread getDatas;
    Thread saveDatas;
    private LinkedBlockingQueue<Map<String, Object>> hospitalDetails = new LinkedBlockingQueue<>(10);
    private AtomicBoolean hospitalDetailOver = new AtomicBoolean(false);
    private static final String SOURCE = "百度地图";
    private static final String CATEGORY="baidu" ;
    private String city;
    private String province;
//    private String address;
/*
    public static void main(String[] args) throws HttpClientException, InterruptedException, TimeoutException {
        BaiDuMapHospitalData t = new BaiDuMapHospitalData();
        t.Mapget();
    }*/

    void Mapget() throws HttpClientException, InterruptedException, TimeoutException, MalformedURLException {
//        System.out.println("开始执行"+"===========================");
        int pn =0;
        for (;;) {
//            String jdUrl = "http://map.baidu.com/?newmap=1&reqflag=pcmap&biz=1&from=webmap&da_par=direct&qt=con&from=webmap&c=257&wd=wd=%E5%B9%BF%E4%B8%9C%E7%9C%81%20%E5%8C%BB%E9%99%A2&pn="+pn++;
            String jdUrl =cityName+ +pn++;
            org.shoper.http.apache.HttpClient httpClient = null;

            httpClient = HttpClientBuilder.custom().setUrl(jdUrl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
            Map<String,String> requestHeader = new HashMap<String, String>();
            requestHeader.put("Proxy-Switch-Ip","yes");
            httpClient.setRequestHeader(requestHeader);
            httpClient.setResponseHandle(new AbuyunProxyResponseHandler());
            String doc = httpClient.doGet();
            JSONObject jsonObject = JSONObject.parseObject(doc);
            JSONArray jsonArray = jsonObject.getJSONArray("content");
            if(Objects.isNull(jsonArray))return;
            System.out.println("第一页"+pn);
            for (int i = 0; i < jsonArray.size(); i++){
                JSONObject comment = jsonArray.getJSONObject(i);
                String exts =comment.getString("ext");
                JSONObject ext = JSONObject.parseObject(exts);
                if (ext != null){
                    Map<String,Object> datas =new HashMap<String,Object>();
                    datas.put("name","");
                    datas.put("address","");
                    datas.put("tel","");
                    datas.put("tag","");
                    datas.put("lng","");
                    datas.put("lat","");
                    datas.put("std_tag","");
                    datas.put("id","");
                    datas.put("source", SOURCE);
                    datas.put("category", CATEGORY);
                    datas.put("city", city);
                    datas.put("province",province);

                    JSONObject ett =ext.getJSONObject("detail_info");
//                  //医院名字
                    String name = ett.getString("name");
                    datas.put("name",name);
                    System.out.println(name);
                    //医院地址
                    String addr = ett.getString("poi_address");
                    datas.put("address",addr);
                    System.out.println(addr);
                    //医院电话
                    String phone = ett.getString("phone");
                    datas.put("tel",phone);
                    System.out.println(phone);
                    //医院等级
                    String tag = ett.getString("tag");
                    datas.put("tag",tag);
                    System.out.println(tag);
                    //x纬度
                    // 根据addr解析经纬度.
                    MapVO mapVO = BaiMapUtil.addressToCoord("", addr);
                    String lat = mapVO.getLat();
                    String lng = mapVO.getLng();
                    datas.put("lng",lng);
                    datas.put("lat",lat);
                    System.out.println(lng);
                    System.out.println(lat);
                    //医院类型
                    String std_tag = ett.getString("std_tag");
                    datas.put("std_tag",std_tag);
                    System.out.println(std_tag);
                    //唯一ID
                    String arid =ett.getString("areaid");
                    String id =MD5Util.GetMD5Code(name+addr+phone);
                    datas.put("id",id);
                    System.out.println(id);
                    while (!hospitalDetails.offer(datas, 2, timeUnit)) ;
                }
            }
        }
    }

    static class BaiMapUtil
    {
        public static final String KEY_1 = "DE7c0d9d33dae2e92a25b591f005b7b8";

        /**
         * 返回输入地址的经纬度坐标 key lng(经度),lat(纬度)
         */
        public static MapVO addressToCoord(String city, String address)
        {
            BufferedReader in = null;
            MapVO vo = new MapVO();
            vo.setCity(city);
            vo.setAddress(address);
            try
            {
                // 将地址转换成utf-8的16进制
                address = URLEncoder.encode(address, "UTF-8");
                // 如果有代理，要设置代理，没代理可注释
                // System.setProperty("http.proxyHost","192.168.1.188");
                // System.setProperty("http.proxyPort","3128");
                URL tirc = new URL("http://api.map.baidu.com/geocoder?address="
                        + address + "&output=json&key=" + KEY_1);

                in = new BufferedReader(
                        new InputStreamReader(tirc.openStream(), "UTF-8"));
                String res;
                StringBuilder sb = new StringBuilder("");
                while ((res = in.readLine()) != null)
                {
                    sb.append(res.trim());
                }
                String str = sb.toString();
                Map<String, String> map = null;
                if (StringUtils.isNotEmpty(str))
                {
                    int lngStart = str.indexOf("lng\":");
                    int lngEnd = str.indexOf(",\"lat");
                    int latEnd = str.indexOf("},\"precise");
                    if (lngStart > 0 && lngEnd > 0 && latEnd > 0)
                    {
                        String lng = str.substring(lngStart + 5, lngEnd);
                        String lat = str.substring(lngEnd + 7, latEnd);
                        vo.setLng(lng);
                        vo.setLat(lat);
                    }
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally
            {
                try
                {
                    if (in != null)
                    {
                        in.close();
                    }
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            return vo;
        }

    }
    static class MapVO
    {
        private String city; // 城市名称
        private String address; // 详细地址
        private String lng; // 经度
        private String lat; // 纬度

        public String getCity()
        {
            return city;
        }

        public void setCity(String city)
        {
            this.city = city;
        }

        public String getAddress()
        {
            return address;
        }

        public void setAddress(String address)
        {
            this.address = address;
        }

        public String getLng()
        {
            return lng;
        }

        public void setLng(String lng)
        {
            this.lng = lng;
        }

        public String getLat()
        {
            return lat;
        }

        public void setLat(String lat)
        {
            this.lat = lat;
        }
    }

    /**
     * 保存数据
     */
    void saveDatas() throws InterruptedException {
//        System.out.println("saveDatas 启动了");
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (; ; ) {
            if (hospitalDetailOver.get() && hospitalDetails.isEmpty())
                break;
            Map<String, Object> datas = hospitalDetails.poll(1, timeUnit);
            if (datas == null) continue;
//            System.out.println("====================");
            mapList.add(datas);
            if (mapList.size() > 20) {
//                System.out.println("------------------------");
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

    @Override
    protected JobParam customInit(JobParam args) throws SystemException
    {
        cityName=cityName.replace("%cityName%",args.getJobCode());
        city=args.getJobName();
        province=args.getType();
        return args;
    }
    /**
     * 任务处理方法,必须实现。
     *
     * @return
     * @throws InterruptedException
     */
    @Override
    protected JobResult call() throws SystemException, InterruptedException {
        System.out.println("存储数据"+"++++++++++++++++++++++");
        CountDownLatch cdl = new CountDownLatch(2);
        getDatas =new Thread(() ->{
            try {
                Mapget();
            } catch (HttpClientException e1) {
                e1.printStackTrace();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            } catch (TimeoutException e1) {
                e1.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }catch (Exception e){

            }finally {
                hospitalDetailOver.set(true);
                cdl.countDown();
            }
        });
        saveDatas=new Thread(() ->{
            try {
                saveDatas();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }finally {
                cdl.countDown();
            }
        });
        getDatas.start();
        saveDatas.start();
        cdl.await();
        return result;
    }
    /**
     * 自定义检测参数<br>
     * Created by ShawnShoper Apr 14, 2016
     *
     * @param args
     * @return
     */
    @Override
    protected boolean checkArgs(JobParam args) {
        if(StringUtil.isAnyEmpty(args.getJobName())){
            return false;
        }
        return true;
    }
    @Override
    public void destroy() {

    }
}

