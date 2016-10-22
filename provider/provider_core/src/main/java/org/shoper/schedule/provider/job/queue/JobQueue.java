package org.shoper.schedule.provider.job.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.pojo.TaskMessage;
import org.shoper.schedule.provider.system.RunningStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job queue.<br>
 * Storing some job that need be processed
 *
 * @author ShawnShoper
 */
public class JobQueue {
    private static Logger logger = LoggerFactory.getLogger(JobQueue.class);
    /**
     * Pending queue for storing job
     */
    private static volatile BlockingDeque<TaskMessage> pendingQueue = new LinkedBlockingDeque<TaskMessage>();
    /**
     * Running queue
     */
    private static volatile ConcurrentMap<String, TaskMessage> runningQueue = new ConcurrentHashMap<String, TaskMessage>();

    /**
     * Getting holder job count
     *
     * @return
     */
    public static int getHolder() {
        return runningQueue.size() + pendingQueue.size();
    }

    public static void addRunning(String key, TaskMessage value) {
        runningQueue.put(key, value);
    }

    public static void removeRunning(String key) {
        if (runningQueue.containsKey(key))
            runningQueue.remove(key);
    }

    /**
     * put a task message into queue. and waiting to be processed
     *
     * @param tm
     * @throws SystemException
     *         if can't put in the 'pendQueue'.
     */
    public static synchronized void putPending(TaskMessage tm) throws SystemException {
        if (logger.isInfoEnabled())
            logger.info("Puting a pending task {}", tm);
        try {
            if(getHolder()>= RunningStatus.limitTask){
                throw new SystemException(
                        "Task Queue is full,can not put in any tasks");
            }
            if (!pendingQueue.offer(tm, 1, TimeUnit.SECONDS)) {
                throw new SystemException(
                        "Task Queue is full,can not put in any tasks");
            }
        } catch (InterruptedException e) {
        }
    }

    /**
     * task a pending job.blocking if queue empty
     *
     * @return
     * @throws InterruptedException
     */
    public static TaskMessage takePending(long time) throws InterruptedException {
        if (logger.isInfoEnabled())
            logger.info("Requesting a pending task ");
        TaskMessage taskMessage;
        while(Objects.isNull(taskMessage=pendingQueue.poll(time,TimeUnit.SECONDS)));
//        for (; ; ) {
//            taskMessage = pendingQueue.poll(time, TimeUnit.SECONDS);
//            if (Objects.nonNull(taskMessage))
//                break;
//        }
        if (logger.isInfoEnabled())
            logger.info("Taking a pending task {}", taskMessage);
        return taskMessage;
    }

    public static List<String> getRunning() {
        List<String> running = new ArrayList<>();
        if (!runningQueue.isEmpty())
            running.addAll(runningQueue.keySet());
        return running;
    }

    public static int getRunningSize() {
        return runningQueue.size();
    }


}
