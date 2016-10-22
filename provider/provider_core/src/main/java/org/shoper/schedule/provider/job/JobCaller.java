package org.shoper.schedule.provider.job;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.job.JobParam;
import org.shoper.schedule.job.JobResult;
import org.shoper.schedule.provider.module.HDFSModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Map;

/**
 * 任务继承类
 *
 * @author ShawnShoper
 */
public abstract class JobCaller {
	// 不同的网站...
	// 不同网站字段多样化的。

	/**
	 * 任务参数
	 */
	protected JobParam args;
	/**
	 * HDFS 文件系统储存模块
	 */
	protected HDFSModule hdfsModule;
	/**
	 * 任务结果
	 */
	protected JobResult result = new JobResult();

	/**
	 * 获取 HDFS 文件系统操作模块<br>
	 * Created by ShawnShoper Apr 14, 2016
	 *
	 * @return
	 */
	protected HDFSModule getHdfsModule () {
		return hdfsModule;
	}

	public void setHdfsModule (HDFSModule hdfsModule) {
		this.hdfsModule = hdfsModule;
	}
	// 这里使用 JobCaller.this.getClass 而不是直接 JobCaller.class的原因是，子类的使用 log
	/**
	 * Logger 日志模块
	 */
	protected Logger logger = LoggerFactory
			.getLogger(JobCaller.this.getClass());

	/**
	 * 获取 mongo 数据库操作模块<br>
	 * Created by ShawnShoper Apr 14, 2016
	 *
	 * @return
	 */
	public JobResult run () throws SystemException, InterruptedException {
		try {

			call();
		} catch (SystemException e) {
			e.printStackTrace();
			throw e;
		} finally {
			complete();
		}

		return result;
	}

	/**
	 * 任务完成日志输出方法<br>
	 * Created by ShawnShoper Apr 14, 2016
	 */
	private void complete () {
		logger.info(
				"Task over ,summary:total amount:[{}],update amount:[{}],new add amount:[{}]",
				(result.getSaveCount().get() + result.getUpdateCount().get()),
				result.getUpdateCount().get(), result.getSaveCount().get()
		);
	}

	/**
	 * 任务处理方法,必须实现。
	 *
	 * @return
	 * @throws InterruptedException
	 */
	protected abstract JobResult call ()
			throws SystemException, InterruptedException;

	/**
	 * 自定义任务参数过滤器
	 *
	 * @param jobParam
	 * @return
	 * @throws SystemException
	 */
	protected JobParam customInit (JobParam jobParam) throws SystemException {
		return jobParam;
	}

	/**
	 * 初始化提供任务框架调用。
	 *
	 * @param args
	 */
	public final boolean init (JobParam args) {
		if (args == null)
			return false;
		if (!checkArgs(args))
			return false;
		try {
			this.args = customInit(args);
		} catch (SystemException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 自定义检测参数<br>
	 * Created by ShawnShoper Apr 14, 2016
	 *
	 * @param args
	 * @return
	 */
	protected abstract boolean checkArgs (JobParam args);

	public JobResult getJobResult () {
		return result;
	}

	public JobParam getArgs () {
		return args;
	}

	public abstract void destroy ();

	/**
	 * Saving data
	 *
	 * @param datas
	 * @return New add amount<br>
	 * the update amount equals datas.size minus the result
	 */
	protected int saveDatas (List<Map<String, Object>> datas) {
		return saveDatas(datas, "datas");
	}

	/**
	 * 保存数据并指定存储表<br>
	 * Created by ShawnShoper Apr 14, 2016
	 *
	 * @param datas
	 * @param cname
	 * @return
	 */
	@Autowired
	MongoTemplate mongoTemplate;

	protected int saveDatas (final List<Map<String, Object>> datas, String cname) {
		DBCollection collection = mongoTemplate.getDb()
				.getCollection(cname);
		int newAddAmount = 0;
		if (datas != null && datas.size() > 0) {
			for (Map<String, Object> map : datas) {
				if (!map.containsKey("id"))
					throw new IllegalArgumentException(
							"Saving data must be container key [id]");
				DBObject query = new BasicDBObject("id", map.get("id"));
				DBObject update = new BasicDBObject(map);
				WriteResult wr = collection.update(query, update, true, false);
				if (!wr.isUpdateOfExisting())
					++newAddAmount;
				else {
					System.out.println("The id [{" + map.get("id") + "}] 's data is exists.name is {" + map.get("name") + "}");
				}
			}
		}
		result.getSaveCount().addAndGet(newAddAmount);
		result.getUpdateCount().addAndGet(datas.size() - newAddAmount);
		return newAddAmount;
	}

}
