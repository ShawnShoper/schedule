package org.shoper.common.rpc.manager.selector;

import org.shoper.commons.MD5Util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by IntelliJ IDEA.
 * User: test
 * Date: 12-5-24
 * Time: 下午5:37
 * To change this template use File | Settings | File Templates.
 */
public class ConsistencyHash {


	/**
	 * 根据2^32把节点分布到圆环上面。
	 *
	 * @return
	 */
	public static long hash (String key) {
		ByteBuffer buf = ByteBuffer.wrap(key.getBytes());
		int seed = 0x1234ABCD;
		ByteOrder byteOrder = buf.order();
		buf.order(ByteOrder.LITTLE_ENDIAN);
		long m = 0xc6a4a7935bd1e995L;
		int r = 47;
		long h = seed ^ (buf.remaining() * m);
		long k;
		while (buf.remaining() >= 8) {
			k = buf.getLong();

			k *= m;
			k ^= k >>> r;
			k *= m;

			h ^= k;
			h *= m;
		}

		if (buf.remaining() > 0) {
			ByteBuffer finish = ByteBuffer.allocate(8).order(
					ByteOrder.LITTLE_ENDIAN);
			// for big-endian version, do this first:
			// finish.position(8-buf.remaining());
			finish.put(buf).rewind();
			h ^= finish.getLong();
			h *= m;
		}
		h ^= h >>> r;
		h *= m;
		h ^= h >>> r;
		buf.order(byteOrder);
		return h;
	}

	/**
	 * Get the md5 of the given key.
	 * 计算MD5值
	 */
	public static byte[] computeMd5 (String k) {

		return MD5Util.GetMD5Code(k).getBytes();
	}


}  