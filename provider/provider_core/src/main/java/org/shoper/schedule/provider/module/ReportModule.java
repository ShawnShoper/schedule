package org.shoper.schedule.provider.module;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.shoper.common.rpc.common.URL;
import org.shoper.common.rpc.common.URLBuilder;
import org.shoper.concurrent.future.AsynCallable;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.schedule.conf.ApplicationInfo;
import org.shoper.schedule.conf.ZKInfo;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.manager.ZKModule;
import org.shoper.schedule.provider.handle.MasterConnector;
import org.shoper.schedule.provider.job.queue.ReportQueue;
import org.shoper.schedule.resp.ReportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URLDecoder;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * 任务报告模块<br>
 * 用于执行任务关闭后的报告信息..
 *
 * @author ShawnShoper
 */
@Component
public class ReportModule extends ZKModule {
	private MasterConnector masterConnector = null;

	public MasterConnector getMasterConnector () {
		return masterConnector;
	}

	Logger LOGGER = LoggerFactory.getLogger(ReportModule.class);
	@Autowired
	ApplicationInfo appInfo;
	@Autowired
	ZKInfo zkInfo;

	@PostConstruct
	public void init () {
		setZkInfo(zkInfo);
	}

	@Override
	public int start () {
		try {
			if (super.start() == 1)
				return 1;
			if (startReport() == 1)
				delayInit();

		} catch (InterruptedException | SystemException e) {
			return 1;
		}
		return 0;
	}

	public int startReport () throws SystemException, InterruptedException {
		masterConnector = initMaster();
		if (Objects.isNull(masterConnector)) {
			return 1;
		}
		startReportDaemon();
		setStarted(true);
		return 0;
	}

	void delayInit () throws InterruptedException, SystemException {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run () {
				try {
					while (startReport() != 0)
						TimeUnit.SECONDS.sleep(5);
				} catch (SystemException e1) {
					e1.printStackTrace();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}, 2000);
	}

	/**
	 * 启动任务汇报线程...
	 */
	private void startReportDaemon () {
		FutureManager.pushFuture(appInfo.getName(), "report",
								 new AsynCallable<Boolean, Object>() {

									 @Override
									 public Boolean run (Object params) throws Exception {
										 for (; ; ) {
											 ReportResponse report = ReportQueue.takeReport();
											 try {
												 masterConnector.getThriftHandler()
														 .report(report.toJson());
											 } catch (SystemException e) {
												 // 自行处理 SystemException
												 LOGGER.info(
														 "[ReportModule]:任务汇报失败,重入汇报队列,等待下次汇报...");
												 ReportQueue.putReport(report);
											 }
										 }
									 }
								 }
		);
	}

	/**
	 * 扫描provider
	 *
	 * @throws InterruptedException
	 * @throws SystemException
	 */
	private MasterConnector initMaster ()
			throws InterruptedException, SystemException {
		MasterConnector masterConnector = null;
		try {
			byte[] data = super.getZkClient().showData(appInfo.getMasterPath());
			if (data == null)
				LOGGER.info(
						"[ReportModule]:未获取调度端节点数据...可能原因:调度端未启动...网络不通...");
			String master = new String(data);
			URL tc = URLBuilder.Builder(URLBuilder.RPCType.Thrift)
					.deBuild(URLDecoder.decode(master));
			masterConnector = initConnector(tc);
		} catch (KeeperException e) {
			LOGGER.info(e.getMessage());
			//e.printStackTrace();// Do nothing...
		}
		return masterConnector;
	}

	MasterConnector initConnector (URL tc) throws SystemException {
		LOGGER.info("[ReportModule]:检查调度端是否可用...");
		MasterConnector mc = MasterConnector.buider(tc);
		try {
			mc.getThriftHandler().check();
		} catch (SystemException e) {
			LOGGER.info("[ReportModule]:调度端连接失败.", e);
			throw new SystemException(e.getLocalizedMessage());
		}
		return mc;
	}

	@Override
	public void dataChangeProcess (WatchedEvent event) {
		try {
			masterConnector = initMaster();
		} catch (InterruptedException e) {
			// Do nothing...
		} catch (SystemException e) {
			e.printStackTrace();
		}
	}

}
