package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Mnemonic;
import ppfl.ByteCodeGraph;
import ppfl.MyWriter;
import ppfl.Node;
import ppfl.ParseInfo;
import ppfl.instrumentation.DynamicTrace;
import ppfl.instrumentation.Trace;
import ppfl.StmtNode;
import ppfl.WriterUtils;
import ppfl.instrumentation.CallBackIndex;
import ppfl.instrumentation.TraceDomain;

public class OpcodeInst {
	private static MyWriter debugWriter = WriterUtils.getWriter("Debugger");
	// private static Logger debugLogger = LoggerFactory.getLogger("Debugger");

	public int form;
	public String opcode;

	// buildtrace
	protected StmtNode stmt;
	protected ParseInfo info;
	protected DynamicTrace dtrace;
	protected List<Node> prednodes;
	protected List<Node> usenodes;
	protected Node defnode;

	// buildtrace actions
	protected boolean doPush = true;
	protected boolean doPop = true;
	protected boolean doLoad = true;
	protected boolean doStore = true;
	protected boolean doPred = true;
	protected boolean doBuild = true;

	public enum paratype {
		VAR, CONST, PARAVAR, PARACONST, STATIC, FIELD, NONE, POOL;
	}

	public enum datatype {
		BYTE, SHORT, CHAR, BOOLEAN, INT, LONG, FLOAT, DOUBLE, LDC, STRING, OBJECT, NONE
	}

	// pushs
	int pushnum;
	// pops
	int popnum;
	// store(var)

	public OpcodeInst(int form, int pushnum, int popnum) {
		this.form = form;
		if (form != 255)
			this.opcode = Mnemonic.OPCODE[form];
		this.pushnum = pushnum;
		this.popnum = popnum;
	}

	public static int getArgNumByDesc(String desc) {
		return splitMethodDesc(desc).size();
	}

	public static String getCurrDomainType(ByteCodeGraph graph) {
		return graph.dynamictrace.getDomain().signature;
	}

	public static boolean isLongReturnMethodByDesc(String desc) {
		int endIndex = desc.lastIndexOf(')');
		if (endIndex == -1) {
			// System.err.println(beginIndex);
			// System.err.println(endIndex);
			throw new IllegalArgumentException("bracket mismatch in descriptor");
		}
		String rettype = desc.substring(endIndex + 1);
		return rettype.contains("J") || rettype.contains("D");
	}

	public static boolean isVoidMethodByDesc(String desc) {
		int endIndex = desc.lastIndexOf(')');
		if (endIndex == -1) {
			// System.err.println(beginIndex);
			// System.err.println(endIndex);
			throw new IllegalArgumentException("bracket mismatch in descriptor");
		}
		String rettype = desc.substring(endIndex + 1);
		return rettype.contentEquals("V");
	}

	public static boolean isBooleanMethodByDesc(String desc) {
		int endIndex = desc.lastIndexOf(')');
		if (endIndex == -1) {
			// System.err.println(beginIndex);
			// System.err.println(endIndex);
			throw new IllegalArgumentException("bracket mismatch in descriptor");
		}
		String rettype = desc.substring(endIndex + 1);
		return rettype.contentEquals("Z");
	}

	public static List<String> splitMethodDesc(String desc) {
		// \[*L[^;]+;|\[[ZBCSIFDJ]|[ZBCSIFDJ]
		int beginIndex = desc.indexOf('(');
		int endIndex = desc.lastIndexOf(')');
		if ((beginIndex == -1 && endIndex != -1) || (beginIndex != -1 && endIndex == -1)) {
			// System.err.println(beginIndex);
			// System.err.println(endIndex);
			throw new IllegalArgumentException("bracket mismatch in descriptor");
		}
		String x0;
		if (beginIndex == -1 && endIndex == -1) {
			x0 = desc;
		} else {
			x0 = desc.substring(beginIndex + 1, endIndex);
		}
		Pattern pattern = Pattern.compile("\\[*L[^;]+;|\\[[ZBCSIFDJ]|[ZBCSIFDJ]");
		Matcher matcher = pattern.matcher(x0);

		ArrayList<String> listMatches = new ArrayList<>();

		while (matcher.find()) {
			listMatches.add(matcher.group());
		}
		return listMatches;
	}

	String getpara(CodeIterator ci, int cindex, int paraindex) {
		if (ci == null)
			return null;
		return String.valueOf(ci.byteAt(cindex + paraindex));
	}

	String getpool(CodeIterator ci, int cindex, int paraindex, ConstPool constp) {
		if (ci == null)
			return null;
		return constp.getLdcValue(ci.byteAt(cindex + paraindex)).toString();
	}

	int dispatchByDesc(String desc, CallBackIndex cbi) {
		if (desc.length() < 1) {
			return 0;
		}
		if(ppfl.instrumentation.TraceTransformer.useNewTrace){
			switch (desc.charAt(0)) {
				case 'L':
				case '[':
					return cbi.traceindex_object;
				case 'B':
					return cbi.traceindex_byte;
				case 'C':
					return cbi.traceindex_char;
				case 'D':
					return cbi.traceindex_double;
				case 'F':
					return cbi.traceindex_float;
				case 'I':
					return cbi.traceindex_int;
				case 'J':
					return cbi.traceindex_long;
				case 'S':
					return cbi.traceindex_short;
				case 'Z':
					return cbi.traceindex_boolean;
				default:
					return 0;
			}
		}
		else{
			switch (desc.charAt(0)) {
				case 'L':
				case '[':
					return cbi.tsindex_object;
				case 'B':
					return cbi.tsindex_byte;
				case 'C':
					return cbi.tsindex_char;
				case 'D':
					return cbi.tsindex_double;
				case 'F':
					return cbi.tsindex_float;
				case 'I':
					return cbi.tsindex_int;
				case 'J':
					return cbi.tsindex_long;
				case 'S':
					return cbi.tsindex_short;
				case 'Z':
					return cbi.tsindex_boolean;
				default:
					return 0;
			}
		}
	}

	int get1para(CodeIterator ci, int index) {
		if (ci == null)
			return 0;
		return ci.byteAt(index + 1);
	}

	int getu16bitpara(CodeIterator ci, int index) {
		if (ci == null)
			return 0;
		return ci.u16bitAt(index + 1);
	}

	int gets16bitpara(CodeIterator ci, int index) {
		if (ci == null)
			return 0;
		return ci.s16bitAt(index + 1);
	}

	int gets32bitpara(CodeIterator ci, int index) {
		if (ci == null)
			return 0;
		return ci.s32bitAt(index + 1);
	}

	String getmethodinfo(CodeIterator ci, int callindex, ConstPool constp) {
		if (ci == null)
			return null;
		String calltype = constp.getMethodrefType(callindex);
		String callclass = constp.getMethodrefClassName(callindex);
		String callname = constp.getMethodrefName(callindex);
		// String callname = null;
		// if (constp.isConstructor(callclass, callindex) == 0) {
		// callname = "init";
		// } else {
		// callname = constp.getMethodrefName(callindex);
		// }
		return ",calltype=" + calltype + ",callclass=" + callclass + ",callname=" + callname;
	}

	String getStaticFieldInfo(CodeIterator ci, int index, ConstPool constp) {
		int num = this.getu16bitpara(ci, index);
		StringBuilder ret = new StringBuilder();
		ret.append(",field=");
		ret.append(constp.getFieldrefClassName(num));
		ret.append("#");
		ret.append(constp.getFieldrefName(num));
		return ret.toString();
	}

	String getFieldInfo(CodeIterator ci, int index, ConstPool constp) {
		int num = this.getu16bitpara(ci, index);
		StringBuilder ret = new StringBuilder();
		ret.append(",field=");
		ret.append(constp.getFieldrefName(num));
		return ret.toString();
	}

	private static int getIntFromMap(Map<String, String> m, String key) {
		String s = m.get(key);
		if (s == null)
			return -1;
		m.remove(key);
		return Integer.parseInt(s);
	}

	public String encode(String msg) {
		String[] splt = msg.split(",");
		Map<String, String> m = new HashMap<>();
		for (String s : splt) {
			String[] tmp = s.split("=");
			m.put(tmp[0].trim(), tmp[1].trim());
		}
		StringBuilder sb = new StringBuilder("\n");
		String op = m.get("opcode");
		// if (op == null) {
		// System.err.println(m.keySet());
		// }
		op = op.substring(0, op.indexOf('('));
		m.remove("opcode");

		int opc = Integer.parseInt(op);// 2
		int popn = getIntFromMap(m, "popnum");// 1
		int pushn = getIntFromMap(m, "pushnum");// 1
		int load = getIntFromMap(m, "load");// 4
		int store = getIntFromMap(m, "store");// 4
		// TODO compress lineinfo
		// String lineinfo[] = m.get("lineinfo").split("#");
		// m.remove("lineinfo");
		// int linenum = Integer.parseInt(lineinfo[2]);// 4
		// int byteindex = Integer.parseInt(lineinfo[3]);// 4
		int nextinst = getIntFromMap(m, "nextinst");// 4

		int comp[] = { opc, popn, pushn, load, store, nextinst };
		sb.append(opc);
		for (int i = 1; i < comp.length; i++) {
			sb.append(',');
			sb.append(comp[i]);
		}

		for (Entry<String, String> k : m.entrySet()) {
			sb.append(String.format(",%s=%s", k.getKey(), k.getValue()));
		}
		return sb.toString();
	}

	// temporary.
	// extended class should override this method.
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder("\n");
		ret.append("opcode=" + this.form + "(" + this.opcode + ")\t");
		if (this.popnum != 0)
			ret.append(",popnum=" + this.popnum);
		if (this.pushnum != 0)
			ret.append(",pushnum=" + this.pushnum);
		return ret.toString();
	}

	// extended class should override this method. (wide index)
	public String getinst_wide(CodeIterator ci, int index, ConstPool constp) {
		return getinst(ci, index, constp);
	}

	// there's no need to override this.
	public void insertByteCodeBefore(CodeIterator ci, int index, ConstPool constp, String inst, CallBackIndex cbi)
			throws BadBytecode {
		if (inst != null && !inst.equals("")) {
			// insertmap.get(ln).append(inst);
			int instpos = ci.insertGap(6);
			// inst = encode(inst);
			int instindex = constp.addStringInfo(inst);
			// System.out.println(constp.getStringInfo(instindex));

			ci.writeByte(19, instpos);// ldc_w
			ci.write16bit(instindex, instpos + 1);

			ci.writeByte(184, instpos + 3);// invokestatic
			ci.write16bit(cbi.logstringindex, instpos + 4);
		}
	}

	//FIXME: there's no need to override this, except Areturn ? 
	// For other return insts, seems not to trace the return value now. 
	public void insertBefore(CodeIterator ci, ConstPool constp, int poolindex, CallBackIndex cbi)
		throws BadBytecode {

		int instpos = ci.insertGap(6);
		int instindex = constp.addIntegerInfo(poolindex);
		ci.writeByte(19, instpos);// ldc_w
		ci.write16bit(instindex, instpos + 1);
		ci.writeByte(184, instpos + 3);// invokestatic
		ci.write16bit(cbi.logtraceindex, instpos + 4);
		
	}

	public void insertBeforeCompress(CodeIterator ci, ConstPool constp, int poolindex, CallBackIndex cbi)
	throws BadBytecode {

	int instpos = ci.insertGap(6);
	int instindex = constp.addIntegerInfo(poolindex);
	ci.writeByte(19, instpos);// ldc_w
	ci.write16bit(instindex, instpos + 1);
	ci.writeByte(184, instpos + 3);// invokestatic
	ci.write16bit(cbi.logcompressindex, instpos + 4);
	
}

	// only for invoke insts.
	public void insertReturnSite(CodeIterator ci, int previndex, ConstPool constp, String instinfo, CallBackIndex cbi)
			throws BadBytecode {
		// doing nothing
	}

		// only for invoke insts.
	public void insertReturn(CodeIterator ci, ConstPool constp, int poolindex, CallBackIndex cbi)
			throws BadBytecode {
		// doing nothing
	}

	// extended class should override this method.
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		// print stack value pushed by this instruction.
		// this should be inserted after the instruction is executed
		// (after ci.next() is called)

		// if (this.pushnum == 1 && (this.opcode.startsWith("i"))) {
		// should use insertExGap here.
		// int instpos = ci.insertExGap(8);
		// ci.writeByte(184, instpos + 2);// invokestatic
		// ci.write16bit(cbi.tsindex_int, instpos + 3);
		// }
	}

	// extended class should override this method.
	public void insertAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {

	}

	public void buildstmt(ByteCodeGraph graph) {
		this.dtrace = graph.dynamictrace;
		TraceDomain tDomain = dtrace.getDomain();
		int linenumber = dtrace.trace.lineno;
		int byteindex = dtrace.trace.index;

		// TODO: use index insteadof String name as id
		String stmtname = tDomain.toString() + "#" + linenumber;
		stmtname = stmtname + "#" + byteindex;
		this.stmt = graph.getStmt(stmtname, this.dtrace.trace.opcode);

		// count how many times this statment has been executed
		if (graph.stmtcountmap.containsKey(stmtname)) {
			graph.stmtcountmap.put(stmtname, graph.stmtcountmap.get(stmtname) + 1);
		} else {
			graph.stmtcountmap.put(stmtname, 1);
		}

		if (graph.max_loop > 0 && graph.stmtcountmap.get(stmtname) > graph.max_loop) {
			return;
		}

		// auto-assigned observation: test function always true
		if (graph.auto_oracle) {
			if (tDomain.tracemethod.contentEquals(graph.testname) || graph.d4jTestClasses.contains(tDomain.traceclass)
					|| stmtname.startsWith("junit")) {
				stmt.observe(true);
				// debugWriter.writeln(String.format("Observe %s as true", stmt.getName()));
			}
		}

	}

	public void buildtrace_simple(ByteCodeGraph graph) {
		buildstmt(graph);
		this.prednodes = new ArrayList<>();
		this.usenodes = new ArrayList<>();
		this.defnode = null;
		if (!graph.getRuntimeStack().isEmpty())
			usenodes.add(graph.getRuntimeStack().pop());
		defnode = graph.addNewStackNode(stmt);
		if (this.doBuild && defnode != null) {
			// TODO should consider ops.
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		}
	}

	// override needed.
	public void buildtrace_wide(ByteCodeGraph graph) {
		buildtrace(graph);
	}

	// override only by putstatic
	public void buildinit(ByteCodeGraph graph) {
		// do nothing
	}

	// override needed.
	public void buildtrace(ByteCodeGraph graph) {
		// build the stmtnode(common)
		buildstmt(graph);
		this.prednodes = new ArrayList<>();
		this.usenodes = new ArrayList<>();
		this.defnode = null;

		if (this.doPred) {
			Node thepred = graph.getPredStack();
			if (thepred != null)
				prednodes.add(thepred);
		}

		if (this.doLoad && dtrace.trace.load != null) {
			int loadvar = dtrace.trace.load;
			Node node = graph.getLoadNodeAsUse(loadvar);
			if (node != null)
				usenodes.add(node);
		}
		if (this.doPop && dtrace.trace.popnum != null) {
			int instpopnum = dtrace.trace.popnum;
			for (int i = 0; i < instpopnum; i++) {
				usenodes.add(graph.getRuntimeStack().pop());
			}
		}
		// defs
		// stack
		if (this.doPush && dtrace.trace.pushnum != null) {
			int instpushnum = dtrace.trace.pushnum;
			assert (instpushnum <= 1);
			defnode = graph.addNewStackNode(stmt);
		}
		if (this.doStore && dtrace.trace.store != null) {
			int storevar = dtrace.trace.store;
			defnode = graph.addNewVarNode(storevar, stmt);
		}

		// build factor.
		if (this.doBuild && defnode != null) {
			// TODO should consider ops.
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		}
	}
}