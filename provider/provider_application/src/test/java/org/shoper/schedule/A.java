package org.shoper.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Created by ShawnShoper on 16/8/5.
 */
public class A extends C {
    @Autowired
    RedisTemplate redisTemplate;

    public void say() {
        System.out.println("a");
    }
}
