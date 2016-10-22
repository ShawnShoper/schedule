import org.junit.Test;
import org.shoper.common.rpc.common.URL;
import org.shoper.common.rpc.connector.Connector;
import org.shoper.common.rpc.manager.selector.HashNodeRing;
import org.shoper.common.rpc.manager.selector.Selector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.shoper.common.rpc.connector.Connector.buider;

/**
 * @author ShawnShoper
 * @date 2016/10/17
 * @sice
 */
public class NodeManager_Tests {
    HashNodeRing nodeManager = new HashNodeRing();

    @Test
    public void HashNodeRing() throws InterruptedException, IOException {

        {
            URL url = new URL();
            url.setHost("192.168.100.1");
            url.setPort(8881);
            url.setVersion("1.2.0");
            url.setClusterName("test");
            nodeManager.addNode(buider(url));
        }
        {
            URL url = new URL();
            url.setHost("192.168.100.2");
            url.setPort(8882);
            url.setVersion("1.2.0");
            url.setClusterName("test");
            nodeManager.addNode(buider(url));
        }
        {
            URL url = new URL();
            url.setHost("192.168.100.3");
            url.setPort(8883);
            url.setVersion("1.2.0");
            url.setClusterName("test");
            nodeManager.addNode(buider(url));
        }
        {
            URL url = new URL();
            url.setHost("192.168.100.4");
            url.setPort(8884);
            url.setVersion("1.2.0");
            url.setClusterName("test");
            nodeManager.addNode(buider(url));
        }
        {
            URL url = new URL();
            url.setHost("192.168.100.5");
            url.setPort(8885);
            url.setVersion("1.2.0");
            url.setClusterName("test");
            nodeManager.addNode(buider(url));
        }
        {
            URL url = new URL();
            url.setHost("192.168.100.6");
            url.setPort(8886);
            url.setVersion("1.2.0");
            url.setClusterName("test");
            nodeManager.addNode(buider(url));
        }
        {
            URL url = new URL();
            url.setHost("192.168.100.7");
            url.setPort(8886);
            url.setVersion("1.2.0");
            url.setClusterName("test");
            nodeManager.addNode(buider(url));
        }
        {
            URL url = new URL();
            url.setHost("192.168.100.8");
            url.setPort(8886);
            url.setVersion("1.2.0");
            url.setClusterName("test");
            nodeManager.addNode(buider(url));
        }
        System.out.println("第一次新增");
        print();
        System.out.println("删除后");
        {
            URL url = new URL();
            url.setHost("192.168.100.6");
            url.setPort(8886);
            url.setVersion("1.2.0");
            url.setClusterName("test");
            nodeManager.delNode(buider(url));
        }
        print();
        System.out.println("第二次新增");
        {
            URL url = new URL();
            url.setHost("192.168.100.6");
            url.setPort(8886);
            url.setVersion("1.2.0");
            url.setClusterName("test");
            nodeManager.addNode(buider(url));
        }
        {
            URL url = new URL();
            url.setHost("192.168.100.9");
            url.setPort(8886);
            url.setVersion("1.2.0");
            url.setClusterName("test");
            nodeManager.addNode(Connector.buider(url));
        }
        print();
    }

    public void print() {
        {
            Map<Object, AtomicInteger> result = new HashMap<>();
            IntStream.iterate(0, n -> ++n).limit(1000000).mapToObj(n -> {
                Selector selector = nodeManager.select("DEFAULT_GROUP", "1.2.0", "test");
                if (Objects.isNull(selector)) return null;
                return selector.getConnector().getUrl().getHost();
            }).forEach(j -> {
                if (result.containsKey(j)) {
                    result.get(j).incrementAndGet();
                } else {
                    result.put(j, new AtomicInteger(1));
                }
            });
            result.forEach((k, v) -> System.out.println(k + "--" + v));
        }
    }
}
