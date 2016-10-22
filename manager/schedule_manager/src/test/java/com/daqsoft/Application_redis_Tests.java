package com.daqsoft;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.shoper.ManagerApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ManagerApplication.class)
public class Application_redis_Tests {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    @Test
    public void redisTemplateTests() throws Exception {
        redisTemplate.opsForList().leftPush("test", "asdasd");
        redisTemplate.opsForHash().putIfAbsent("job", "job1", "testjob1");
        redisTemplate.opsForHash().putIfAbsent("job", "job2", "testjob2");
    }

    @Test
    public void redisTemplateReadTest() {
        System.out.println(redisTemplate.opsForHash().get("job", "job2"));
        redisTemplate.opsForValue().set("a","asd");
    }
}