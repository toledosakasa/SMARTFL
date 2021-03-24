package ppfl;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.CompilationUnit;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

public class LineInfo {
	private static MyWriter debugLogger = WriterUtils.getWriter("Debugger");

	private Map<Integer, Line> linemap;
	public final CompilationUnit cu;

	public LineInfo(CompilationUnit _cu) {
		linemap = new TreeMap<>();
		cu = _cu;
	}

	public Line getLineByPos(int pos) {
		int l = cu.getLineNumber(pos);
		return getLine(l);
	}

	public Line getLine(int l) {
		if (!linemap.containsKey(l))
			addLine(l);
		return linemap.get(l);
	}

	public int getLineNumber(int pos) {
		return cu.getLineNumber(pos);
	}

	public void addLine(Integer i) {
		linemap.put(i, new Line(i));
	}

	public void print() {
		debugLogger.writeln("lineinfo: ");
		for (Map.Entry<Integer, Line> k : linemap.entrySet()) {
			debugLogger.writeln(k.getKey() + ":");
			k.getValue().print();
		}
	}
}
