package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Mnemonic;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.ParseInfo;
import ppfl.StmtNode;
import ppfl.instrumentation.CallBackIndex;

public class OpcodeInst {
	private static Logger debugLogger = LoggerFactory.getLogger("Debugger");

	int form;
	String opcode;

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
		this.opcode = Mnemonic.OPCODE[form];
		this.pushnum = pushnum;
		this.popnum = popnum;
	}

	public static int getArgNumByDesc(String desc) {
		return splitMethodDesc(desc).size();
	}

	public static List<String> splitMethodDesc(String desc) {
		// \[*L[^;]+;|\[[ZBCSIFDJ]|[ZBCSIFDJ]
		int beginIndex = desc.indexOf('(');
		int endIndex = desc.lastIndexOf(')');
		if ((beginIndex == -1 && endIndex != -1) || (beginIndex != -1 && endIndex == -1)) {
			// System.err.println(beginIndex);
			// System.err.println(endIndex);
			throw new RuntimeException();
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

	String getmethodinfo(CodeIterator ci, int callindex, ConstPool constp) {
		if (ci == null)
			return null;
		String calltype = constp.getMethodrefType(callindex);
		String callclass = constp.getMethodrefClassName(callindex);
		String callname = constp.getMethodrefName(callindex);
		return ",calltype=" + calltype + ",callclass=" + callclass + ",callname=" + callname;
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

	// there's no need to override this.
	public void insertByteCodeBefore(CodeIterator ci, int index, ConstPool constp, String inst, CallBackIndex cbi)
			throws BadBytecode {

		if (inst != null && !inst.equals("")) {
			// insertmap.get(ln).append(inst);
			int instpos = ci.insertGap(8);
			int instindex = constp.addStringInfo(inst);
			// System.out.println(constp.getStringInfo(instindex));

			ci.writeByte(19, instpos);// ldc_w
			ci.write16bit(instindex, instpos + 1);

			ci.writeByte(184, instpos + 3);// invokestatic
			ci.write16bit(cbi.logstringindex, instpos + 4);
		}
	}

	// extended class should override this method.
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi) throws BadBytecode {
		// print stack value pushed by this instruction.
		// this should be inserted after the instruction is executed
		// (after ci.next() is called)

		// if (this.pushnum == 1 && (this.opcode.startsWith("i"))) {
		// should use insertExGap here.
		// int instpos = ci.insertExGap(8);
		// // ci.writeByte(93, instpos + 1);// buggy dup. can't explain(?)
		// // call (I)I callback instead of (I)V callback.
		// ci.writeByte(184, instpos + 2);// invokestatic
		// ci.write16bit(cbi.tsindex_int, instpos + 3);
		// }
	}

	public StmtNode buildstmt(ByteCodeGraph graph) {
		ParseInfo info = graph.parseinfo;
		String traceclass = info.traceclass;
		String tracemethod = info.tracemethod;
		int linenumber = info.linenumber;
		int byteindex = info.byteindex;

		StmtNode stmt = null;
		String stmtname = traceclass + ":" + tracemethod + "#" + linenumber;
		// System.out.println("At line " + stmtname);
		stmtname = stmtname + "#" + byteindex;
		// if (!graph.hasNode(stmtname)) {
		//// stmt = new StmtNode(stmtname);
		//// graph.addNode(stmtname, stmt);
		// stmt = graph.addNewStmt(stmtname);
		// } else {
		// stmt = (StmtNode) graph.getNode(stmtname);
		// assert (stmt.isStmt());
		// }
		stmt = graph.getStmt(stmtname);

		// count how many times this statment has been executed
		if (graph.stmtcountmap.containsKey(stmtname)) {
			graph.stmtcountmap.put(stmtname, graph.stmtcountmap.get(stmtname) + 1);
		} else {
			graph.stmtcountmap.put(stmtname, 1);
		}

		if (graph.max_loop > 0 && graph.stmtcountmap.get(stmtname) > graph.max_loop) {
			return null;
		}

		// auto-assigned observation: test function always true
		if (graph.auto_oracle) {
			if (tracemethod.contentEquals(graph.testname)) {
				stmt.observe(true);
				debugLogger.info("Observe " + stmt.getName() + " as true");
			}
		}

		return stmt;
	}

	// override needed.
	public void buildtrace(ByteCodeGraph graph) {
		// build the stmtnode(common)
		StmtNode stmt = buildstmt(graph);

		ParseInfo info = graph.parseinfo;
		// info.print();
		// uses
		List<Node> prednodes = new ArrayList<>();
		// prednodes.addAll(graph.predicates);
		List<Node> usenodes = new ArrayList<>();
		Node defnode = null;
		if (info.getintvalue("load") != null) {
			int loadvar = info.getintvalue("load");
			Node node = graph.getLoadNodeAsUse(loadvar);
			usenodes.add(node);
		}
		if (info.getintvalue("popnum") != null) {
			int instpopnum = info.getintvalue("popnum");
			for (int i = 0; i < instpopnum; i++) {
				usenodes.add(graph.getRuntimeStack().pop());
			}
		}
		// defs
		// stack
		if (info.getintvalue("pushnum") != null) {
			int instpushnum = info.getintvalue("pushnum");
			// push must not be more than 1
			assert (instpushnum == 1);
			defnode = graph.addNewStackNode(stmt);
		}
		if (info.getintvalue("store") != null) {
			int storevar = info.getintvalue("store");
			defnode = graph.addNewVarNode(storevar, stmt);
		}

		// build factor.
		if (defnode != null) {
			// TODO should consider ops.
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		}
	}
}