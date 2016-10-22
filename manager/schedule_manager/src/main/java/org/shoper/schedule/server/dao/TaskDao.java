package org.shoper.schedule.server.dao;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.shoper.commons.StringUtil;
import org.shoper.schedule.pojo.Task;
import org.shoper.schedule.pojo.TaskStatus;
import org.shoper.schedule.pojo.TaskTemplate;
import org.shoper.schedule.server.pojo.NotifyTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class TaskDao {
    //	@Autowired
//	private MongoModule mongo;
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 获取指定时间段之前的非定时任务
     *
     * @param interval
     *         时间区间
     * @param unit
     *         时间单位
     * @param isTiming
     *         是否定时,null 无条件，true 定时，false 非定时
     * @param isTiming
     *         是否禁用,null
     *         无条件， true 禁用，false 非禁用
     * @return 非定时任务
     */
    public List<Task> getTask(int interval, TimeUnit unit, Boolean isTiming) {
        long diff = unit.toMillis(interval);
        Date condition = new Date(System.currentTimeMillis() - diff);
        Criteria criteria = Criteria.where("disabled").is(false);

        if (!isTiming.booleanValue())
            criteria.and("lastFinishTime").lte(condition.getTime());
        criteria.and("timing").is(isTiming);
        Query query = Query.query(criteria);
        List<Task> tasks = mongoTemplate.find(query, Task.class);
        return tasks;
    }

    /**
     * 获取定时任务..所有的..
     *
     * @return 定时任务集合
     */
    public List<Task> getTimingTask() {
        List<Task> tasks = mongoTemplate.find(
                Query.query(
                        Criteria.where("disabled").is(false).and("timing").is(true)),
                Task.class
        );
        return tasks;
    }

    public TaskTemplate getTaskTemplate(String templateID) {
        return mongoTemplate.findOne(
                Query.query(Criteria.where("_id").is(templateID)),
                TaskTemplate.class
        );
    }

    /**
     * 获取 所有task 分页
     *
     * @param offset
     *         起始位置
     * @param limit
     *         每页显示
     * @return
     */
    public List<Task> getTask(int offset, int limit, String search, String sort,
                              String order) {
        Query query = new Query();
        if (!StringUtil.isEmpty(sort) && !StringUtil.isEmpty(order))
            query.with(new Sort(Direction.fromString(order), sort));
        if (search != null)
            query.addCriteria(
                    Criteria.where("name").regex(".*?" + search + ".*"));
        query.skip(offset);
        query.limit(limit);
        return mongoTemplate.find(query, Task.class);
    }

    /**
     * 获取 所有task template 分页
     *
     * @param offset
     *         其实位置
     * @param limit
     *         每页显示
     * @return
     */
    public List<TaskTemplate> getTaskTemplate(int offset, int limit,
                                              String search, String sort, String order) {
        DBObject fieldObject = new BasicDBObject();
        fieldObject.put("code", false);
        Criteria criteria = new Criteria();
        criteria.and("removed").is(0);
        Query q = new BasicQuery(new BasicDBObject(), fieldObject);
        if (!StringUtil.isEmpty(search))
            criteria.and("name").regex(".*?" + search + ".*");
        if (sort != null && order != null)
            q.with(new Sort(Direction.fromString(order), sort));
        q.skip(offset).limit(limit);
        q.addCriteria(criteria);

        return mongoTemplate.find(q, TaskTemplate.class);
    }

    public long getTaskSize(String search) {
        Criteria criteria = new Criteria();
        // .where("disabled").is(false);
        if (search != null)
            criteria.and("name").regex(".*?" + search + ".*");
        Query query = Query.query(criteria);
        return mongoTemplate.count(query, Task.class);
    }

    public long getTaskTemplateSize(String search) {
        Criteria criteria = new Criteria();// .where("disabled").is(false);
        if (search != null)
            criteria.and("name").regex(".*?" + search + ".*");
        criteria.and("removed").is(0);
        Query query = Query.query(criteria);
        return mongoTemplate.count(query, TaskTemplate.class);
    }

    /**
     * 翻转状态..
     *
     * @param id
     *         id
     * @param value
     *         值
     * @param type
     *         类型
     * @return
     */
    public void inverseStatus(String id, boolean value, int type) {
        Query query = Query.query(Criteria.where("_id").is(id));
        Update update = new Update();
        update.set("disabled", !value);
        mongoTemplate.updateFirst(query, update,
                                  type == 0 ? TaskTemplate.class : Task.class
        );
    }

    public void addTaskTemplate(TaskTemplate taskTemplate) {
        mongoTemplate.save(taskTemplate);
    }

    public boolean taskTemplateHasExists(String name) {
        return mongoTemplate.exists(
                Query.query(Criteria.where("name").is(name)),
                TaskTemplate.class
        );
    }

    public boolean taskTemplateHasExistsByID(String templateID) {
        return mongoTemplate.exists(
                Query.query(Criteria.where("_id").is(templateID)),
                TaskTemplate.class
        );
    }

    public void pushNotifyTask(NotifyTask notifyTask) {
        Update update = new Update();
        update.set("domain", notifyTask.getDomain());
        update.set("type", notifyTask.getType());
        update.set("notify", false);
        mongoTemplate
                .upsert(Query.query(
                        Criteria.where("domain").is(notifyTask.getDomain())),
                        update, NotifyTask.class
                );
    }

    public NotifyTask notifyTaskInfo(String domain) {
        if (mongoTemplate.exists(
                Query.query(Criteria.where("domain").is(domain)),
                NotifyTask.class
        )) {
            return mongoTemplate
                    .find(
                            Query.query(Criteria.where("domain").is(domain)),
                            NotifyTask.class
                    )
                    .get(0);
        }
        return null;
    }

    public void taskDone(String job, long endTime, AtomicLong saveCount) {
        Update update = new Update();
        update.inc("loops", 1);
        update.set("lastFinishTime", System.currentTimeMillis());
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(job)), update, Task.class);
    }

    public Task getTaskByID(String job) {
        return mongoTemplate.findOne(
                Query.query(Criteria.where("_id").is(job)), Task.class);
    }

    public void taskFailed(String job) {
        Update update = new Update();
        update.inc("failedCount", 1);
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(job)), update, Task.class);
    }

    public void disabledTask(String job) {
        Update update = new Update();
        update.set("disabled", true);
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(job)), update, Task.class);
    }

    public void addTask(Task task) {
        mongoTemplate.save(task);
    }

    public boolean taskHasExistsByID(String id) {
        return mongoTemplate
                .exists(Query.query(Criteria.where("_id").is(id)), Task.class);
    }

    public void deleteTask(String id) {
        mongoTemplate
                .remove(Query.query(Criteria.where("_id").is(id)), Task.class);
    }

    public void deleteTaskTemp(String id) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(id)),
                Update.update("removed", 1), TaskTemplate.class
        );
    }

    public String getTaskGroup(String id) {
        return mongoTemplate.findById(id, TaskTemplate.class)
                .getGroup();
    }

    public void editTask(Task task) {
        mongoTemplate.save(task);
    }

    public boolean updateTaskStatus(String token) {
        TaskStatus taskStatus = mongoTemplate.findById(
                token,
                TaskStatus.class
        );
        if (!StringUtil.isNull(taskStatus))
            mongoTemplate.remove(
                    Query.query(Criteria.where("_id").is(token)),
                    TaskStatus.class
            );
        return false;
    }

    public Task getTaskByName(String name) {

        return mongoTemplate.findOne(
                Query.query(Criteria.where("name").is(name)), Task.class);
    }

    public List<Task> getTaskByTemplateID(String templateID) {
        return mongoTemplate.find(
                Query.query(Criteria.where("templateID").is(templateID)),
                Task.class
        );
    }

    public TaskTemplate getTaskTemplateByID(String templateID) {
        List<TaskTemplate> datas = mongoTemplate.find(
                Query.query(Criteria.where("_id").is(templateID)),
                TaskTemplate.class
        );
        if (Objects.isNull(datas) || datas.isEmpty())
            return null;
        else
            return datas.get(0);
    }

    public void editTaskTemplate(TaskTemplate taskTemplate) {
        mongoTemplate.save(taskTemplate);
    }

}
