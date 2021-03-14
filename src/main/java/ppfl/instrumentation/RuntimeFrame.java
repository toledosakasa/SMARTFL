package ppfl.instrumentation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import ppfl.Node;

public class RuntimeFrame {
	public int entercnt;
	public Deque<Node> runtimestack;
	public TraceDomain domain;

	public RuntimeFrame() {
		entercnt = 0;
		runtimestack = new ArrayDeque<>();
	}

	private RuntimeFrame(TraceDomain domain) {
		entercnt = 0;
		runtimestack = new ArrayDeque<>();
		this.domain = domain;
	}

	public String getDomain() {
		return domain.toString() + "#" + entercnt + ":";
	}

	public static RuntimeFrame getFrame(TraceDomain domain) {
		String id = domain.toString();
		if (!framemap.containsKey(id)) {
			framemap.put(id, new RuntimeFrame(domain));
		}
		return framemap.get(id);
	}

	private static Map<String, RuntimeFrame> framemap = new HashMap<>();
}
