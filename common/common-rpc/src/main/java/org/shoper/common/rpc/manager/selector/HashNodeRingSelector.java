package org.shoper.common.rpc.manager.selector;

import org.shoper.common.rpc.common.URL;
import org.shoper.common.rpc.connector.Connector;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实现一套 hash 环 来做负载均衡
 *
 * @author ShawnShoper
 * @date 2016/10/18
 * @sice
 */
@Component
public class HashNodeRingSelector {
    /**
     * 所有Connector 集合处,一个 hash环, key为筛选条件 hash.value为 hash环
     */
    private int VIRTUALNODES = 126;
    private Map<Long, TreeMap<Long, Connector>> nodes = new ConcurrentHashMap<>();

    /**
     * add new Node into nodes
     *
     * @param connector
     */
    public void addNode(Connector connector) {
        long hashKey = computeRootNodeHashKey(connector);
        if (!nodes.containsKey(hashKey))
            nodes.put(hashKey, new TreeMap<>());
        TreeMap<Long, Connector> treeMap = nodes.get(hashKey);
        URL url = connector.getUrl();
        for (int i = 0; i < VIRTUALNODES; i++)
            treeMap.put(ConsistencyHash.hash(url.getHost() + url.getPort() + i), connector);
    }

    /**
     * del not available node
     *
     * @param con
     * @return
     */
    public boolean delNode(Connector con) {
        long hashKey = computeRootNodeHashKey(con);
        if (nodes.containsKey(hashKey)) {
            Map<Long, Connector> map = nodes.get(hashKey);
            URL url = con.getUrl();
            for (int i = 0; i < VIRTUALNODES; i++)
                map.remove(ConsistencyHash.hash(url.getHost() + url.getPort() + i));
        }
        return false;
    }

    /**
     * 获取子节点下的Connector
     *
     * @param treeMap
     * @return Selector
     */
    private SelectorResult select(TreeMap<Long, Connector> treeMap) {
        if (treeMap.isEmpty())
            return null;
        SortedMap<Long, Connector> tailMap = treeMap.tailMap(ConsistencyHash.hash("" + ran.nextInt(VIRTUALNODES)));
        long key = tailMap.isEmpty() ? treeMap.firstKey() : tailMap.firstKey();
        Connector connector = treeMap.get(key);
        SelectorResult selectorResult = new SelectorResult();
        selectorResult.setConnector(connector);
//        Collection<Connector> temp = new ArrayList<>();
//        temp.addAll(treeMap.values());
//        treeMap.values().forEach(temp::addAll);
//        temp.remove(connector);
//        Collection<Connector> copy = ObjectUtil.depthClone(connectors, Collection.class);
//        tre.remove(connector);
        selectorResult.setConnectors(treeMap.values());
        selectorResult.setConnector(connector);
        return selectorResult;
    }

    /**
     * Random builder
     */
    private Random ran = new Random();

    /**
     * 通过某个条件筛选,返回一组适配的集合
     *
     * @param group
     * @param version
     * @param name
     * @return
     */
    public SelectorResult select(String group, String version, String name) {
        Long hashKey = computeRootNodeHashKey(group, version, name);
        SelectorResult selector = null;
        if (nodes.containsKey(hashKey)) {
            TreeMap treeMap = nodes.get(hashKey);
            if (!treeMap.isEmpty())
                selector = select(treeMap);
        }
        return selector;
    }


    /**
     * del not available nodes
     *
     * @param urls
     */
    public void delNodes(Collection<Connector> urls) {
        urls.forEach(this::delNode);
    }

    /**
     * Compute Root node HashKey by url
     *
     * @param connector
     * @return
     */
    private long computeRootNodeHashKey(Connector connector) {
        URL url = connector.getUrl();
        String group = url.getGroup();
        String version = url.getVersion();
        String name = url.getClusterName();
        return computeRootNodeHashKey(group, version, name);
    }

    private long computeRootNodeHashKey(String group, String version
            , String name) {
        return ConsistencyHash.hash(group + version + name);
    }

}
