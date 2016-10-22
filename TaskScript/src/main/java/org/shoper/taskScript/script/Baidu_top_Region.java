package org.shoper.taskScript.script;
        import java.net.MalformedURLException;
        import java.text.SimpleDateFormat;
        import java.util.ArrayList;
        import java.util.Date;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;
        import java.util.Queue;
        import java.util.concurrent.BlockingQueue;
        import java.util.concurrent.CountDownLatch;
        import java.util.concurrent.ExecutorService;
        import java.util.concurrent.Executors;
        import java.util.concurrent.LinkedBlockingQueue;
        import java.util.concurrent.LinkedTransferQueue;
        import java.util.concurrent.TimeUnit;
        import java.util.concurrent.atomic.AtomicBoolean;

        import org.shoper.commons.MD5Util;
        import org.shoper.commons.StringUtil;
        import org.shoper.http.apache.HttpClientBuilder;
        import org.shoper.http.apache.handle.AbuyunProxyResponseHandler;
        import org.shoper.schedule.prop.Category;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.data.annotation.Id;
        import org.shoper.http.httpClient.HttpClient;
        import com.alibaba.fastjson.JSONArray;
        import com.alibaba.fastjson.JSONObject;
        import org.shoper.schedule.exception.SystemException;
        import org.shoper.schedule.job.JobParam;
        import org.shoper.schedule.job.JobResult;
        import org.shoper.schedule.provider.job.JobCaller;
        import org.springframework.data.mongodb.core.MongoTemplate;

public class Baidu_top_Region extends JobCaller

{
    @Autowired
    MongoTemplate mongoTemplate;
    String query = "http://top.baidu.com/region/singlelist";
    private String fCategory = "";
    private String category = "";
    private AtomicBoolean listOver = new AtomicBoolean(false);
    private AtomicBoolean parseOver = new AtomicBoolean(false);
    private AtomicBoolean saveOver = new AtomicBoolean(false);
    Queue<Baidu_top_region> regions = new LinkedTransferQueue<Baidu_top_region>();
    BlockingQueue<JSONObject> parseQueue = new LinkedBlockingQueue<>(10);
    BlockingQueue<Map<String, Object>> saveQueue = new LinkedBlockingQueue<>(
            20);
    ExecutorService service = Executors.newFixedThreadPool(20);
    // Map<String, String> region = new HashMap<>();
    @Override
    protected JobResult call() throws SystemException
    {
        queryRegion();
        // 3段式。列表页,详情页,保存页<br>
        CountDownLatch cdl = new CountDownLatch(3);
        service.submit(() -> {
			try
			{
				crawler();
			} catch (Exception e)
			{
				e.printStackTrace();
			} finally
			{
				listOver.set(true);
				cdl.countDown();
			}
		});
        service.submit(() -> {
			try
			{
				parse();
			} catch (Exception e)
			{
				e.printStackTrace();
			} finally
			{
				parseOver.set(true);
				cdl.countDown();
			}
		});

        service.submit(() -> {
			try
			{
				saveData();
			} catch (Exception e)
			{
				e.printStackTrace();
			} finally
			{
				saveOver.set(true);
				cdl.countDown();
			}
		});

        try
        {
            cdl.await();
            result.setSuccess(true);
        } catch (InterruptedException e)
        {
            result.setSuccess(false);
        } finally
        {
            result.setDone(true);
        }
        return result;
    }
    protected void saveData() throws InterruptedException
    {
        List<Map<String, Object>> datas = new ArrayList<>();
        for (;;)
        {
            if (parseOver.get() && saveQueue.isEmpty())
            {
                save(datas);
                break;
            } else
            {
                // 获取analyzeQueue 队列的数据 进行处理
                Map<String, Object> doc = saveQueue.poll(500,
                        TimeUnit.MILLISECONDS);
                if (doc == null)
                    continue;
                datas.add(doc);
                if (datas.size() > 20)
                    save(datas);
            }
        }
    }
    void save(List<Map<String, Object>> datas)
    {
        if (!datas.isEmpty())
        {
            super.saveDatas(datas,"baidu_top");
            datas.clear();
        }
    }
    Map<String, Baidu_top_region> regionMap = new HashMap<>();
    private void queryRegion()
    {

        List<Baidu_top_region> regions =mongoTemplate.findAll(Baidu_top_region.class);
        for (Baidu_top_region map : regions)
        {System.out.println(map);
            this.regionMap.put(map.getCode(), map);
            this.regions.add(map);
        }
    }
    public static void main(String[] args)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd:HH");
        String keyword = "太阳的后裔";
        System.out.println(sdf.format(new Date()));
        String md5 = MD5Util.GetMD5Code(keyword + "2016-03-31:10");
        System.out.println(md5);
    }
    private void parse() throws InterruptedException
    {
        for (;;)
        {
            JSONObject list = null;
            list = parseQueue.poll(500, TimeUnit.MILLISECONDS);
            if (list == null)
            {
                if (listOver.get())
                {
                    logger.info("列表解析任务完成...");
                    break;
                }
                continue;
            }
            JSONArray orders = list.getJSONArray("order");
            JSONObject topWords = list.getJSONObject("topWords");
            for (int i = 0; i < orders.size(); i++)
            {
                String order = orders.getString(i);
                JSONArray words = topWords.getJSONArray(order);
                if (words == null)
                    continue;
                for (int j = 0; j < words.size(); j++)
                {
                    Map<String, Object> data = new HashMap<>();
                    JSONObject word = words.getJSONObject(j);
                    String keyword = word.getString("keyword");
                    int searches = word.getIntValue("searches");
                    int changeRate = word.getIntValue("changeRate");
                    String isNew = word.getString("isNew");
                    String trend = word.getString("trend");
                    String percentage = word.getString("percentage");
                    SimpleDateFormat sdf = new SimpleDateFormat(
                            "yyyy-MM-dd-HH");
                    // 这样做的目的是每按小时对相同的进行更新,不相同的进行新增.
                    // 按时间不同进行不同的保存记录。

                    data.put("keyWord", keyword);
                    data.put("searcheCount", searches);
                    data.put("changeRate", changeRate);
                    data.put("isNew", isNew);
                    Baidu_top_region region = this.regionMap.get(order);
                    Baidu_top_region fRegion = this.regionMap
                            .get(region.getFcode());

                    data.put("city_code", order);
                    data.put("city_name", region.getName());
                    String id = MD5Util.GetMD5Code(region.getName() + keyword
                            + sdf.format(new Date()));
                    data.put("id", id);
                    if (fRegion != null)
                        data.put("f_city_name", fRegion.getName());
                    data.put("trend", trend);
                    data.put("f_type_name", this.fCategory);
                    data.put("type_name", category);
                    data.put("percentage", percentage);
                    data.put("category", Category.BaiDU_Top.getCode());
                    data.put("category_name", Category.BaiDU_Top.getName());
                    data.put("updatetime", new Date().getTime());
                    // Waiting save queue to consumer.
                    while (!saveQueue.offer(data, 500, TimeUnit.MILLISECONDS));
                }
            }

        }
    }
    private void crawler() throws InterruptedException
    {
        // divids 只支持一次传递3个地域参数..
        while (!regions.isEmpty())
        {
            List<Map<String, String>> formData = new ArrayList<>();
            Map<String, String> boardid = new HashMap<>();
            boardid.put("boardid", args.getCategory());
            formData.add(boardid);
            for (int i = 0; i < 3; i++)
            {
                Baidu_top_region region = null;
                if ((region = this.regions.poll()) == null)
                    break;
                Map<String, String> data = new HashMap<>();
                data.put("divids[]", region.getCode());
                formData.add(data);
            }
            Thread.sleep(500);
            org.shoper.http.apache.HttpClient hc = null;
            try {
                hc = HttpClientBuilder.custom().setUrl(query).setProxy(true).setTimeout(10).setTimeoutUnit(TimeUnit.SECONDS).setRetry(3).setCharset(HttpClient.UTF_8).build();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            hc.setResponseHandle(new AbuyunProxyResponseHandler());
            String json = null;
            try
            {
//                json = hc.doPost(HttpClient.PostDataMapToStr(formData),
//                        false);
                json=hc.post();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            JSONObject jsonObject = JSONObject.parseObject(json);
            System.out.println(jsonObject.toJSONString());
            while (!parseQueue.offer(jsonObject, 1000, TimeUnit.SECONDS));
        }
    }

    @Override
    protected JobParam customInit(JobParam jobParam) throws SystemException
    {
        String category = jobParam.getCategory_name();
        if (category.contains("-"))
        {
            String[] cates = category.split("-");
            this.fCategory = cates[0];
            this.category = cates[1];
        } else
        {
            this.category = jobParam.getCategory_name();
        }
        return super.customInit(jobParam);
    }
    @Override
    protected boolean checkArgs(JobParam args)
    {
        if (StringUtil.isEmpty(args.getCategory_name())
                && StringUtil.isEmpty(args.getCategory()))
            return false;
        return true;
    }

    @Override
    public void destroy()
    {
        service.shutdown();
    }
    static class Baidu_top_region
    {
        private String name;
        private String code;
        private String fcode;

        public String getFcode()
        {
            return fcode;
        }
        public void setFcode(String fcode)
        {
            this.fcode = fcode;
        }
        @Id
        private String id;
        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }
        public String getCode()
        {
            return code;
        }
        public void setCode(String code)
        {
            this.code = code;
        }
        public String getId()
        {
            return id;
        }
        public void setId(String id)
        {
            this.id = id;
        }
        @Override
        public String toString()
        {
            return "Baidu_top_region [name=" + name + ", code=" + code
                    + ", fcode=" + fcode + ", id=" + id + "]";
        }

    }
}

