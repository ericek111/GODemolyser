package eu.lixko.godemolyser.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Eventable {
	
	private Map<String, ArrayList<EventListener>> listeners = new HashMap<String, ArrayList<EventListener>>();
	
	public void on(String evname, EventListener lis) {
		this.listeners.computeIfAbsent(evname, (k) -> new ArrayList<EventListener>()).add(lis);
	}
	
	protected void fire(DemoEvent<?> ev) {
		ArrayList<EventListener> evlisteners = this.listeners.get(ev.getName());
		for (EventListener lis : evlisteners) {
			lis.onEvent(ev);
		}
	}
	
	protected <K> void fire(String evname, K... data) {
		ArrayList<EventListener> evlisteners = this.listeners.get(evname);
		if (evlisteners == null)
			return;
		DemoEvent<K> de = new DemoEvent<K>(evname, data);
		for (EventListener lis : evlisteners) {
			lis.onEvent(de);
		}
	}
	
	protected void fire(String evname) {
		ArrayList<EventListener> evlisteners = this.listeners.get(evname);
		if (evlisteners == null)
			return;
		DemoEvent<?> de = new DemoEvent<Void>(evname, null);
		for (EventListener lis : evlisteners) {
			lis.onEvent(de);
		}
	}
	
}
