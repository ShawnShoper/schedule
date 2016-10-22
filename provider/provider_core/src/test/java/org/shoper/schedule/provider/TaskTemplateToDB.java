package org.shoper.schedule.provider;

import com.mongodb.MongoClient;


import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

public class TaskTemplateToDB {

    MongoClient mongoClient = null;
    DBCollection collection = null;
    List<Map<String, String>> data = new ArrayList<>();

    @Before
    public void init() throws Exception {
        mongoClient = new MongoClient(new ServerAddress("192.168.0.24", 27017));
        collection = mongoClient.getDB("daq").getCollection("taskTemplate");
        {
            Map<String, String> d = new HashMap<>();
            d.put("fn", "Baidu_travel_remark");
            d.put("url", "http://lvyou.baidu.com/");
            d.put("cookies", null);
            d.put("name", "百度旅游点评模版");
            data.add(d);
        }
    }

    @Test
    public void save() throws Exception {
        for (Map<String, String> map : data) {
            String name = map.get("name");
            DBObject query = new BasicDBObject();
            query.put("name", name);
            DBObject dbObject = collection.findOne(query);
            if (dbObject == null) {
                // save
                DBObject save = new BasicDBObject();
                save.put("name", name);
                save.put(
                        "code",
                        FileUtils
                                .readFileToString(new File(
                                        "/Users/ShawnShoper/Documents/workspace/daqsoft_schedule_provider/src/main/java/com/daqsoft/schedule/provider/script/Baidu_travel_remark.java"))
                                .getBytes()
                );
                save.put("createTime", new Date());
                save.put("url", map.get("url"));
                save.put("cookies", map.get("cookies"));
                save.put("updateTime", new Date());
                collection.save(save);
            } else {
                DBObject q = new BasicDBObject();
                q.put("name", name);
                DBObject update = new BasicDBObject();
                update.put(
                        "code",
                        FileUtils
                                .readFileToString(new File(
                                        "/Users/ShawnShoper/Documents/workspace/daqsoft_schedule_provider/src/main/java/com/daqsoft/schedule/provider/script/Baidu_travel_remark.java"))
                                .getBytes()
                );
                update.put("updateTime", new Date());
                DBObject updateSetValue = new BasicDBObject("$set", update);
                collection.update(q, updateSetValue, true, false);
            }
        }
    }
}
