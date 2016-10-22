package org.shoper.schedule.provider.module;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.shoper.common.rpc.common.URL;
import org.shoper.common.rpc.common.URLBuilder;
import org.shoper.schedule.conf.Master;
import org.shoper.schedule.conf.ZKInfo;
import org.shoper.schedule.manager.ZKModule;
import org.shoper.schedule.provider.handle.MasterConnector;
import org.shoper.schedule.provider.handle.MasterHandler;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PreDestroy;

//@Component
public class MasterDiscover extends ZKModule {
	@Autowired
	ZKInfo zkInfo;
	@Autowired
	Master master;
	@Autowired
	MasterConnector masterConnector;

	public void init () {
		super.setZkInfo(zkInfo);
	}

	@Override
	public void stop () {
		super.stop();
	}

	@Override
	public int start () {
		// 获取 master 节点...
		if (super.start() == 1)
			return 1;
		fetchMasterInfo();
		setStarted(true);
		return 0;
	}

	/**
	 * 加载 masterinfo
	 *
	 * @throws InterruptedException
	 * @throws KeeperException
	 */
	private void fetchMasterInfo () {
		byte[] b;
		try {
			b = super.getZkClient().showData(master.getNodePath());
			if (b != null && b.length > 0) {
				String data = new String(b);
				// assignment to masterInfo for spring bean...
				URL thriftConnection = URLBuilder.Builder(URLBuilder.RPCType.Thrift)
						.deBuild(data);
				// masterConnector.setThriftConnection(thriftConnection);
				MasterHandler thriftHandler = new MasterHandler(
						thriftConnection);
				// check the master is available..
				thriftHandler.connect();
				masterConnector.enabled();
				// fetch data over...
			}
		} catch (Exception e) {
			// 一旦失败,那么则判断未 写入的数据不正确或者机器配置错误...
			masterConnector.disabled();
		}
	}

	@Override
	public void dataChangeProcess (WatchedEvent event) {
		fetchMasterInfo();
		super.dataChangeProcess(event);
	}

	@Override
	public void nodeDeleteProcess (WatchedEvent event) {
		masterConnector.disabled();
		super.nodeDeleteProcess(event);
	}

	@PreDestroy
	public void destroy () {
		stop();
	}
}
