package org.shoper.schedule.server.listener;

import org.shoper.common.rpc.manager.NodeManager;
import org.shoper.common.rpc.manager.NodeScanner;
import org.shoper.schedule.SystemContext;
import org.shoper.schedule.conf.ZKInfo;
import org.shoper.schedule.server.job.TaskManager;
import org.shoper.schedule.server.module.MailModule;
import org.shoper.schedule.server.module.Registrar;
import org.shoper.schedule.server.module.schedule.TaskSchedule;
import org.shoper.schedule.server.module.sub.TaskProgressLog;
import org.shoper.schedule.server.web.TaskController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;


/**
 * spring context started listener...
 *
 * @author ShawnShoper
 */
@Component
public class StartedListener
        implements
        ApplicationListener<ContextRefreshedEvent> {
    @Autowired
    ZKInfo zkInfo;

    @Autowired
    NodeScanner providerScanner;
    @Autowired
    TaskManager taskManager;
    @Autowired
    TaskSchedule taskSchedule;
    @Autowired
    Registrar registrar;
    @Autowired
    NodeManager providerManager;
    @Autowired
    MailModule mailModule;
    @Autowired
    TaskProgressLog taskProgressLog;
    @Autowired
    TaskController taskController;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            // start master module
            registrar.start();
            // start reids log module
//            logModule.start();
            // start providerScanner module
            providerScanner.start();
            providerManager.start();
            // task progress log
            taskProgressLog.start();
            // start mongo database operate module
            // 邮件系统
            mailModule.start();
            // mongo 数据库模块
            // start task management module
            taskManager.fire();
            // start schedule module
            taskSchedule.fire();
        } catch (Exception e) {
            e.printStackTrace();
            SystemContext.shutdown();
        } finally {
        }
    }
}
