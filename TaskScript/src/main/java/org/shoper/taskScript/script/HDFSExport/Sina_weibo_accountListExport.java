package org.shoper.taskScript.script.HDFSExport;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.Configuration;
import org.shoper.commons.StringUtil;
import org.shoper.dynamiccompile.ClassLoaderHandler;
import org.shoper.schedule.SystemContext;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.job.JobCaller;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by admin on 2016-10-9.
 */
public class Sina_weibo_accountListExport extends JobCaller {
    Thread saveDatas;
    Thread explainDatas;
    private LinkedBlockingQueue<String> informations = new LinkedBlockingQueue<>(30);
    private AtomicBoolean scanPersonIdInfo = new AtomicBoolean(false);
    private FileSystem fs = null;
    private int timeout = 20;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private int retry = 3;
    private String charset = "UTF-8";
    private String filename;
    private String taskName=args.getJobName();
    private String klassName;
    enum KlassName{
        Sina_weibo_accountList
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        File file=new File("D:\\jungle\\schedule\\parser\\dazhongdianping-parser\\target\\dazhongdianping-parser-1.0-SNAPSHOT.jar");
        ClassLoaderHandler clh = ClassLoaderHandler.newInstanceFromJar(file);
        Class className=clh.getClassByName("HelloWorld");
        Object obj =className.newInstance();
        Method  method = className.getDeclaredMethod("hello");
        method.invoke(obj);

    }



    private void explainDatas(){
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        if ("新浪微博账号列表".equals(taskName)) {
            try {
                Path path = new Path("/weibo/sina/user_list/");
                // FSDataInputStream fsinput =fs.open(path);
                //获取这个路径下的所有文件
               FileStatus[] fileStatuses= fs.listStatus(path);
               for(FileStatus fileStatu:fileStatuses){
                   String fileName=fileStatu.getPath().toString();
                   String time=fileName.substring(fileName.indexOf("/weibo/sina/user_list/"),fileName.lastIndexOf("/")).replace("/","");
                   System.out.println(time);
                   Date date=format.parse(time);
                   DBCollection collection = SystemContext.getBean("mongoTemplate", MongoTemplate.class).getDb()
                           .getCollection("rule");
                   DBCursor dbCursor=collection.find();
                   roolabel:
                   while(dbCursor.hasNext()){
                       JSONObject jsonObject= JSON.parseObject(dbCursor.next().toString());
                       String taskName = jsonObject.getString("taskName");
                       if("新浪微博账号列表".equals(taskName)) {
                           String startTime = jsonObject.getString("startTime");
                           String endTime=jsonObject.getString("endTime");
                           //判断mongo取出来的这个数据是否有最后时间
                           if (StringUtil.isEmpty(endTime)) {
                                    Date startDate=format.parse(startTime);

                                    if (date.getTime()>startDate.getTime()){
                                         klassName=jsonObject.getString("klassName");
                                        Class className=Class.forName(klassName);
                                        ClassLoaderHandler clh=ClassLoaderHandler.newInstance();

                                        break roolabel;
                                    }else {
                                       continue;
                                    }
                           }
                           //能走到这里mongo取出来的这个数据肯定就有最后时间
                           Date startDate=format.parse(startTime);
                           Date endDate=format.parse(endTime);
                           if(date.getTime()>startDate.getTime()&&date.getTime()<endDate.getTime()){

                               break roolabel;
                           }else {
                               continue;
                           }
                       }
                   }
               }


            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                logger.info("找不到这个类",klassName);
                e.printStackTrace();
            }


        }
    }


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
        return result;
    }

    protected JobParam customInit(JobParam args) throws SystemException
    {

        return args;
    }

    @Override
    protected boolean checkArgs(JobParam args) {
        if (StringUtil.isAnyEmpty(args.getJobName())) {
            return false;
        }
        return true;
    }
    @Override
    public void destroy() {

    }
}
