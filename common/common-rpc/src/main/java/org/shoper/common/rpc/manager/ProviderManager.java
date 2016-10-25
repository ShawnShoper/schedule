package org.shoper.common.rpc.manager;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.apache.zookeeper.KeeperException;
import org.shoper.common.rpc.common.URL;
import org.shoper.common.rpc.common.URLBuilder;
import org.shoper.common.rpc.connector.Connector;
import org.shoper.commons.StringUtil;
import org.shoper.concurrent.future.AsynRunnable;
import org.shoper.concurrent.future.FutureManager;
import org.shoper.concurrent.future.RunnableCallBack;
import org.shoper.schedule.conf.ApplicationInfo;
import org.shoper.schedule.conf.ProviderInfo;
import org.shoper.schedule.conf.ZKInfo;
import org.shoper.schedule.exception.RPCConnectionException;
import org.shoper.schedule.manager.ZKModule;
import org.shoper.schedule.resp.StatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * ProviderQueue<br>
 * store provider ..<br>
 *
 * @author ShawnShoper
 */
@Component
public class ProviderManager extends ZKModule {
	//	@Autowired
//	LogModule logModule;
	static Logger logger = LoggerFactory.getLogger(ProviderManager.class);
	@Autowired
	ProviderInfo providerInfo;
	/**
	 * Has been exists provider collections. <br>
	 * connections key is group ID.
	 */
	private volatile ConcurrentSkipListMap<String, Map<String, Connector>> connections = new ConcurrentSkipListMap<>();
	/**
	 * Available provider queue.Priority. <br>
	 * Choose the highest priority Connectors<br>
	 * Details please see ThriftConnector method {@code compareTo}
	 */
	private volatile Map<String, PriorityBlockingQueue<Connector>> connectors = new ConcurrentHashMap<>();

	// Consistent hashing
	private TreeMap<Long, Connector> ch_connectors = new TreeMap<>();
	public volatile boolean isWaitting;
	Timer timer;
	@Autowired
	private ApplicationInfo application;
	@Autowired
	ZKInfo zkInfo;

	@PostConstruct
	public void init () {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		setZkInfo(zkInfo);
	}

	@Override
	public int start () {
		if (super.start() == 1)
			return 1;
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		timer = new Timer(true);
		// 扫描各个节点的状态检查心跳
		timer.schedule(new TimerTask() {
			@Override
			public void run () {
				try {
					checkHeartBeat();
				} catch (InterruptedException e) {
					timer.cancel();
				}
			}
		}, 0, application.getHeartBeat());
		return 0;
	}

	/**
	 * 检测心跳...
	 *
	 * @throws InterruptedException
	 */
	private void checkHeartBeat () throws InterruptedException {

		for (String key : connections.keySet()) {
			for (String token : connections.get(key).keySet()) {
				Connector tc = connections.get(key).get(token);
				// if (!tc.isDisabled())
				FutureManager.pushFuture(application.getName(),
										 "check-heatbeat-" + key, new AsynRunnable() {
							@Override
							public void call () throws Exception {
								StatusResponse statusResponse = null;
								// 3 times for check heartBeatByRegistry
								for (int i = 0; Objects.isNull(statusResponse)
										&& i < 3; i++) {
									try {
										//default first by registry,then p2p
										// 通过注册中心去取服务器状态
										statusResponse = getStatusRespByZK(
												tc.getUrl());
									} catch (Exception e) {
										// Connection failed by zookeeper
										// maybe network
										// error or provider down ,so switch
										// manual check by thrift direct
										// connect
									}

									if (Objects.isNull(statusResponse)) {
										try {
											statusResponse = tc
													.getThriftHandler()
													.getStatus();
										} catch (RPCConnectionException e) {
											// Connection failed by thrift
											// direct connect,maybe network
											// error or provider down.
										}
									}
								}
								if (statusResponse == null) {
									if (tc.isCheckHeartbeatByRegistry()) {
										tc.setCheckHeartbeatByRegistry(false);
									}
									tc.disabled();
								} else {
									tc.enabled();
									tc.setStatusResponse(statusResponse);
								}
							}
						}.setCallback(new RunnableCallBack() {

							@Override
							protected void fail (Exception e) {
								e.printStackTrace();
							}
						})
				);
			}
		}
	}

	/**
	 * 通过 ZK 注册中心获取 cluster status
	 *
	 * @param tc
	 * @return
	 * @throws InterruptedException
	 * @throws KeeperException
	 */
	public StatusResponse getStatusRespByZK (URL tc)
			throws InterruptedException, KeeperException {
		StatusResponse sr = null;
		String response = null;
		try {
			response = new String(
					super.getZkClient().showData(providerInfo.getNodePath()
														 + "/" + StringUtil.urlEncode(tc.getOriginPath())));
			if (StringUtil.isEmpty(response)) {
				throw new NullPointerException("");
			}
			sr = JSONObject.parseObject(response, StatusResponse.class);
		} catch (JSONException e) {
			e.printStackTrace();
			logger.info(
					"通过 ZK获取【" + tc.getHost() + ":" + tc.getPort() + "/"
							+ tc.getClusterName() + "】,解析 json 失败状态失败" + "\n"
							+ response
			);
		}
		return sr;
	}

	@PreDestroy
	public void destroy () {
		super.stop();
		connections.clear();
		if (timer != null) timer.cancel();
	}

	/**
	 * Pushing all provider into manager when application start up.
	 *
	 * @param nodes
	 * 		cluster 节点...
	 * @throws InterruptedException
	 */
	public void pushAllProvider (List<String> nodes) throws InterruptedException {
		for (String node : nodes) {
			try {
				addProvider(StringUtil.urlDecode(node));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * add new provider.reset status if exists<br>
	 * put to provider pool and offer to queue if not exists<br>
	 * modify checkHeartbeat way if exists<br>
	 *
	 * @param path
	 * 		of thrift provider addr
	 * @throws InterruptedException
	 */
	public void addProvider (String path) throws InterruptedException {

		URL tctmp = URLBuilder.Builder(URLBuilder.RPCType.Thrift).deBuild(path);
		String group = tctmp.getGroup();
		String token = tctmp.getToken();
		// In advance check provider's group has exists in connections
		{
			if (!connections.containsKey(group))
				connections.put(group, new HashMap<>());
			if (!connectors.containsKey(group))
				connectors.put(group, new PriorityBlockingQueue<>());
		}
		PriorityBlockingQueue<Connector> pbqs = connectors.get(group);
		Map<String, Connector> ctrs = connections.get(group);
		if (!ctrs.containsKey(token)) {
			logger.info(
					"Increased provider [" + tctmp + "]"
			);
			Connector thriftConnector = Connector.buider(tctmp);
			ctrs.put(token, thriftConnector);

			if (!pbqs.offer(thriftConnector, 1, TimeUnit.SECONDS)) {
				logger.warn(
						"Offer connector failed ,the queue is full..."
				);
			}
		} else {
			logger.info(
					"Update provider [" + tctmp + "]"
			);
			Connector tc = ctrs.get(token);
			if (!tc.isCheckHeartbeatByRegistry())
				tc.setCheckHeartbeatByRegistry(true);
			if (tc.isDisabled())
				ctrs.get(token).enabled();
			if (!pbqs.offer(tc, 1, TimeUnit.SECONDS)) {
				logger.warn(
						"Offer connector failed ,the queue is full..."
				);
			}
		}
	}

	/**
	 * 根据传递的 ThriftConnection 生成一个 key 作为 map 的 key
	 *
	 * @param tc
	 * @return
	 */
	private String buildKey (URL tc) {
		return tc.getHost() + tc.getPort() + tc.getVersion();
	}

	/**
	 * delete a provider.if some one down.
	 *
	 * @param path
	 * 		path of thrift provider addr
	 */
	public void deleteProvider (String path) {
		URL tc = URLBuilder.Builder(URLBuilder.RPCType.Thrift).deBuild(path);
		String group = tc.getGroup();
		String token = tc.getToken();
		if (connections.containsKey(group)) {
			Map<String, Connector> connectorGroup = connections
					.get(group);
			if (connectorGroup.containsKey(token)) {
				Connector connector = connectorGroup.get(token);
				connector.disabled();
				if (connectors.containsKey(group))
					if (connectors.get(group).contains(connector))
						connections.remove(connector);
			}
		}
	}

	/**
	 * get available provider
	 *
	 * @param timeout
	 * 		timeout
	 * @param unit
	 * 		time unit
	 * @param isTiming
	 * 		timing or not timing
	 * @return
	 * @throws InterruptedException
	 */
	public Connector getAvailableProvider (String group, int timeout,
												   TimeUnit unit, boolean isTiming) throws InterruptedException {
		//TODO 不用用队列的方式来做,因为每台机器不可能只处理一件事,应该根据对于的服务器状态处理
		Connector connector = null;
		if (!connectors.containsKey(group))
			connectors.put(group, new PriorityBlockingQueue<>());
		return connectors.get(group).poll(timeout, unit);
	}

	public Map<String, Map<String, Connector>> takeAllProvider () {
		return this.connections;
	}

	/**
	 * 还回 cluster
	 *
	 * @param group
	 * @param providerToken
	 * @throws InterruptedException
	 */
	public void putBack (String group, String providerToken)
			throws InterruptedException {
		if (StringUtil.isEmpty(group) || StringUtil.isEmpty(providerToken))
			return;
		if (connections.containsKey(group)) {
			Map<String, Connector> connctor = connections.get(group);
			Connector thriftConnector = connctor.get(providerToken);
			logger.info("Provider ["
								+ thriftConnector.getUrl() + "] 执行完毕，重新待命...");
			if (connectors.containsKey(group))
				while (!connectors.get(group).offer(thriftConnector, 2,
													TimeUnit.SECONDS
				)) ;
		}
	}
}
