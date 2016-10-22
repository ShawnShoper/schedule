import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * @author ShawnShoper
 * @date 2016/10/19
 * @sice
 */
public class RouletteTests {
	Roulette roulette = new Roulette();

	@Before
	public void init () {
		IntStream.rangeClosed(0, 100).forEach(roulette::addNode);
		roulette.init();
	}

	@Test
	public void test () {
		{
			Map<Object, AtomicInteger> result = new HashMap<>();
			roulette.get();
			IntStream.iterate(0, n -> ++n).limit(10000000).mapToObj(n -> roulette.get()).forEach(j -> {
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
