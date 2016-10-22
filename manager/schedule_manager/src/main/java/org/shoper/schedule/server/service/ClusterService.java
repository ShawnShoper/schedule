package org.shoper.schedule.server.service;

import org.shoper.common.rpc.common.URL;
import org.shoper.common.rpc.connector.Connector;
import org.shoper.common.rpc.manager.NodeManager;
import org.shoper.schedule.resp.StatusResponse;
import org.shoper.schedule.server.web.vo.ProviderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class ClusterService
{
	@Autowired
	NodeManager providerManager;
	public List<ProviderVO> getAllCluster()
	{
		ConcurrentHashMap<String, ConcurrentHashMap<String, Connector>> connections = providerManager
				.takeAllProvider();
		List<ProviderVO> providers = new ArrayList<>();
		for (String key : connections.keySet())
		{
			Map<String, Connector> connector = connections.get(key);
			for (String providerKey : connector.keySet())
			{
				ProviderVO provider = new ProviderVO();
				Connector thriftConnector = connector.get(providerKey);
				URL thriftURL = thriftConnector
						.getUrl();
				provider.setStatus(
						thriftConnector.isDisabled() ? "Disabled" : "Enabled");
				provider.setAddress(thriftURL.getHost() + ":"
						+ thriftURL.getPort());
				provider.setGroup(thriftURL.getGroup());
				provider.setProvideName(thriftURL.getClusterName());
				provider.setVersion(thriftURL.getVersion());
				StatusResponse sr = thriftConnector.getStatusResponse();
				if (sr != null)
				{
					provider.setCpuIdlePercent(sr.getCpuIdlePercent());
					provider.setMemUsedPercent(sr.getMemUsedPercent());
					provider.setServeTimes(sr.getServeTimes());
					provider.setStartTime(sr.getStartTime());
					provider.setHoldeCount(sr.getHoldeCount());
				}
				providers.add(provider);
			}
		}

		return providers;
	}
}
