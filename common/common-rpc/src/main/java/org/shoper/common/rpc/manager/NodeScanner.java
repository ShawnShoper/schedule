package org.shoper.common.rpc.manager;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.shoper.schedule.conf.ProviderInfo;
import org.shoper.schedule.conf.ZKInfo;
import org.shoper.schedule.manager.ZKModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

@Component
public class NodeScanner extends ZKModule {
	//	@Autowired
//	LogModule logModule;
	static Logger logger = LoggerFactory.getLogger(NodeScanner.class);
	@Autowired
	private ZKInfo zkInfo;
	@Autowired
	private ProviderInfo providerInfo;
	@Autowired
	private NodeManager providerManager;

	@PostConstruct
	public void init () {
		setZkInfo(zkInfo);
	}

	@Override
	public int start () {
		if (super.start() == 1)
			return 1;
		try {
			scanner();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return 1;
		}
		setStarted(true);
		return 0;
	}

	/**
	 * 扫描provider
	 *
	 * @throws InterruptedException
	 */
	private void scanner () throws InterruptedException {
		logger.info("开始扫描 provider");
		try {
			List<String> nodes = super.getZkClient()
					.getChildren(providerInfo.getNodePath(), true);
			logger.info(
					"发现provider个数[" + nodes.size() + "]");
			providerManager.pushAllProvider(nodes);
		} catch (KeeperException e) {
			e.printStackTrace();
			scanner();
			// Timer timer = new Timer();
			// timer.schedule(new TimerTask() {
			// @Override
			// public void run()
			// {
			// try
			// {
			// scanner();
			// } catch (InterruptedException e)
			// {
			// ;
			// }
			// timer.cancel();
			// }
			// }, 0);
		}
	}

	@PreDestroy
	public void destroy () {
		super.getZkClient().close();
	}

	@Override
	public void childrenNodeChangeProcess (WatchedEvent event) {
		try {
			scanner();
		} catch (InterruptedException e) {
			;
		}
	}
}
