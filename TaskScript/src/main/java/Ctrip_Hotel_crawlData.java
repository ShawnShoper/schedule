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
import org.shoper.schedule.resp.ReportResponse;
import org.shoper.http.apache.HttpClient;
import org.shoper.http.apache.HttpClientBuilder;
import org.shoper.http.exception.HttpClientException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 必要参数1
 */
public class Ctrip_Hotel_crawlData extends JobCaller {

    private String ROOTURL = "http://hotels.ctrip.com/";
    private String ID = "ctrip_";
    private String listUrl = "http://hotels.ctrip.com/Domestic/Tool/AjaxHotelList.aspx";
    private String hotelUrl = "http://hotels.ctrip.com/hotel/%id%.html";
    //    private String roomUrl = "http://hotels.ctrip.com/domestic/ComparePriceNew/RoomList";
    private String roomUrl = "http://hotels.ctrip.com/domestic/comparepricepanel/loadlist";
    private String charset = "utf-8";
    final int timeout = 20;
    final int retry = 3;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private AtomicBoolean hotelIDOver = new AtomicBoolean(false);
    private AtomicBoolean hotelDetailOver = new AtomicBoolean(false);
    private LinkedBlockingQueue<String> hotelIDs = new LinkedBlockingQueue<>(
            100);
    private LinkedBlockingQueue<Map<String, Object>> hotelDetails = new LinkedBlockingQueue<>(
            100);
    Thread hotelList = null;
    Thread hotelDetail = null;
    Thread saveDatas = null;

    @Override
    protected JobResult call () throws SystemException, InterruptedException {
        CountDownLatch cdl = new CountDownLatch(3);
        hotelList = new Thread(() ->
        {
            try {
                getHotelList();
            } catch (InterruptedException e) {

            } catch (Exception e) {
                jobErr(e.getLocalizedMessage());
                e.printStackTrace();
            } finally {
                hotelIDOver.set(true);
                cdl.countDown();
            }
        });
        hotelList.start();
        hotelDetail = new Thread(() ->
        {
            try {
                getHotelDetail();
            } catch (InterruptedException e) {

            } catch (Exception e) {
                jobErr(e.getLocalizedMessage());
                e.printStackTrace();
            } finally {
                hotelDetailOver.set(true);
                cdl.countDown();
            }
        });
        hotelDetail.start();
        saveDatas = new Thread(() ->
        {
            try {
                saveDatas();
            } catch (InterruptedException e) {

            } catch (Exception e) {
                jobErr(e.getLocalizedMessage());
                e.printStackTrace();
            } finally {
                cdl.countDown();
            }
        });
        saveDatas.start();
        cdl.await();
        return result;
    }

    private void jobErr (String msg) {
        result.setDone(false);
        result.setErrMessage(msg);
        result.setSuccess(false);
        result.setError(ReportResponse.Error.EXCEP);
    }

    private void saveDatas () throws InterruptedException {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (; ; ) {
            if (hotelDetailOver.get() && hotelDetails.isEmpty())
                break;

            Map<String, Object> datas = hotelDetails.poll(1, timeUnit);
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
    }

    void getHotelList ()
            throws InterruptedException, IllegalAccessException {
        // 开始分页查询...
        int page = 0;
        int total = 0;
        int current = 0;
        for (; ; ) {
            page++;
            Map<String, String> postData = new HashMap<String, String>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar calendar = Calendar.getInstance();
            //postData.put("cityName", URLEncoder.encode("攀枝花", charset));
            postData.put("StartTime", sdf.format(calendar.getTimeInMillis()));
            calendar.add(Calendar.DATE, 1);
            postData.put("DepTime", sdf.format(calendar.getTimeInMillis()));
            try {
                postData.put("cityName", URLEncoder.encode(args.getJobName(), charset));
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
            postData.put("cityId", args.getJobCode());//"1097");
            postData.put("cityPY", args.getCategory());//"panzhihua");
            postData.put("cityCode", args.getType());//"0812");
            postData.put("page" +
                    "", page + "");
            System.err.println(args.getJobCode()+"......."+args.getCategory()+"....."+args.getType());
            try {

                HttpClient hc = null;
                hc = HttpClientBuilder.custom().setUrl(listUrl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).setFormDatas(postData).build();
                Map<String,String> requestHeader=new HashMap<String,String>();
                requestHeader.put("Proxy-Switch-Ip","yes");
                hc.setRequestHeader(requestHeader);
                hc.setResponseHandle(new AbuyunProxyResponseHandler());
                String jsonMsg = null;
                jsonMsg = hc.post();
                if (StringUtil.isEmpty(jsonMsg)) continue;
                JSONObject jsonObject = JSONObject.parseObject(jsonMsg);
                if (page == 1)
                    total = jsonObject.getInteger("hotelAmount");
                String htmlJson = jsonObject.getString("hotelList");
                Document doc = Jsoup.parse(htmlJson);
                Elements searchresult_list = doc
                        .getElementsByClass("searchresult_list");
                for (Element hotel_emt : searchresult_list) {
                    String hotel_id = hotel_emt.attr("id");
                    if ("hoteltuan".equals(hotel_id))
                        continue;
                    current++;
                    logger.info("hotel_id:",hotel_id);
                    while (!hotelIDs.offer(hotel_id, 2, TimeUnit.SECONDS)) ;
                    if (current >= total) {
                        return;
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (HttpClientException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            } finally {

            }
        }
    }

    private final String category = "ctrip";
    private final String type="hotel";
    private final String source = "携程";
    AtomicInteger available = new AtomicInteger(0);
    ExecutorService hotelDetailServer = Executors.newFixedThreadPool(10);

    void getHotelDetail () throws InterruptedException, IllegalAccessException {
        rootLabel:
        for (; ; ) {
            if (hotelIDOver.get() && hotelIDs.isEmpty()) {
                for (; ; ) {
                    if (available.get() == 0) {
                        break rootLabel;
                    } else {
                        TimeUnit.MILLISECONDS.sleep(10);
                        continue;
                    }
                }
            }
            final String hotel_id = hotelIDs.poll(1, TimeUnit.SECONDS);
            if (StringUtil.isEmpty(hotel_id))
                continue;
            available.incrementAndGet();
            hotelDetailServer.submit(() -> {
                try {
                    Map<String, Object> datas = new HashMap<>();
                    // 构建酒店的 url 地址
                    String hotel_url = hotelUrl.replace("%id%", hotel_id);
                    logger.info("hotel_url:",hotel_url);
                    datas.put("category", category);
                    datas.put("source", source);
                    datas.put("id", ID + hotel_id);
                    datas.put("type",type);
                    datas.put("website", hotel_url);
                    datas.put("updatetime", System.currentTimeMillis());
                    datas.put("name", "");
                    datas.put("tags", new String[]{});
                    datas.put("hotel_facilities", new String[]{});
                    datas.put("rim_facilities", new String[]{});
                    datas.put("roomCount", "");
                    datas.put("openYear", "");
                    datas.put("tel", "");
                    datas.put("fax", "");
                    datas.put("lat", "");
                    datas.put("province", "");
                    datas.put("city", "");
                    datas.put("introduce", "");
                    datas.put("lon", "");
                    datas.put("address", "");
                    datas.put("star", "");
                    datas.put("in_time", "");
                    datas.put("out_time", "");
                    datas.put("rooms", new HashMap<String, Object>());
                    logger.info("hotel_url",hotel_url);
                    Document doc = null;
                    try {
                        HttpClient hc = null;
                        hc = HttpClientBuilder.custom().setUrl(hotel_url).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
                        Map<String,String> requestHeader=new HashMap<String,String>();
                        requestHeader.put("Proxy-Switch-Ip","yes");
                        hc.setRequestHeader(requestHeader);
                        hc.setResponseHandle(new AbuyunProxyResponseHandler());
                        try {
                            doc = hc.getDocument();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (TimeoutException e) {
                            e.printStackTrace();
                        }
                    } catch (HttpClientException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    if (Objects.isNull(doc)) return;
                    //获取酒店星级
                    {
                        Elements grades = doc.getElementsByClass("grade");
                        if (!Objects.isNull(grades) && !grades.isEmpty()) {
                            Element grade = grades.get(0).child(0);
                            String className = grade.attr("class");
                            if (className.startsWith("hotel_stars")) {
                                datas.put("star", className.replace("hotel_stars", ""));
                            }
                        }
                    }
                    //获取酒店省份城市
                    {
                        Elements pc = doc.getElementsByAttributeValue("name", "location");
                        try {
                            checkElementsIsExists(pc);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        String pc_value = pc.get(0).attr("content");
                        String[] pcs = pc_value.split(";");
                        String provinces = pcs[0];
                        String cities = pcs[1];
                        datas.put("province", provinces.split("=")[1]);
                        datas.put("city", cities.split("=")[1]);
                    }
                    Element divDetailMain = doc.getElementById("divDetailMain");
                    if (divDetailMain == null)
                        try {
                            illegalAccessException();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    // 获取酒店信息
                    Elements htl_infos = divDetailMain
                            .getElementsByClass("htl_info");
                    try {
                        checkElementsIsExists(htl_infos);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    Element htl_info = htl_infos.get(0);
                    // 获取酒店名字
                    {
                        Elements names = htl_info.getElementsByClass("name");
                        try {
                            checkElementsIsExists(names);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        Element name = names.get(0);
                        Elements cn_ns = name.getElementsByClass("cn_n");
                        try {
                            checkElementsIsExists(cn_ns);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        Element cn_n = cn_ns.get(0);
                        String hotel_name = cn_n.text();
                        datas.put("name", hotel_name);
                    }

                    // 酒店关键词
                    {
                        Elements label_des = doc
                                .getElementsByClass("special_label");
                        try {
                            checkElementsIsExists(label_des);
                            Elements tags_emt = label_des.get(0).children();
                            Set<String> tags = new HashSet<>();
                            for (Element tag : tags_emt) {
                                tags.add(tag.text());
                            }
                            datas.put("tags", tags.toArray(new String[]{}));
                        } catch (IllegalAccessException e) {
                            //忽略,可能酒店没有标签
                        }
                    }
                    // 酒店设施
                    Element facilities_table = doc
                            .getElementById("J_htl_facilities");
                    {

                        Set<String> facilities = new HashSet<>();
                        if (facilities_table == null) {
                            try {
                                illegalAccessException();
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        Elements facilities_li = facilities_table
                                .getElementsByTag("li");
                        for (Element facilitie : facilities_li) {
                            facilities.add(facilitie.attr("title"));
                        }
                        datas.put("hotel_facilities", facilities.toArray(new String[]{}));
                    }
                    Element hotel_policy = facilities_table.nextElementSibling().nextElementSibling();
                    //酒店政策
                    {
                        Elements th_emts = hotel_policy.getElementsByTag("th");
                        {
                            for (Element th : th_emts) {
                                String policy = th.text();
                                //获取退房时间
                                if ("入住和离店".equals(policy)) {
                                    String policy_value = th.nextElementSibling().text();
                                    policy_value = policy_value.replaceAll(" ", "");
                                    Pattern pattern = Pattern.compile("入住时间：(\\d{2}:\\d{2})以后离店时间：(\\d{2}:\\d{2})");
                                    Matcher matcher = pattern.matcher(policy_value);
                                    if (matcher.find()) {
                                        String in = matcher.group(1);
                                        datas.put("in_time", in);
                                        String out = matcher.group(2);
                                        datas.put("out_time", out);
                                    }
                                    //目前只需要开房退房时间
                                } else if ("接受信用卡".equals(policy)) {
                                    Elements card_spans = th.nextElementSibling().getElementsByTag("span");
                                    if (Objects.isNull(card_spans))
                                        datas.put("support_credit", "不支持信用卡");
                                    else {
                                        List<String> cards = new ArrayList<>();
                                        for (Element emt : card_spans) {
                                            String dataParams = emt.attr("data-params");
                                            String cardName = dataParams.replace("{'options':{'type':'jmp_table','template':'$jmp_table','content':{'txt':'<div class=\"jmp_bd\">", "").replace("</div>'},'classNames':{'boxType':'jmp_table'},'css':{'maxWidth':500}}}", "").replace("'},'classNames':{'boxType':'jmp_table'},'css':{'maxWidth':500}}}", "");
                                            if (!StringUtil.isEmpty(cardName))
                                                cards.add(cardName);
                                        }
                                        datas.put("support_credit", cards);
                                    }
                                }
                            }
                        }
                    }
                    // 周边设施
                    {
                        Set<String> facilities = new HashSet<>();
                        Elements details = doc.getElementsByAttributeValue(
                                "class",
                                "detail_title"
                        );
                        for (Element tag : details) {
                            if ("周边设施".equals(tag.text())) {
                                Element rimFacilities = tag.nextElementSibling();
                                Elements rim_facilities_li = rimFacilities
                                        .getElementsByTag("li");
                                for (Element facilitie : rim_facilities_li) {
                                    facilities.add(facilitie.attr("title"));
                                }
                                datas.put("rim_facilities", facilities.toArray(new String[]{}));
                            }
                        }
                    }
                    // 获取酒店介绍
                    {
                        Element hotel_des = doc.getElementById("htlDes");
                        if (hotel_des == null)
                            try {
                                illegalAccessException();
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        String hotelDesc = hotel_des.text();
                        //获取酒店介绍
                        {
                            Elements introduces = hotel_des.getElementsByAttributeValue("class", "description");
                            if (Objects.nonNull(introduces) && !introduces.isEmpty())
                                datas.put("introduce", introduces.get(0).text());
                        }
                        // 获取酒店房间数
                        {
                            Pattern pattern = Pattern.compile("(\\d+)间房");
                            Matcher matcher = pattern.matcher(hotelDesc);
                            if (matcher.find()) {
                                int roomCount = Integer.valueOf(matcher.group(1))
                                        .intValue();
                                datas.put("roomCount", roomCount);
                            }
                        }

                        // 获取酒店开业时间
                        {
                            Pattern pattern = Pattern.compile("(\\d+)年开业");
                            Matcher matcher = pattern.matcher(hotelDesc);
                            if (matcher.find()) {
                                int openYear = Integer.valueOf(matcher.group(1));

                                datas.put("openYear", openYear);
                            }
                        }
                        // 获取联系方式
                        {
                            Element contact = doc.getElementById("J_realContact");
                            String dataReal = contact.attr("data-real");
                            Pattern tel_pattern = Pattern
                                    .compile("电话((\\d+-)?\\d+)");
                            Matcher tel_matcher = tel_pattern.matcher(dataReal);
                            if (tel_matcher.find()) {
                                String tel = tel_matcher.group(1);
                                datas.put("tel", tel);
                            }
                            Pattern fax_pattern = Pattern
                                    .compile("传真((\\d+-)?\\d+)");
                            Matcher fax_matcher = fax_pattern.matcher(dataReal);
                            if (fax_matcher.find()) {
                                String fax = fax_matcher.group(1);
                                datas.put("fax", fax);
                            }
                        }
                    }
                    // 获取酒店坐标
                    {
                        // <meta itemprop="latitude" content="26.579376713484" />
                        // <meta itemprop="longitude" content="101.73444616661" />
                        Elements latitudes = doc.getElementsByAttributeValue(
                                "itemprop", "latitude");
                        Elements longitudes = doc.getElementsByAttributeValue(
                                "itemprop", "longitude");
                        try {
                            checkElementsIsExists(latitudes);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        try {
                            checkElementsIsExists(longitudes);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        double lat = Double
                                .valueOf(latitudes.get(0).attr("content"));
                        double lon = Double
                                .valueOf(longitudes.get(0).attr("content"));
                        datas.put("lat", lat);
                        datas.put("lon", lon);
                    }
                    // 获取酒店地址
                    {
                        Elements addresses = htl_info.getElementsByClass("adress");
                        try {
                            checkElementsIsExists(addresses);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        Element address = addresses.get(0);
                        String hotel_address = address.text();
                        datas.put("address", hotel_address);
                    }
                    // 获取房间信息
                    {
                        try {
                            List<Map<String, Object>> rooms = null;
                            try {
                                rooms = getRoomsPrice(hotel_id);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            datas.put("rooms", rooms);
                        } catch (IllegalAccessException e) {
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    // 数据抓取完毕,扔入保存队列
                    try {
                        while (!hotelDetails.offer( datas, 2, timeUnit)) ;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }finally {
                    available.decrementAndGet();
                }
            });

        }
    }

    private List<Map<String, Object>> getRoomsPrice (String hotelId) throws IOException, InterruptedException, IllegalAccessException {
        Map<String, String> postDatas = new HashMap<>();
        postDatas.put("hotelId", hotelId);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        postDatas.put("checkIn", sdf.format(calendar.getTimeInMillis()));
        calendar.add(Calendar.DATE, 1);
        postDatas.put("checkOut", sdf.format(calendar.getTimeInMillis()));
        postDatas.put("type", "1");
        String postData = null;
        try {
            org.shoper.http.apache.HttpClient hc = null;
            hc = HttpClientBuilder.custom().setUrl(roomUrl).setProxy(true).setTimeout(timeout).setTimeoutUnit(timeUnit).setRetry(retry).setCharset(charset).build();
            Map<String,String> requestHeader=new HashMap<String,String>();
            requestHeader.put("Proxy-Switch-Ip","yes");
            hc.setRequestHeader(requestHeader);
            hc.setResponseHandle(new AbuyunProxyResponseHandler());
            postData = hc.post();
        } catch (HttpClientException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        if (StringUtil.isEmpty(postData)) return null;
        Document rooms_html = Jsoup.parse(postData);
        List<Map<String, Object>> rooms = new ArrayList<>();
        // 解析rooms_html
        {
            Elements modular_rooms = rooms_html
                    .getElementsByClass("J_modular_warp");
            checkElementsIsExists(modular_rooms);
            for (Element modular_room : modular_rooms) {
                Map<String, Object> room = new HashMap<>();
                //获取房间属性
                {
                    Elements room_caplists = modular_room.getElementsByClass("room_caplist");
                    if (Objects.nonNull(room_caplists) && !room_caplists.isEmpty()) {
                        for (Element room_cap : room_caplists.get(0).children()) {
                            String className = room_cap.attr("class");
                            if ("".equals(className)) {
                                String title = room_cap.attr("title");
                                if (title.startsWith("建筑面积："))
                                    room.put("area", title.replace("建筑面积：", ""));
                                if (title.startsWith("楼层："))
                                    room.put("floor", title.replace("楼层：", ""));
                                if (title.startsWith("床型："))
                                    room.put("bed", title.replace("床型：", ""));
                                if (title.startsWith("可加床：")) {
                                    room.put("addBed", true);
                                    String price = title.replace("可加床：", "").replaceAll("RMB(&nbsp;|\\s)", "").replace("/床/间夜", "");
                                    room.put("addBedPrice", price);
                                }
                            }
                        }
                    }
                }
                // 获取房间名字
                {
                    Elements room_types = modular_room
                            .getElementsByClass("room_type_name");
                    checkElementsIsExists(room_types);
                    String name = room_types.get(0).text().trim();
                    room.put("name", name);
                }
                // 获取房间描述
//                {
//                    Elements room_types = compare_room
//                            .getElementsByClass("room_type");
//                    checkElementsIsExists(room_types);
//                    Elements infos = room_types.get(0)
//                            .getElementsByClass("info");
//                    checkElementsIsExists(infos);
//                    String desc = infos.get(0).text();
//                    room.put("desc", desc);
//                }
                // 获取房型图片
                {
                    Elements room_pics = modular_room.getElementsByClass("room_pic");
                    checkElementsIsExists(room_pics);
                    List<String> pics = new ArrayList<>();
                    for (Element room_pic : room_pics.get(0).getElementsByTag("img")) {
                        pics.add(room_pic.attr("src"));
                    }
                    room.put("pic", pics);
                }
                //获取房间价格
                {
                    Elements room_prices = modular_room.getElementsByClass("room_price");
                    checkElementsIsExists(room_prices);
                    String price = room_prices.get(0).text().replaceAll("¥|起", "");
                }
//                Element compare_list_emt = pic_main_emt.nextElementSibling();
//                if (compare_list_emt == null || !compare_list_emt.className()
//                        .contains("compare_list"))
//                    illegalAccessException();
//                // 获取房间价格
//                {
//                    Elements compare_list_rooms = compare_list_emt
//                            .getElementsByClass("compare_list_room");
//                    checkElementsIsExists(compare_list_rooms);
//                    // 获取价格
//                    for (Element compare_list_room : compare_list_rooms) {
//                        Elements childrens = compare_list_room.children();
//                        checkElementsIsExists(childrens);
//                        String styles = childrens.get(0).attr("style");
//                        // 检查是否是携程的价格
//                        if (styles.contains("ctrip65x20.png")) {
//                            Elements col8s = compare_list_room
//                                    .getElementsByAttributeValue(
//                                            "class",
//                                            "col8"
//                                    );
//                            checkElementsIsExists(col8s);
//                            String price_s = col8s.get(0).text();
//                            price_s = price_s.replace("¥", "");
//                            int index = 0;
//                            if ((index = price_s.indexOf(" ")) > 0) {
//                                price_s = price_s.substring(0, index);
//                            }
//                            double price = Double.valueOf(price_s.trim());
//                            room.put("price", price);
//                            break;
//                        }
//                    }
//                }
                rooms.add(room);
            }
        }
        return rooms;
    }


    void checkElementsIsExists (Elements elements) throws IllegalAccessException {
        if (elements == null || elements.isEmpty())
            illegalAccessException();
    }

    void illegalAccessException () throws IllegalAccessException {
        throw new IllegalAccessException("网站页面规则变更");
    }

    @Override
    protected boolean checkArgs (JobParam args) {
        if (StringUtil.isAnyEmpty(args.getCategory(), args.getJobCode(), args.getJobName(), args.getType()))
            return false;
        return true;
    }

    @Override
    public void destroy () {
        hotelDetailServer.shutdownNow();
        if (Objects.nonNull(hotelList)&&hotelList.isAlive())
            hotelList.interrupt();
        if (Objects.nonNull(hotelDetail)&&hotelDetail.isAlive())
            hotelDetail.interrupt();
        if (Objects.nonNull(saveDatas)&&saveDatas.isAlive())
            saveDatas.interrupt();
        hotelIDs.clear();
        hotelIDs = null;
        hotelDetails.clear();
        hotelDetails = null;
    }

    public static void main(String[] args)
            throws SystemException, InterruptedException
    {
        try {
            ProxyServerPool.importProxyServer(new File("D:/jungle/bigData/proxyip.ls"), Charset.forName("utf-8"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        JobCaller jobCaller = new Ctrip_Hotel_crawlData();
        JobParam jobParam = new JobParam();
        jobParam.setJobCode("1200");
        jobParam.setJobName("盐城");
        jobParam.setCategory("yancheng");
        jobParam.setType("0515");
        jobCaller.init(jobParam);
        jobCaller.run();
        jobCaller.destroy();
    }
}
