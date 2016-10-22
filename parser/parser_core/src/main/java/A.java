/**
 * @author ShawnShoper
 * @date 16/10/12
 * @sice
 */
public class A {
	private String zookeeper;
	private String application;

	public String getZookeeper () {
		return zookeeper;
	}

	public void setZookeeper (String zookeeper) {
		this.zookeeper = zookeeper;
	}

	public String getApplication () {
		return application;
	}

	public void setApplication (String application) {
		this.application = application;
	}

	@Override
	public String toString () {
		return "A{" +
				"zookeeper='" + zookeeper + '\'' +
				", application='" + application + '\'' +
				'}';
	}
}
