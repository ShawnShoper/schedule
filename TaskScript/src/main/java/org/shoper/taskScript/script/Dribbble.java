package org.shoper.taskScript.script;

import org.shoper.http.httpClient.HttpClient;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.job.JobCaller;

/**
 * Created by ShawnShoper on 16/8/8.
 */
public class Dribbble extends JobCaller{
    @Override
    protected JobResult call() throws SystemException, InterruptedException {
        System.out.println("....");
        return result;
    }

    @Override
    protected boolean checkArgs(JobParam args) {
        System.out.println("vilidate params");
        return true;
    }

    @Override
    public void destroy() {
        System.out.println("destroy...");
    }
}
