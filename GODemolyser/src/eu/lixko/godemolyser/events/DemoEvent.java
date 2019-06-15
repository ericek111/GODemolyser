package eu.lixko.godemolyser.events;

public class DemoEvent<K> {
	
	private String name;
	private K[] data;
	
	public DemoEvent(String name, K... data) {
		this.data = data;
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public K[] getData() {
		return this.data;
	}
	
	public K getData(int idx) {
		if (idx >= this.data.length)
			return null;
		return this.data[idx];
	}
	
}
