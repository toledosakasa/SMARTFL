package ppfl.instrumentation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import ppfl.Node;

public class RuntimeFrame {
	public int entercnt;
	public Deque<Node> runtimestack;
	public String traceclass;
	public String tracemethod;

	public RuntimeFrame() {
		entercnt = 0;
		runtimestack = new ArrayDeque<>();
	}

	private RuntimeFrame(String tclass, String tmethod) {
		entercnt = 0;
		runtimestack = new ArrayDeque<>();
		traceclass = tclass;
		tracemethod = tmethod;
	}

	public String getDomain() {
		return this.traceclass + ":" + this.tracemethod + "#" + entercnt + ":";
	}

	public static RuntimeFrame getFrame(String tclass, String tmethod) {
		String id = tclass + ":" + tmethod;
		if (!framemap.containsKey(id)) {
			framemap.put(id, new RuntimeFrame(tclass, tmethod));
		}
		return framemap.get(id);
	}

	private static Map<String, RuntimeFrame> framemap = new HashMap<>();
}
