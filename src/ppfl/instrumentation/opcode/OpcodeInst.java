package ppfl.instrumentation.opcode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private boolean isinvoke;
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
	paratype pushtype = paratype.NONE;
	datatype pushdatatype = datatype.NONE;
	String pushvalue = null;
	// pops
	int popnum;
	// store(var)
	paratype storetype = paratype.NONE;
	String storevalue = null;
	// for weird instructions(e.g. iinc)
	paratype para[] = new paratype[2];

//	//for function calls
//	String calltype;
//	String callname;
//	String callclass;

	public OpcodeInst(int _form, int _pushnum, int _popnum) {
		form = _form;
		opcode = Mnemonic.OPCODE[_form];
		pushnum = _pushnum;
		popnum = _popnum;
		this.isinvoke = false;
	}

	public static int getArgNumByDesc(String desc) {
		return splitMethodDesc(desc).size();
	}

	public static ArrayList<String> splitMethodDesc(String desc) {
		// \[*L[^;]+;|\[[ZBCSIFDJ]|[ZBCSIFDJ]
		int beginIndex = desc.indexOf('(');
		int endIndex = desc.lastIndexOf(')');
		if ((beginIndex == -1 && endIndex != -1) || (beginIndex != -1 && endIndex == -1)) {
			System.err.println(beginIndex);
			System.err.println(endIndex);
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

		ArrayList<String> listMatches = new ArrayList<String>();

		while (matcher.find()) {
			listMatches.add(matcher.group());
		}

//        for(String s : listMatches)
//        {
//            System.out.print(s + " ");
//        }
//        System.out.println();
		return listMatches;
	}

	public void setStore(paratype t, String _storevalue) {
		this.storetype = t;
		this.storevalue = _storevalue;
	}

	public void setPush(paratype t, String _pushvalue) {
		this.pushtype = t;
		this.pushvalue = _pushvalue;
	}

	public void setPushDataType(datatype t) {
		this.pushdatatype = t;
	}

	public void setPara(int id, paratype t) {
		para[id] = t;
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

	String getparas(CodeIterator ci, int index) {
		if (ci == null)
			return null;
		String ret = "";
		if (this.para[0] != null)
			ret = ret + getpara(ci, index, 1);
		if (this.para[1] != null)
			ret = ret + getpara(ci, index, 2);
		return ret;
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
		// ret.append("opcode="+this.opcode);
		ret.append("opcode=" + this.form + "(" + this.opcode + ")\t");
//		if (this.isinvoke) {
//			int callindex = getu16bitpara(ci, index);
//			ret.append(getmethodinfo(ci, callindex, constp));
//			return ret.toString();
//		}
		if (this.popnum != 0)
			ret.append(",popnum=" + this.popnum);
		if (this.pushnum != 0)
			ret.append(",pushnum=" + this.pushnum);
		return ret.toString();
	}

	// there's no need to override this.
	public void insertByteCodeBefore(CodeIterator ci, int index, ConstPool constp, String linenumberinfo,
			CallBackIndex cbi) throws BadBytecode {

		String inst = getinst(ci, index, constp);
		inst = inst + linenumberinfo;
		if (inst != null) {
			// insertmap.get(ln).append(inst);
			int instpos = ci.insertGap(8);
			int instindex = constp.addStringInfo(inst);
			System.out.println(constp.getStringInfo(instindex));
			ci.writeByte(19, instpos);// ldc_w
			ci.write16bit(instindex, instpos + 1);

			ci.writeByte(184, instpos + 3);// invokestatic
			ci.write16bit(cbi.logstringindex, instpos + 4);
		}
	}

	public void setinvoke() {
		this.isinvoke = true;
	}

	// temporary solution for integer insts.
	// extended class should override this method.
	public void insertByteCodeAfter(CodeIterator ci, int index, ConstPool constp, CallBackIndex cbi)
			throws BadBytecode {
		// print stack value pushed by this instruction.
		// this should be inserted after the instruction is executed
		// (after ci.next() is called)

//		if (this.pushnum == 1 && (this.opcode.startsWith("i"))) {
//			int instpos = ci.insertGap(8);
//			// ci.writeByte(93, instpos + 1);// buggy dup. can't explain(?)
//			// call (I)I callback instead of (I)V callback.
//			ci.writeByte(184, instpos + 2);// invokestatic
//			ci.write16bit(cbi.tsindex_int, instpos + 3);
//		}
	}

	public StmtNode buildstmt(ByteCodeGraph graph) {
		ParseInfo info = graph.parseinfo;
		String traceclass = info.traceclass;
		String tracemethod = info.tracemethod;
		int linenumber = info.linenumber;
		int byteindex = info.byteindex;

		StmtNode stmt = null;
		String stmtname = traceclass + ":" + tracemethod + "#" + String.valueOf(linenumber);
		// System.out.println("At line " + stmtname);
		stmtname = stmtname + "#" + String.valueOf(byteindex);
		if (!graph.hasNode(stmtname)) {
			stmt = new StmtNode(stmtname);
			graph.addNode(stmtname, stmt);
		} else {
			stmt = (StmtNode) graph.getNode(stmtname);
			assert (stmt.isStmt());
		}

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
				System.out.println("Observe " + stmt.getName() + " as true");
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
		List<Node> prednodes = new ArrayList<Node>();
		List<Node> usenodes = new ArrayList<Node>();
		Node defnode = null;
		if (info.getintvalue("load") != null) {
			int loadvar = info.getintvalue("load");
			Node node = graph.getNode(graph.getFormalVarName(loadvar));
			if (node == null) {
				System.out.println(graph.getFormalVarName(loadvar));
			}
			assert (node != null);
			usenodes.add(node);
		}
		if (info.getintvalue("popnum") != null) {
			int instpopnum = info.getintvalue("popnum");
			for (int i = 0; i < instpopnum; i++) {
				usenodes.add(graph.runtimestack.pop());
			}
		}
		// defs
		// stack
		if (info.getintvalue("pushnum") != null) {
			int instpushnum = info.getintvalue("pushnum");
			// push must not be more than 1
			assert (instpushnum == 1);
			graph.incStackIndex();
			String nodename = graph.getFormalStackNameWithIndex();
			Node node = new Node(nodename, graph.testname, stmt);
			graph.addNode(nodename, node);
			defnode = graph.getNode(nodename);
			assert (node == defnode);
			graph.runtimestack.add(defnode);
		}
		if (info.getintvalue("store") != null) {
			int storevar = info.getintvalue("store");
			graph.incVarIndex(storevar);
			String nodename = graph.getFormalVarNameWithIndex(storevar);
			Node node = new Node(nodename, graph.testname, stmt);
			graph.addNode(nodename, node);
			defnode = graph.getNode(nodename);
			assert (node == defnode);
		}

		// build factor.
		if (defnode != null) {
			graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
			if (graph.auto_oracle) {
				graph.last_defined_var = defnode;
			}
		}
	}
}