import org.junit.Test;
import org.shoper.common.rpc.manager.NodeProxy;

/**
 * Created by ShawnShoper on 2016/10/21.
 */
public class NodeProxyTests {
    @Test
    public void test() {
        NodeProxy proxy = new NodeProxy();
        //通过生成子类的方式创建代理类
        Test1 proxyImp = (Test1) proxy.getProxy(Test1.class);
        proxyImp.say();
    }
}
