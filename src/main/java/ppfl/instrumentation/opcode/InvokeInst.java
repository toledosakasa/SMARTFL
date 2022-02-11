package ppfl.instrumentation.opcode;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import ppfl.ByteCodeGraph;
import ppfl.Node;
import ppfl.instrumentation.CallBackIndex;
import ppfl.instrumentation.TraceDomain;

//Utils for all invoke insts.
public class InvokeInst extends OpcodeInst {
	public int argadd;

	public InvokeInst(int _form, int argadd) {
		super(_form, 0, 0);
		this.doBuild = false;
		this.doPop = false;
		this.doPush = false;
		this.doLoad = false;
		this.doStore = false;
		this.argadd = argadd;
	}

	@Override
	public String getinst(CodeIterator ci, int index, ConstPool constp) {
		StringBuilder ret = new StringBuilder(super.getinst(ci, index, constp));
		int callindex = getu16bitpara(ci, index);
		ret.append(getmethodinfo(ci, callindex, constp));
		return ret.toString();
	}

	@Override
	public void insertReturnSite(CodeIterator ci, int previndex, ConstPool constp, String instinfo, CallBackIndex cbi)
			throws BadBytecode {
		int instpos = ci.insertExGap(8);
		String msg = "\n###RET@" + instinfo.trim();
		int instindex = constp.addStringInfo(msg);

		ci.writeByte(19, instpos);// ldc_w
		ci.write16bit(instindex, instpos + 1);

		ci.writeByte(184, instpos + 3);// invokestatic
		ci.write16bit(cbi.logstringindex, instpos + 4);
	}

	@Override
	public void buildtrace(ByteCodeGraph graph) {
		super.buildtrace(graph);
		String traceclass = info.getvalue("callclass");
		String tracemethod = info.getvalue("callname");
		String signature = info.getvalue("calltype");
		TraceDomain callDomain = new TraceDomain(traceclass, tracemethod, signature);
		Boolean istraced = true;

		TraceDomain newDomain = graph.resolveMethod(callDomain);
		if (newDomain == null) {
			istraced = false;
		} else {
			if (!callDomain.equals(newDomain)) {
				// System.out.println(callDomain);
				// System.out.println(newDomain);
				callDomain = newDomain;
			}
		}

		String desc = info.getvalue("calltype");
		int argcnt = OpcodeInst.getArgNumByDesc(desc);

		// add An extra argument: caller:object->callee:this
		// for non-static invokes.
		argcnt += this.argadd;

		// if argadd avail.
		Node objectAddress = null;

		// collect arguments
		if (graph.getRuntimeStack().size() < argcnt) {
			System.err.println(String.format("%d stacks is not enough for %d args", graph.getRuntimeStack().size(), argcnt));
		}
		for (int i = 0; i < argcnt; i++) {
			Node node = graph.getRuntimeStack().pop();
			usenodes.add(node);
			// System.out.println(node.getStmtName());
			if (i == argcnt - 1 && this.argadd == 1) {
				objectAddress = node;
			}
		}

		// mark untraced invokes
		if (!istraced) {
			graph.untracedInvoke = graph.parseinfo;
			graph.untracedStmt = stmt;
			graph.untraceduse = usenodes;
			graph.untracedpred = prednodes;
			graph.untracedargcnt = argcnt;
			if (this.argadd == 1) {
				if (!objectAddress.isHeapObject()) {
					// System.out.println("not a object:" + objectAddress.getStmtName());
				}
				graph.untracedObj = objectAddress;
			}
		} else {
			graph.tracedInvoke = graph.parseinfo;
			graph.tracedStmt = stmt;
			graph.traceduse = usenodes;
			graph.tracedpred = prednodes;
			graph.tracedargcnt = argcnt;
			if (this.argadd == 1) {
				// assert (objectAddress.isHeapObject());
				graph.tracedObj = objectAddress;
			}
		}

		// // if not traced
		// if (!graph.isTraced(traceclass)) {
		// // System.out.println("Not traced");
		// if (!OpcodeInst.isVoidMethodByDesc(desc)) {
		// defnode = graph.addNewStackNode(stmt);
		// graph.buildFactor(defnode, prednodes, usenodes, null, stmt);
		// }
		// // graph.exceptionuse = new ArrayList<>();
		// // graph.exceptionuse.addAll(usenodes);
		// graph.untraceduse = usenodes;
		// return;
		// }

		// switch stack frame
		// if (istraced)
		// graph.pushStackFrame(callDomain);

		// // static arguments starts with 0
		// int paravarindex = 0;
		// // non-static
		// // paravarindex = 1;
		// for (int i = 0; i < argcnt; i++) {

		// List<Node> adduse = new ArrayList<>();
		// Node curArgument = usenodes.get(argcnt - i - 1);
		// adduse.add(curArgument);

		// Node defnode = graph.addNewVarNode(paravarindex, stmt, callDomain);
		// graph.buildFactor(defnode, prednodes, adduse, null, stmt);
		// paravarindex += curArgument.getSize();
		// }
	}
}
