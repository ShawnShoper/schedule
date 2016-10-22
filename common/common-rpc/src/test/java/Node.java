public class Node {
	String name;
	String ip;
	public Node(String name,String ip) {
		this.name = name;
		this.ip = ip;
	}
	@Override
	public String toString() {
		return this.name+"-"+this.ip;
	}
}
