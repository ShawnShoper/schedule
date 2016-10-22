import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author ShawnShoper
 * @date 2016/10/19
 * @sice
 */
public class Roulette {

	private Integer[] roulette;
	List<Integer> tmp = new ArrayList<>();

	public void addNode (int node) {
		tmp.add(node);
	}

	public void init () {
		roulette = tmp.toArray(new Integer[]{});
	}

	Random random = new Random();

	public Integer get () {
		return roulette[random.nextInt(roulette.length)];
	}
}
