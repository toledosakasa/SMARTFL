package ppfl.instrumentation;

import java.util.HashMap;
import java.util.Map;

import ppfl.SafeRunTimeStack;

public class RuntimeFrame {
	public int entercnt;
	// public Deque<Node> runtimestack;
	public SafeRunTimeStack runtimestack;
	public TraceDomain domain;

	public RuntimeFrame() {
		entercnt = 0;
		runtimestack = new SafeRunTimeStack();
	}

	private RuntimeFrame(TraceDomain domain) {
		entercnt = 0;
		runtimestack = new SafeRunTimeStack();
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
