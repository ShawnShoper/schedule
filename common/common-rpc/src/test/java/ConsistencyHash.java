import org.shoper.commons.MD5Util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by IntelliJ IDEA.
 * User: test
 * Date: 12-5-24
 * Time: 下午5:37
 * To change this template use File | Settings | File Templates.
 */
public class ConsistencyHash {
	private TreeMap<Long, Object> nodes = null;
	//真实服务器节点信息
	private List<Object> shards = new ArrayList();
	//设置虚拟节点数目
	private int VIRTUAL_NUM = 6;

	/**
	 * 初始化一致环
	 */
	public void init () {
		shards.add("192.168.0.0-服务器0");
		shards.add("192.168.0.1-服务器1");
		shards.add("192.168.0.2-服务器2");
		shards.add("192.168.0.3-服务器3");
		shards.add("192.168.0.4-服务器4");
		shards.add("192.168.0.5-服务器5");
		shards.add("192.168.0.6-服务器6");
		shards.add("192.168.0.7-服务器7");

		nodes = new TreeMap<>();
		for (int i = 0; i < shards.size(); i++) {
			Object shardInfo = shards.get(i);
			for (int j = 0; j < VIRTUAL_NUM; j++) {
				nodes.put(hash(computeMd5(i + "" + j), j), shardInfo);
			}
		}
		System.out.println();
	}

	/**
	 * 根据key的hash值取得服务器节点信息
	 *
	 * @param hash
	 * @return
	 */
	public Object getShardInfo (long hash) {
		Long key = hash;
		SortedMap<Long, Object> tailMap = nodes.tailMap(key);
		if (tailMap.isEmpty()) {
			key = nodes.firstKey();
		} else {
			key = tailMap.firstKey();
		}
		return nodes.get(key);
	}

	/**
	 * 打印圆环节点数据
	 */
	public void printMap () {
		System.out.println(nodes);
	}

	/**
	 * 根据2^32把节点分布到圆环上面。
	 *
	 * @param digest
	 * @param nTime
	 * @return
	 */
	public long hash (byte[] digest, int nTime) {
		long rv = ((long) (digest[3 + nTime * 4] & 0xFF) << 24)
				| ((long) (digest[2 + nTime * 4] & 0xFF) << 16)
				| ((long) (digest[1 + nTime * 4] & 0xFF) << 8)
				| (digest[0 + nTime * 4] & 0xFF);
		return rv & 0xffffffffL; /* Truncate to 32-bits */
	}

	char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	/**
	 * Get the md5 of the given key.
	 * 计算MD5值
	 */
	public byte[] computeMd5 (String k) {

//		MessageDigest md5;
//		try {
//			md5 = MessageDigest.getInstance("MD5");
//		} catch (NoSuchAlgorithmException e) {
//			throw new RuntimeException("MD5 not supported", e);
//		}
//		md5.reset();
//		byte[] keyBytes = null;
//		try {
//			keyBytes = k.getBytes("UTF-8");
//		} catch (UnsupportedEncodingException e) {
//			throw new RuntimeException("Unknown string :" + k, e);
//		}
//
//		md5.update(keyBytes);
//		return md5.digest();
		return MD5Util.GetMD5Code(k).getBytes();
	}

	public static void main (String[] args) {

		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("MD5 not supported", e);
		}
		md5.reset();
		byte[] keyBytes = null;
		try {
			keyBytes = "...asdsdas".getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unknown string :" + "...", e);
		}

		md5.update(keyBytes);
		System.out.println(md5.digest().length);


		Random ran = new Random();
		ConsistencyHash hash = new ConsistencyHash();
		hash.init();
		hash.printMap();
		//循环50次，是为了取50个数来测试效果，当然也可以用其他任何的数据来测试
//		for (int i = 0; i < 50; i++) {
//			long hashcode = hash.hash(hash.computeMd5(String.valueOf(i)), ran.nextInt(hash.VIRTUAL_NUM));
//			System.out.println(hash.getShardInfo(hashcode));
//		}
		Map<String, AtomicInteger> result = new HashMap<>();
		IntStream.iterate(0, n -> ++n).limit(1000).mapToObj(n -> hash.getShardInfo(hash.hash(hash.computeMd5(String.valueOf(n)), ran.nextInt(hash.VIRTUAL_NUM)))).forEach(j -> {
			if (result.containsKey(j)) {
				result.get(j).incrementAndGet();
			} else {
				result.put(String.valueOf(j), new AtomicInteger(1));
			}
		});
		result.forEach((k, v) -> System.out.println(k + "--" + v));
	}

}  