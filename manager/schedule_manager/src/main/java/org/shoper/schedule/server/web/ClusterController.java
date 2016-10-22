package org.shoper.schedule.server.web;


import org.shoper.schedule.server.service.ClusterService;
import org.shoper.schedule.server.web.response.BootstrapTableResponse;
import org.shoper.schedule.server.web.vo.ProviderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cluster")
public class ClusterController
{
	@Autowired
	ClusterService clusterService;
	@RequestMapping("/getInfo")
	public BootstrapTableResponse<ProviderVO> getCluster()
	{
		BootstrapTableResponse<ProviderVO> bootstrapTableResponse = new BootstrapTableResponse<ProviderVO>();
		List<ProviderVO> providerVOs = clusterService.getAllCluster();
		bootstrapTableResponse.setRows(providerVOs);
		bootstrapTableResponse.setTotal(providerVOs.size());
		return bootstrapTableResponse;
	}
}
