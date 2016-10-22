import org.omg.CORBA.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.job.JobCaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Created by ShawnShoper on 16/8/4.
 */
public class ForTest extends JobCaller {
    @Autowired
    RedisTemplate redisTemplate;
    protected JobResult call() throws SystemException, InterruptedException {
        for (;;) {
            System.out.println(redisTemplate.opsForHash().get("job", "job2"));
            TimeUnit.SECONDS.sleep(2);
        }
    }

    protected boolean checkArgs(JobParam args) {
        return true;
    }

    @Override
    public void destroy() {

    }

    public static void main(String[] args){

    }
}
