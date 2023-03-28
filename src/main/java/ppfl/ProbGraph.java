package ppfl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.ObjectInputStream;

import ppfl.instrumentation.TracePool;
import ppfl.instrumentation.TraceDomain;
import ppfl.instrumentation.DynamicTrace;
import ppfl.instrumentation.TraceSequence;
import ppfl.instrumentation.Interpreter;
import ppfl.instrumentation.InvokeTrace;
import ppfl.instrumentation.Trace;
import ppfl.instrumentation.TraceTransformer;

import ppfl.defects4j.GraphBuilder;
import ppfl.instrumentation.opcode.OpcodeInst;

import ppfl.instrumentation.TracePool;
import ppfl.instrumentation.TraceSequence;
import ppfl.instrumentation.Trace;
import ppfl.instrumentation.TraceTransformer;

public class ProbGraph {
    private MyWriter graphLogger;
    private MyWriter resultLogger;
    private MyWriter debugLogger;

    public boolean debug_logger_switch = true;

    public void setGraphLogger(String proj, int id) {
        String path = "logs/graph/";
        graphLogger = WriterUtils.getWriter(path, "graph-" + proj + "-" + id);
    }

    public void setResultLogger(String proj, int id) {
        String path = "logs/result/";
        resultLogger = WriterUtils.getWriter(path, "result-"+ proj + "-" + id);
    }

    public void setDebugLogger(String proj, int id) {
        String path = "logs/debug/";
        debugLogger = WriterUtils.getWriter(path, "debug-" + proj + "-" + id);
        Edge.debugLogger = debugLogger;
        FactorNode.debugLogger = debugLogger;
        Node.debugLogger = debugLogger;
    }

    // all test methods in metadata, each string - TestClass::testMethod
    public Set<String> d4jMethodNames = new HashSet<>();
    // all test classes in metadata, each string - TestClass
    public Set<String> d4jTestClasses = new HashSet<>();
    // all trigger classes in metadata, each string - TestClass::testMethod
    public Set<String> d4jTriggerTestNames = new HashSet<>();

    // return false for trigger test, true for other tests, Exception if the name is
    // not in d4j tests
    private boolean getD4jTestState(String fullname) {
        assert (d4jMethodNames.contains(fullname));
        return !d4jTriggerTestNames.contains(fullname);
    }

    private static Set<TraceDomain> tracedDomain = new HashSet<>();
    private static Map<String, String> superClassMap = new HashMap<>();

    // set superClassMap and tracedDomain from traced.source.log
    public void parseWhatIsTracedLog(String logfilename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(logfilename))) {
            String t;
            while ((t = reader.readLine()) != null) {
                if (t.isEmpty())
                    continue;
                String[] splt = t.split("::");
                String traceclass = splt[0];
                if (splt.length < 2) {
                    continue;
                }
                splt = splt[1].split(",");
                for (String methodAndDesc : splt) {
                    splt = methodAndDesc.split("#");
                    String tracemethod = splt[0];
                    if (splt.length < 2) {
                        System.out.println(t);
                    }
                    String signature = splt[1];
                    if (signature.equals("SuperClass")) {
                        superClassMap.put(traceclass, tracemethod);
                    } else {
                        TraceDomain tDomain = new TraceDomain(traceclass, tracemethod, signature);
                        tracedDomain.add(tDomain);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isTraced(TraceDomain domain){
        while(!tracedDomain.contains(domain)){
            String superClass = superClassMap.get(domain.traceclass);
            if(superClass == null)
                return false;
            domain = new TraceDomain(superClass, domain.tracemethod, domain.signature);
        }
        return true;
    }

    public TracePool tracePool;

    public void setPool(String path) {
        try {
            FileInputStream fileIn = new FileInputStream(path);
            BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
            ObjectInputStream in = new ObjectInputStream(bufferedIn);
            this.tracePool = (TracePool) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
            return;
        } catch (ClassNotFoundException c) {
            System.out.println("TracePool class not found");
            c.printStackTrace();
            return;
        }
    }

    public Map<Integer, Integer> post_Idom;

    public void set_Post_Idom(String path) {
        post_Idom = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String tmp;
            while ((tmp = reader.readLine()) != null) {
                String[] splt = tmp.split(",");
                post_Idom.put(Integer.valueOf(splt[0]), Integer.valueOf(splt[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<Integer, Set<Integer>> branch_Stores;

    public void set_Branch_Stores(String path) {
        branch_Stores = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String tmp;
            while ((tmp = reader.readLine()) != null) {
                String[] splt = tmp.split(",");
                Integer branchInst = Integer.valueOf(splt[0]);
                Set<Integer> stores = new HashSet<>();
                if (splt.length > 1) {
                    for (String id : splt[1].split("#")) {
                        stores.add(Integer.valueOf(id));
                    }
                }
                branch_Stores.put(branchInst, stores);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //key {ClassName}::{testMethod}, value {ClassName}:{lineno}
    Map<String, String> exceptionObsMap;
    public void set_Exception_Observe(String path){
        exceptionObsMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String tmp;
            while ((tmp = reader.readLine()) != null) {
                String[] splt = tmp.split("-");
                String key = splt[1];
                String value = splt[2] + ":" + splt[3];
                exceptionObsMap.put(key, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Set<String> assertObs;
    public void set_Assert_Observe(String path){
        assertObs = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String tmp;
            while ((tmp = reader.readLine()) != null) {
                String[] splt = tmp.split("-");
                String value = splt[0] + ":" + splt[1];
                assertObs.add(value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<TraceSequence> traceList;

    private void sortTraceList() {
        this.traceList.sort(new Comparator<TraceSequence>() {
            @Override
            public int compare(TraceSequence arg0, TraceSequence arg1) {
                if (arg0.pass == arg1.pass) {
                    return arg0.size() - arg1.size();
                } else {
                    return arg0.pass ? 1 : -1;
                }
            }
        });
    }

    private static final double MAX_FILE_LIMIT = 1e8;// threshold for maximum pass trace : 100M

    public void parseTrace(String traceFolder) {
        traceList = new ArrayList<>();
        File folder = new File(traceFolder);
        File[] fs = folder.listFiles();
        for (File f : fs) {
            if (f.getName().equals("all.log.ser"))
                continue;
            String suffix = ".log.ser";
            if (f.getName().endsWith(suffix)) {
                String name = f.getName();
                name = name.substring(0, name.length() - suffix.length());
                int index = name.lastIndexOf('.');
                //"fullname = {TestClass}::{TestMethod}"
                String fullname = name.substring(0, index) + "::" + name.substring(index + 1);
                boolean isPassed = getD4jTestState(fullname);
                if (isPassed && f.length() > MAX_FILE_LIMIT) {
                    continue;
                }
                TraceSequence traceSeq = null;
                try {
                    FileInputStream fileIn = new FileInputStream(f.getAbsolutePath());
                    BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
                    ObjectInputStream in = new ObjectInputStream(bufferedIn);
                    traceSeq = (TraceSequence) in.readObject();
                    traceSeq.setTracePool(tracePool);
                    traceSeq.setName(fullname);
                    traceSeq.pass = isPassed;
                    in.close();
                    fileIn.close();
                } catch (IOException i) {
                    // i.printStackTrace();
                    System.out.printf("IOException at " + fullname + "\n");
                    continue;
                } catch (ClassNotFoundException c) {
                    System.out.println("TraceSequence class not found at" + fullname + "\n");
                    // c.printStackTrace();
                    continue;
                }
                traceList.add(traceSeq);
            }
        }

        // this.predstack.clear();
		// this.initmaps();
        sortTraceList();
		int totalSize = 0;
		int thres = 1000000;// threshold : 1M lines for all traces
		for (TraceSequence traceSeq : traceList) {
            if(!traceSeq.pass){
                // observeClass = exceptionObsMap.get(traceSeq.getName()).split(":")[0];
                // observeLineno = Integer.parseInt(exceptionObsMap.get(traceSeq.getName()).split(":")[1]);
                observeSet = new HashSet<>();
                String exceptionObs = exceptionObsMap.get(traceSeq.getName());
                if(exceptionObs != null){
                    observeSet.add(exceptionObs);
                    traceSeq.prune(observeSet);
                }   
                // traceSeq.prune(observeClass, observeLineno);
            }
            else{
                observeSet = assertObs;
                // debugLogger.write("\n" + traceSeq.getName());
                traceSeq.prune(observeSet);
            }
            totalSize += traceSeq.size();
            if (totalSize > thres && traceSeq.pass) {
				break;
			}
			try {
                int size = parseSeq(traceSeq);
                // totalSize += size;

			} catch (Exception e) {
				System.out.println("parse " + traceSeq.getName() + " failed");
                e.printStackTrace();
                debugLogger.write("\n[Bug] parse " + traceSeq.getName() + " failed");
                String sStackTrace = WriterUtils.handleException(e);
                debugLogger.write(String.format("\n[Bug] parse Exception, %s", sStackTrace));
			}
		}

    }

    private boolean isValid;
    public boolean frameValid(){
        return topFrame().isValid();
    }

    public void pushFrame(TraceDomain domain, boolean valid){
        //debugLogger.write("\npush frame %s, valid = %b", domain.toString(), valid);
        this.stackframe.push(domain, valid);
        // this.stackframe.print(debugLogger);
    }

    public void popFrame(){
        RuntimeFrame f = this.stackframe.top();
        // debugLogger.write("\npop frame " + f.getName());
        this.stackframe.pop();
        // this.stackframe.print(debugLogger);
    }

    public RuntimeFrame topFrame(){
        return this.stackframe.top();
    }

    public StmtNode getStmt(Integer index){
        return stmtMap.get(index);
    }

    public StmtNode addStmt(Integer index, int form){
        StmtNode stmt = new StmtNode(index, form, tracePool.get(index).getlineinfo());
        stmtMap.put(index, stmt);
        this.stmts.add(stmt);
        return stmt;
    }

    public Node addVarNode(int index, StmtNode stmt, int stacksize){
        Node defnode = topFrame().putMap(index, this.testname, stmt);
        defnode.setSize(stacksize);
        nodes.add(defnode);
        // this.stackframe.print(debugLogger);
        return defnode;
    }

    public Node getVarNode(int index){
        return topFrame().getMap(index);
    }

    // for dup inst
    public void pushStackNode(Node node){
        topFrame().pushStack(node);
    }

    public Node addStackNode(StmtNode stmt, int stacksize){
        Node defnode = topFrame().pushStack(this.testname, stmt);
        defnode.setSize(stacksize);
        nodes.add(defnode);
        return defnode;
    }

    public Node popStackNode(){
        Node ret = topFrame().popStack();
        // debugLogger.write("\n   pop stack node" + ret.getNodeName());
        return ret;
    }


    public Node pushPred(StmtNode stmt){
        // debugLogger.write("\n add pred from " + stmt.getName());
        Node defnode = topFrame().pushPred(this.testname, stmt);
        nodes.add(defnode);
        return defnode;
    }

    public Node getPred(){
        return topFrame().getPred();
    }

    public Node popPred(){
        return topFrame().popPred();
    }

    public void pushStore(Set<Integer> store){
        topFrame().pushStore(store);
    }

    public Set<Integer> popStore(){
        return topFrame().popStore();
    }

    public Node getHeap(Node objref, String field){
        Integer addr = objref.getAddress();
        if(heapMap.get(addr) == null)
            return null;
        return heapMap.get(addr).get(field);
    }

    public Node getObj(Node objref){
        return getHeap(objref, "-self");
    }

    public Node addHeap(Node objref, String field, StmtNode stmt, int stacksize){
        Integer addr = objref.getAddress();
        if(heapMap.get(addr) == null)
            heapMap.put(addr, new HashMap<>());
        int defcnt = 0;
        Node heapnode = heapMap.get(addr).get(field);
        if(heapnode != null)
            defcnt = heapnode.defcnt;
        String name = addr + "." + field;
        heapnode = new Node(name, testname, stmt, defcnt);
        heapnode.setSize(stacksize);
        heapMap.get(addr).put(field, heapnode);
        nodes.add(heapnode);
        return heapnode;
    }

    public Map<String, Node> getFields(Node objref){
        return heapMap.get(objref.getAddress());
    }

    public Node addObj(Node objref, StmtNode stmt){
        return addHeap(objref, "-self", stmt, 1);
    }

    public Node getStatic(String name){
        return staticMap.get(name);
    }

    public Node addStatic(String name, StmtNode stmt, int stacksize){
		int defcnt = 0;
		if(staticMap.get(name) != null)
			defcnt = staticMap.get(name).defcnt;
		Node staticnode = new Node(name, testname, stmt, defcnt);
        staticnode.setSize(stacksize);
		staticMap.put(name, staticnode);
        nodes.add(staticnode);
		return staticnode;
    }



    public void killPredStack(Integer instIndex) {
		boolean willcontinue = true;
		while (willcontinue) {
			willcontinue = false;
			if (getPred() != null) {
				Integer stmtIndex = getPred().getStmtIndex();
                if(post_Idom.get(stmtIndex) == null) // Pred from Exception, never pop this pred
                    return;
				// System.out.println("in kill "+stmtName);
				if (post_Idom.get(stmtIndex).equals(instIndex)) {
					Node curPred = popPred();
					StmtNode curPredStmt = curPred.stmt;
					Set<Integer> stores = popStore();
					// System.out.println("kill "+stores);
					boolean unexecuted_complement = true; // evaluation switch
					if (unexecuted_complement) {
						if (stores != null) {
							// StmtNode curStmt = curPred.stmt;
                            // debugLogger.write("\n   store = ");
							for (Integer i : stores) {
                                // debugLogger.write(" %d,",i);
								Node usenode = getVarNode(i);
                                // 只在某一个分差中定义的变量，在另一条路上会是null的，在此后由于离开作用域，也不会再出现
								if (usenode == null)
									continue;

                                // TODO 这里把原本的独立虚拟语句换成了predstmt，可能要确认一下效果
                                Node defnode = addVarNode(i, curPredStmt, usenode.getSize());
                                buildFactor(defnode, curPred, usenode, null, curPredStmt);
							}
						}
					}
					willcontinue = true;
				}
			}
		}
	}


	public FactorNode buildFactor(Node defnode, Node prednode, Node usenode, List<String> ops, StmtNode stmt) {
		List<Node> prednodes = new ArrayList<>();
		prednodes.add(prednode);
		List<Node> usenodes = new ArrayList<>();
		usenodes.add(usenode);
		return buildFactor(defnode, prednodes, usenodes, ops, stmt);
	}


	public int lastDefinedLine = 0;
	public List<Node> lastDefinedVar = new ArrayList<>();

    public int lastDefinedLine_Out = 0;
	public List<Node> lastDefinedVar_Out = new ArrayList<>();

    // public String observeClass;
    // public int observeLineno;
    //{ClassName}:{lineno}
    public Set<String> observeSet;
    public List<Node> assertVar = new ArrayList<>();
    // public List<Node> observeVar = new ArrayList<>();
    // public int observeState = 0;

    public FactorNode buildFactor(Node defnode, List<Node> prednodes, List<Node> usenodes, List<String> ops, StmtNode stmt) {

        // // evaluation switch
        // this.setStackForNode(defnode);

        // FIXME this may conceal bugs in various insts.
        // temporary solution.
        // if (compromise) { // false
        //     Iterator<Node> iter = usenodes.iterator();
        //     while (iter.hasNext()) {
        //         Node tmp = iter.next();
        //         if (tmp == null) {
        //             iter.remove();
        //         }
        //     }
        // }

        // 存疑，暂时注释掉
        // TODO: add calss 判断
        Trace stmtTrace = this.tracePool.get(stmt.getIndex());
        // String stmtName = stmtTrace.classname + "::" + stmtTrace.methodname;
        // if(!stmtName.equals(this.testname) && frameValid()){
        int ln = stmtTrace.lineno;
        if (this.lastDefinedLine != ln) {
            this.lastDefinedLine = ln;
            this.lastDefinedVar.clear();
        }
        this.lastDefinedVar.add(defnode);
        // }

        String testClassName = this.testname.split("::")[0];

        if(!stmtTrace.classname.equals(testClassName) && this.isValid){
            // debugLogger.write("\n stmtName " + stmtName);
            // debugLogger.write("\n testname " + this.testname);
            if (this.lastDefinedLine_Out != ln) {
                this.lastDefinedLine_Out = ln;
                this.lastDefinedVar_Out.clear();
            }
            this.lastDefinedVar_Out.add(defnode);

            // // TODO: check this
            // if(defnode.isHeapObject()){
            //     this.lastDefinedVar_Out.add(getObj(defnode));
            // }
        }

        String stmtName = stmtTrace.classname + ":" + stmtTrace.lineno;
        if(assertObs.contains(stmtName))
            assertVar.add(defnode);

        // if(stmtTrace.classname.equals(observeClass) && stmtTrace.lineno == observeLineno){
        //     if(observeState == 0){
        //         observeVar.clear();
        //         observeState = 1;
        //     }
        //     observeVar.add(defnode);
        // }
        // else{
        //     observeState = 0;
        // }

        Edge dedge = new Edge();
        dedge.setnode(defnode);
        defnode.add_edge(dedge);
        defnode.setdedge(dedge);
        Edge sedge = new Edge();
        sedge.setnode(stmt);
        stmt.add_edge(sedge);
        List<Edge> pedges = new ArrayList<>();
        for (Node n : prednodes) {
            Edge nedge = new Edge();
            nedge.setnode(n);
            pedges.add(nedge);
            n.add_edge(nedge);
        }
        List<Edge> uedges = new ArrayList<>();
        for (Node n : usenodes) {
            Edge nedge = new Edge();
            nedge.setnode(n);
            uedges.add(nedge);
            n.add_edge(nedge);
        }
        FactorNode ret = new FactorNode(defnode, stmt, prednodes, usenodes, ops, dedge, sedge, pedges, uedges);
        factornodes.add(ret);
        dedge.setfactor(ret);
        sedge.setfactor(ret);
        for (Edge n : pedges)
            n.setfactor(ret);
        for (Edge n : uedges)
            n.setfactor(ret);
        this.lastFactor = new InvokeItem(stmt, usenodes, prednodes, this.dynamicTrace, 0, null);
        return ret;
    }

	private boolean matchTracedInvoke(InvokeItem invoke, DynamicTrace dTrace) {
        if(!invoke.canBeParsed)
            return false;
		TraceDomain td = invoke.invokeTrace.getCallDomain();
        // 对一些重载方法，可能会是父子类的关系，所以此处先不比较类
		return td.signature.equals(dTrace.trace.signature) && td.tracemethod.equals(dTrace.trace.methodname);
	}

    private void resolveTracedArgs(InvokeItem invoke, DynamicTrace dTrace) {
		// switch stack frame
        pushFrame(dTrace.getDomain(), true);
		int argcnt = invoke.argcnt;
		// static arguments starts with 0
		int paravarindex = 0;
		// non-static
		// paravarindex = 1;
		for (int i = 0; i < argcnt; i++) {

			List<Node> adduse = new ArrayList<>();
			Node curArgument = invoke.use.get(argcnt - i - 1);
			adduse.add(curArgument);
            Node defnode = addVarNode(paravarindex, invoke.stmt, curArgument.getSize());
			buildFactor(defnode, invoke.pred, adduse, null, invoke.stmt);
			paravarindex += curArgument.getSize();
		}
	}

    // private void resolveUnTracedArgs(DynamicTrace dTrace) {
	// 	// switch stack frame
    //     pushFrame(dTrace.getDomain());
	// 	// int argcnt = this.untracedargcnt;


    //     List<String> parameters = OpcodeInst.splitMethodDesc(dTrace.trace.signature);
    //     // unstatic arguments starts with 1
    //     if(!dTrace.trace.isStatic())
    //         parameters.add(0, "Lself;");
	// 	int paravarindex = 0;

    //     InvokeItem currentInvoke = invoke.peek();

	// 	for (String parameter: parameters) {
    //         int stacksize = 1;
    //         if(parameter.equals("D") || parameter.equals("J"))
    //             stacksize = 2;
    //         Node defnode = addVarNode(paravarindex, currentInvoke.stmt, stacksize);
	// 		buildFactor(defnode, currentInvoke.pred, currentInvoke.use, null, currentInvoke.stmt);
	// 		paravarindex += stacksize;
	// 	}
	// }


    private void buildAliveInvoke(){
        InvokeItem invoke = getInvoke();
        List<Node> useobj = new ArrayList<>();
        for(Node n : invoke.use){
            if(n.isHeapObject()){
                Node self = getObj(n);
                if(self != null)
                    useobj.add(self);
            }
        }
        invoke.use.addAll(useobj);

        Node expnode = addStackNode(invoke.stmt, 1);
        buildFactor(expnode, invoke.pred, invoke.use, null, invoke.stmt);
    }

    private void buildInvoke() {
        InvokeItem invoke = getInvoke();
        String signature = invoke.invokeTrace.trace.getcalltype();
        List<Node> useobj = new ArrayList<>();
        // for(Node n : invoke.use)
        //     debugLogger.write("\n use " + n.getNodeName());
        for(Node n : invoke.use){
            if(n.isHeapObject()){
                Node self = getObj(n);
                if(self != null && !useobj.contains(self)) // 需要去重，不然可能会把多个self加进来，引发inference的问题
                    useobj.add(self);
            }
        }
        // for(Node n : useobj)
        //     debugLogger.write("\n obj " + n.getNodeName());
        invoke.use.addAll(useobj);
        
        Set<Integer> objAddrs = new HashSet<>();
        for(Node n : invoke.use){
            if(n.isHeapObject()){
                if(objAddrs.contains(n.getAddress()))
                    continue;
                objAddrs.add(n.getAddress());
                Node defself = addObj(n, invoke.stmt);
                buildFactor(defself, invoke.pred, invoke.use, null, invoke.stmt);

                Map<String, Node> fields = getFields(n);
                for(Map.Entry<String, Node> entry : fields.entrySet()){
                    String field = entry.getKey();
                    int size = entry.getValue().getSize();
                    if(!field.equals("-self")){
                        Node deffield = addHeap(n, field, invoke.stmt, size);
                        buildFactor(deffield, invoke.pred, invoke.use, null, invoke.stmt);
                    }
                }
            }
        }
        
        // debugLogger.write("\n   sig = %s", signature);
		if (!OpcodeInst.isVoidMethodByDesc(signature)) {
            int size = 1;
            if(OpcodeInst.isLongReturnMethodByDesc(signature))
                size = 2;
			Node defnode = addStackNode(invoke.stmt, size);
			buildFactor(defnode, invoke.pred, invoke.use, null, invoke.stmt);
		}
		cleanInvoke();

		// String desc = this.untracedInvoke.trace.getcalltype();
		// List<Node> toadd = new ArrayList<>();
		// for (Node n : this.untraceduse) {
		// 	if (n.isHeapObject()) {
		// 		Node nd = getObjectNode(n);
		// 		if (nd != null)
		// 			toadd.add(nd);
		// 	}
		// }
		// this.untraceduse.addAll(toadd);
		// if (!OpcodeInst.isVoidMethodByDesc(desc)) {
		// 	Node defnode = this.addNewStackNode(this.untracedStmt);
		// 	if (OpcodeInst.isLongReturnMethodByDesc(desc)) {
		// 		defnode.setSize(2);
		// 	}
		// 	buildFactor(defnode, this.untracedpred, this.untraceduse, null, this.untracedStmt);
		// }
		// for (Node n : this.untraceduse) {
		// 	if (n.isHeapObject()) {
		// 		// 把untraced方法中的所有 由aload加载得到的堆obj中的所有访问过的field，给重新定义一次
		// 		buildFactorForAllField(n, this.untracedpred, this.untraceduse, null, this.untracedStmt);
		// 	}
		// }
		// // build untraced object
		// if (this.untracedObj != null && untracedObj.isHeapObject()) {
		// 	Node obj = addNewObjectNode(this.untracedObj, this.untracedStmt);
		// 	buildFactor(obj, this.untracedpred, this.untraceduse, null, this.untracedStmt);
		// }


	}

    // private void buildTracedInvoke() {
    //     InvokeItem invoke = this.tracedInvoke;
    //     String signature = invoke.invokeTrace.trace.getcalltype();
    //     // debugLogger.write("\n   sig = %s", signature);
	// 	if (!OpcodeInst.isVoidMethodByDesc(signature)) {
    //         int size = 1;
    //         if(OpcodeInst.isLongReturnMethodByDesc(signature))
    //             size = 2;
	// 		Node defnode = addStackNode(invoke.stmt, size);
	// 		buildFactor(defnode, invoke.pred, invoke.use, null, invoke.stmt);
	// 	}
	// 	this.cleanTraced();
	// }

    // private void buildUntracedInvoke() {
    //     InvokeItem invoke = this.untracedInvoke;
    //     String signature = invoke.invokeTrace.trace.getcalltype();
    //     // debugLogger.write("\n   sig = %s", signature);
	// 	if (!OpcodeInst.isVoidMethodByDesc(signature)) {
    //         int size = 1;
    //         if(OpcodeInst.isLongReturnMethodByDesc(signature))
    //             size = 2;
	// 		Node defnode = addStackNode(invoke.stmt, size);
	// 		buildFactor(defnode, invoke.pred, invoke.use, null, invoke.stmt);
	// 	}
	// 	this.cleanUntraced();
	// }

    public void setInvoke(InvokeItem invoke){
        topFrame().setInvoke(invoke);
        this.lastFactor = invoke;
        // debugLogger.write("\n invoke size = " + invoke.use.size());
        // debugLogger.write("\n invoke use = ");
        // for(Node n : invoke.use)
        //     debugLogger.write(" " + n.getNodeName() + ",");
    }

	public InvokeItem getInvoke(){
        if(topFrame() == null)
            return null;
		return topFrame().getInvoke();
	}

    public void cleanInvoke(){
        // debugLogger.write("\n clean invoke" + topFrame().getInvoke().invokeTrace);
        topFrame().cleanInvoke();
    }

    // public void setTraced(InvokeItem invoke){
    //     this.tracedInvoke = invoke;
    // }

	// public void cleanTraced() {
	// 	this.tracedInvoke = null;
	// }

    // public void setUntraced(InvokeItem invoke){
    //     this.untracedInvoke = invoke;
    // }

    // public void cleanUntraced(){
    //     this.untracedInvoke = null;
    // }

    // public void pushUntraced(InvokeItem invoke){
    //     invoke.add(invoke);
    // }

	// public void popUntraced() {
	// 	invoke.pop();
	// }

    public void clear(){
        this.stackframe.clear();
        this.heapMap.clear();
        this.staticMap.clear();
        this.lastFactor = null;
        this.lastDefinedLine = 0;
        this.lastDefinedVar = new ArrayList<>();
        this.lastDefinedLine_Out = 0;
        this.lastDefinedVar_Out = new ArrayList<>();
        this.assertVar = new ArrayList<>();
        // invoke.clear();
        // this.cleanInvoke();
    }

	private int parseSeq(TraceSequence traceSeq) {
		this.clear();
		int tracelength = traceSeq.size();
		System.out.println("parsing trace,length=" + tracelength + ":");
		System.out.println("\t" + traceSeq.getName());  
		boolean debugswitch = false;

		boolean testpass = traceSeq.pass;
		this.testname = traceSeq.getName();
        // if(!testpass){
        //     observeClass = exceptionObsMap.get(traceSeq.getName()).split(":")[0];
        //     observeLineno = Integer.parseInt(exceptionObsMap.get(traceSeq.getName()).split(":")[1]);
        // }

		int linec = 0;
        for(int index=0;index<tracelength;index++){
            DynamicTrace dTrace = traceSeq.get(index);

            if(debug_logger_switch)
                debugLogger.write(dTrace.toString());

            // if(!dTrace.trace.isInst()) //log like ###Class::MethodName
            //     continue;

            if(dTrace.isret){
                parseRet(dTrace);
            }

            else if(dTrace.isStackTrace()){
                parseException(dTrace);
            }

            // // still buggy if untraced with recursion
            // else if(this.untracedInvoke != null)
            //     continue;

            else if(dTrace.trace.isMethodLog()){
                parseInvoke(dTrace);
            }

            else if(getInvoke() != null){
                getInvoke().canBeParsed = false;
                continue;
            }

            // // 对一些重载的方法，可能子类没有被追踪，导致tracedInvoke没被消掉
            // else if(this.tracedInvoke != null)
            //     continue;

            else if(dTrace.trace.isInst()){
                linec++;
                parseSingleTrace(dTrace);
            }

            // if(getInvoke() != null)
            //     debugLogger.write("\n   invoke = " + getInvoke().invokeTrace);

            // if(topFrame()!=null)
            //     topFrame().printStack(debugLogger);

            // for (RuntimeFrame f : stackframe.stackframe){
            //     if(f.getDomain().tracemethod.equals("getPublicMethod"))
            //         f.printStack(debugLogger);
            // }

            // printHeap();

            // if(topFrame()!=null)
            //     topFrame().printMap(debugLogger);

        }

        // if crash occurs, and there is still unresolved invoke, lastDefinedVar should be modified with last call
        if(getInvoke() != null){
            buildAliveInvoke();
        }

        for (Node i : lastDefinedVar) {
            i.observe(testpass);
            if(debug_logger_switch)
                debugLogger.writeln("\nobserve " + i.getNodeName() + " = " + testpass);
        }

        for (Node i : lastDefinedVar_Out){
            i.observe(testpass);
            if(debug_logger_switch)
                debugLogger.writeln("\nobserve " + i.getNodeName() + " = " + testpass);
        }

        for(Node i : assertVar){
            if(i.getobs())
                continue;
            i.observe(true);
            if(debug_logger_switch)
                debugLogger.writeln("\nobserve " + i.getNodeName() + " = " + true);
        }

        // for(Node i : observeVar){
        //     i.observe(testpass);
        //     if(debug_logger_switch)
        //     debugLogger.writeln("\nnew observe " + i.getNodeName() + " = " + testpass);
        // }

        return linec;
	}

    private void parseInvoke(DynamicTrace dTrace){
        // this.dynamicTrace = dTrace;

        // 有一个隐患是如果调用未追踪方法，然后未追踪方法里有catch，又触发了clinit，又把clinit
        // 中的exception给catch了，会导致clinit没有return而出问题.
        // 不过一般正常情况下clinit里的exception也不会被外层的东西给抓到？ 所以应该影响不大？可以把由未追踪方法的clinit 给加进来
        if(dTrace.trace.methodname.equals("<clinit>")){
            pushFrame(dTrace.getDomain(), true);
            return;
        }

        InvokeItem invoke = getInvoke();
        if(invoke != null){
            if(matchTracedInvoke(invoke, dTrace)){
                cleanInvoke();
                resolveTracedArgs(invoke, dTrace);
            }
            return;
        }
        // some other frame 
        else{
            pushFrame(dTrace.getDomain(), false);
            return;
        }

        // if(dTrace.trace.methodname.equals("<clinit>")){
        //     pushFrame(dTrace.getDomain());
        //     return;
        // }

        // InvokeItem currentInvoke = invoke.peek();
        // if (currentInvoke != null) {
        //     // 在有多层untraced方法时，如果中间方法发生throw，会无法确定被哪个untraced method catch
        //     while(!topFrame().getDomain().equals(currentInvoke.invokeTrace.getCallDomain())){
        //         popFrame();
        //     }
        //     resolveUnTracedArgs(dTrace);
        //     return;
		// }

        // debugLogger.writeln("No caller for " + dTrace.toString());

    }

    private void parseRet(DynamicTrace dTrace){

        InvokeItem invoke = getInvoke();
        if(invoke != null){
            //debugLogger.write("\ninvoke index = %d, dtrace index = %d", invoke.invokeTrace.traceindex, dTrace.traceindex);
            if(invoke.invokeTrace.traceindex == dTrace.traceindex)
                buildInvoke();
        }

        //不需要，clinit会以 return指令结尾，在那里popframe即可
        // if(dTrace.trace.methodname.equals("<clinit>")){
        //     if(topFrame().getDomain().equals(dTrace.getDomain())){
        //         popFrame();
        //     }
        //     else{
        //         debugLogger.writeln("unmatched clinit ret at " + dTrace.toString());
        //     }
        //     return;
        // }
        // if(isTraced(dTrace.getCallDomain())){
        //     if(this.tracedInvoke != null){
        //         if(this.tracedInvoke.invokeTrace.traceindex == dTrace.traceindex){
        //             buildTracedInvoke();
        //         }
        //     }
        //     return;
        // }
        
        // debugLogger.writeln("%d, %d",untracedInvoke.invokeTrace.traceindex, dTrace.traceindex);
        
        // if(this.untracedInvoke.invokeTrace.traceindex == dTrace.traceindex){
        //     buildUntracedInvoke();
        // }

        // InvokeItem currentInvoke = invoke.peek();
        // if (currentInvoke != null) {
        //     if(currentInvoke.invokeTrace.traceindex == dTrace.traceindex){
        //         // may still exist some middle frame (throw but catched by untraced method)
        //         while(!topFrame().getDomain().equals(currentInvoke.invokeTrace.getCallDomain())){
        //             popFrame();
        //         }
                
        //         // 在invokeinst的build中，加入对untraced method建的一个空栈，里面会保存中间方法的ret
        //         List<Node> usenodes = new ArrayList<>();
        //         while(true){
        //             Node use = popStackNode();
        //             if(use == null)
        //                 break;
        //             usenodes.add(use);
        //         }
        //         popFrame();

        //         int stacksize = 1;
        //         if (OpcodeInst.isLongReturnMethodByDesc(currentInvoke.invokeTrace.trace.getcalltype())) {
        //             stacksize = 2;
        //         }
                
        //         Node defnode = addStackNode(currentInvoke.stmt, stacksize);
        //         buildFactor(defnode, currentInvoke.pred, usenodes, null, currentInvoke.stmt);
        //         popUntraced();
        //         return;
        //     }
		// }
    
    }

    private void parseException(DynamicTrace dTrace){
        this.stackframe.handleException(dTrace.stackTrace);
        StmtNode triggerStmtNode = lastFactor.stmt;
        Node exceptionNode = addStackNode(triggerStmtNode, 1);
        buildFactor(exceptionNode, lastFactor.pred, lastFactor.use, null, triggerStmtNode);
        Node exceptionPred = pushPred(triggerStmtNode);
        buildFactor(exceptionPred, lastFactor.pred, lastFactor.use, null, triggerStmtNode);
    }

    private void parseSingleTrace(DynamicTrace dTrace){
        this.dynamicTrace = dTrace;
        int instIndex = dTrace.traceindex;

		// TODO: use index, not string as id
		killPredStack(instIndex);

        Set<Integer> stores = branch_Stores.get(instIndex);
        if(stores != null)
            pushStore(stores);

        this.isValid = frameValid();   
		Interpreter.map[this.dynamicTrace.trace.opcode].build(this);
    }



	public String testname;
    public List<FactorNode> factornodes;
	public List<Node> nodes;
	public List<StmtNode> stmts;
    public Map<Integer, StmtNode> stmtMap;


    // private InvokeItem tracedInvoke;
    // private InvokeItem untracedInvoke;
    // private Deque<InvokeItem> untracedInvoke;
    private InvokeItem lastFactor;

    // public StmtNode tracedStmt;
	// public List<Node> traceduse;
	// public List<Node> tracedpred;
	// public DynamicTrace tracedInvoke = null;
	// public int tracedargcnt = 0;
	// public Node tracedObj = null;

    // public StmtNode untracedStmt;
	// public List<Node> untraceduse;
	// public List<Node> untracedpred;
	// public DynamicTrace untracedInvoke = null;
	// public int untracedargcnt = 0;
	// public Node untracedObj = null;

	private StackFrame stackframe;
    // private RuntimeFrame currentframe;

    // private Map<String, Node> heapMap;
    private Map<Integer, Map<String, Node>> heapMap;
    private Map<String, Node> staticMap;

    public DynamicTrace dynamicTrace = null;


    public ProbGraph(){
        this.factornodes = new ArrayList<>();
		this.nodes = new ArrayList<>();
		this.stmts = new ArrayList<>();
        this.stmtMap = new HashMap<>();
        this.stackframe = new StackFrame();
        this.heapMap = new HashMap<>();
        this.staticMap = new HashMap<>();

        Interpreter.init();
        // invoke = new ArrayDeque<>();
    }

    public int bp_times = 100;

    public void check_bp(boolean verbose) {
        // for(FactorNode f : factornodes){
        //     f.print(debugLogger);
        //     debugLogger.writeln("%b",f.hasUNKoperator);
        // }

		long bptime = this.bp_inference();
		resultLogger.writeln("\nProbabilities: ");
		resultLogger.writeln("Vars:%d", nodes.size());
		resultLogger.writeln("Stmts:%d", stmts.size());
		for (StmtNode n : stmts) {
			if (!n.getreduced())
				n.bpPrintProb(resultLogger);
		}
		resultLogger.writeln("Belief propagation time : %fs", bptime / 1000.0);
	}

	public void path_reduce() {
		for (Node n : nodes) {
			if (n.getobs()) {
                //graphLogger.write("\n get obs " + n.getNodeName());
				if (n.getreduced())
					mark_reduce(n);
			}
		}
		// maybe won't be used?
		for (Node n : stmts) {
			if (n.getobs()) {
				n.setreduced();
			}
		}
	}

    public void mark_reduce(Node node) {
		Deque<Node> reducestack = new ArrayDeque<>();
		reducestack.push(node);
		node.setreduced();
		while (!reducestack.isEmpty()) {
			Node reducNode = reducestack.pop();
			FactorNode deffactor = (reducNode.getdedge()).getfactor();
			deffactor.getstmt().setreduced();
			List<Node> pulist = deffactor.getpunodes();
			for (Node n : pulist) {
				if (n.getreduced() == true && !n.isStmt) {
					n.setreduced();
                    //graphLogger.write("\n reduce mark " + n.getNodeName());
					reducestack.push(n);
				}
			}
		}
	}

    public long bp_inference() {
		long startTime = System.currentTimeMillis();
		path_reduce();
		boolean outreduced = false;
		if (outreduced) {
			graphLogger.writeln("\nReduced Nodes: ");
			for (Node n : stmts) {
				if (n.getreduced())
					n.print(graphLogger);
			}
			for (Node n : nodes) {
				if (n.getreduced())
					n.print(graphLogger);
			}
			graphLogger.writeln("\n");
		}

		boolean loopend = false;
		for (int i = 0; i < bp_times; i++) {
			boolean isend = true;
			for (FactorNode n : factornodes) {
                if(FactorNode.nontrivial)
				    n.send_message();
                else
                    n.sendMessage();
			}
			for (Node n : nodes) {
				if (n.send_message())
					isend = false;
			}
			for (Node n : stmts) {
				if (n.send_message())
					isend = false;
			}
			if (isend) {
				// if (debug_logger_switch)
				// 	graphLogger.writeln("\n\n%d\n\n", i);
				resultLogger.writeln("Belief propagation time: %d\n", i);
				loopend = true;
				break;
			}
		}
		if (!loopend)
			resultLogger.writeln("Belief propagation time: %d\n", bp_times);
		Comparator<Node> comp;
		comp = (arg0, arg1) -> Double.compare(arg0.bp_getprob(), arg1.bp_getprob());
		nodes.sort(comp);
		stmts.sort(comp);

		long endTime = System.currentTimeMillis();
		return (endTime - startTime);
	}

    public void printgraph() {
		graphLogger.writeln("\nNodes: stmt=%d,node=%d", stmts.size(), nodes.size());
		for (Node n : stmts) {
			n.print(graphLogger);
		}
		for (Node n : nodes) {
			n.print(graphLogger);
		}
		graphLogger.writeln("Factors: %d", factornodes.size());
		for (FactorNode n : factornodes) {
			n.print(graphLogger);
		}
	}

    public void printreduced(){
        graphLogger.writeln("reduced :");
        for (Node n : nodes) {
			if (n.getreduced())
            graphLogger.writeln(n.getNodeName());
		}
    }

    public void printHeap(){
		debugLogger.write("\n	heap node = ");
		for(Integer addr: heapMap.keySet()){
            debugLogger.write("%d: ", addr);
            for(String field : heapMap.get(addr).keySet()){
                debugLogger.write(field+ ",");
            }
        }
	}


    // private void parseInfo(File f, String name) {
    // String suffix = ".log.ser";
    // name = name.substring(0, name.length() - suffix.length());
    // int index = name.lastIndexOf('.');
    // String.getName() = name.substring(0, index) + "::" + name.substring(index +
    // 1);
    // if (getD4jTestState(fullname) && f.length() > MAX_FILE_LIMIT) {
    // return;
    // }

    // TraceSequence traceseq = null;
    // try {
    // // System.out.printf("yyyy "+f.getAbsolutePath()+"\n");
    // FileInputStream fileIn = new FileInputStream(f.getAbsolutePath());
    // BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
    // ObjectInputStream in = new ObjectInputStream(bufferedIn);
    // traceseq = (TraceSequence) in.readObject();
    // traceseq.setTracePool(tracePool);
    // // System.out.printf("yyyy "+traceseq.get(0).trace.classname +"\n");
    // in.close();
    // fileIn.close();
    // } catch (IOException i) {
    // // i.printStackTrace();
    // System.out.printf("IOException at " + fullname + "\n");
    // return;
    // } catch (ClassNotFoundException c) {
    // System.out.println("TraceSequence class not found at" + fullname + "\n");
    // // c.printStackTrace();
    // return;
    // }
    // this.addTraceChunk(fullname);
    // TraceChunk thischunk = traceList.get(traceList.size() - 1);
    // int size = traceseq.size();
    // // System.out.println(size);
    // for (int i = 0; i < size; i++) {
    // DynamicTrace dtrace = traceseq.get(i);

    // // TODO: 检测何时会发生，若没有fail test，直接丢弃这种测试
    // // if(dtrace==null){
    // // // System.err.println(String.format("dynamicTrace %d/%d is null in
    // TraceThunk %s",i,size,thischunk.fullname));
    // // continue;
    // // }

    // if (dtrace.trace.type == Trace.LogType.Inst) {
    // thischunk.parseOneTrace(dtrace);
    // }
    // // if (dtrace.isret)
    // // traceList.get(traceList.size() - 1).parseOneTrace(dtrace);
    // // } else {
    // // traceList.get(traceList.size() - 1).parseOneTrace(dtrace);
    // // }
    // }
    // }

}
