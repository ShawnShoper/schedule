package org.shoper.parse;

import org.shoper.parse.parseyml.Parseyml;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jungle on 2016-10-12.
 */
public class ParserControl {
    private String paramName;
    Parseyml parseryml;
    public void parse(String argName){
        //類信息都存在mongo中 然後從mongo裡面讀取信息找出所有的類名自動加載順序。
        List<String> klassNames=new ArrayList<String>();
//        DBCollection collection = SystemContext.getBean("mongoTemplate", MongoTemplate.class).getDb()
//                .getCollection("rule");
//        DBCursor dbCursor=collection.find();
//        while (dbCursor.hasNext()){
//            try {
//            JSONObject jsonObject= JSON.parseObject(dbCursor.next().toString());
//            String klassName=jsonObject.getString("klassName");
//            File file=new File("D:\\jungle\\schedule\\parser\\dazhongdianping-parser\\target\\dazhongdianping-parser-1.0-SNAPSHOT.jar");
//            ClassLoaderHandler clh = null;
//                clh = ClassLoaderHandler.newInstanceFromJar(file);
//                Object obj =clh.getClassByName(klassName).newInstance();
//                Method method = clh.getClassByName(klassName).getDeclaredMethod("hello");
//                method.invoke(obj);
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (InstantiationException e) {
//                e.printStackTrace();
//            } catch (InvocationTargetException e) {
//                e.printStackTrace();
//            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            }
//
//        }
    }
    public static void main(String[] args) {
        System.out.println("");
    }
}
