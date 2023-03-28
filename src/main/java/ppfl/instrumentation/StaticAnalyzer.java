package ppfl.instrumentation;

import java.io.Serializable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ppfl.MyWriter;

public class StaticAnalyzer implements Serializable {
	private TracePool tracepool;
	private Map<String, Integer> inverted_pool;
	public Set<Integer> instset;
	public Set<Integer> outset;
	public Set<Integer> inset;
	public Map<Integer, List<Integer>> predataflowmap;
	public Map<Integer, List<Integer>> postdataflowmap;
	public Map<Integer, Integer> pre_idom;
	public Map<Integer, Integer> post_idom;
	public Map<Integer, Integer> store_num;
	public Map<Integer, Set<Integer>> branch_stores;
	public List<BackEdge> loopset;
	public MyWriter debugLogger;

	public StaticAnalyzer(MyWriter debugLogger) {
		tracepool = null;
		loopset = null;
		instset = new HashSet<>();
		inverted_pool = new HashMap<>();
		outset = new HashSet<>();
		inset = new HashSet<>();
		predataflowmap = new HashMap<>();
		postdataflowmap = new HashMap<>();
		pre_idom = new HashMap<>();
		post_idom = new HashMap<>();
		store_num = new HashMap<>();
		branch_stores = new HashMap<>();
		debugLogger = debugLogger;
	}

	public void clear() {
		tracepool = null;
		instset = null;
		inverted_pool = null;
		outset = null;
		inset = null;
		predataflowmap = null;
		postdataflowmap = null;
		pre_idom = null;
		post_idom = null;
		store_num = null;
		branch_stores = null;
	}

	public void setTracePool(TracePool tracepool) {
		this.tracepool = tracepool;
	}

	public void setLoopSet(List<BackEdge> loopset) {
		this.loopset = loopset;
	}

	private void setup_inverted_pool() {
		int poolsize = tracepool.size();
		// poolindex is the identifier of the inst in all the data structure
		for (int poolindex = 0; poolindex < poolsize; poolindex++) {
			Trace inst = tracepool.get(poolindex);
			if (inst.type == Trace.LogType.Inst) {
				String key = inst.getdomain() + inst.index;
				inverted_pool.put(key, poolindex);
			} else if (inst.type == Trace.LogType.OutPoint) {
				String key = "OUT_" + inst.getdomain();
				inverted_pool.put(key, poolindex);
			}
		}
	}

	public void parse() {
		setup_inverted_pool();
		Set<String> noNextInsts = new HashSet<String>() {
			{
				add("goto_w");
				add("goto");
				add("return");
				add("areturn");
				add("dreturn");
				add("freturn");
				add("ireturn");
				add("lreturn");
				add("athrow");
			}
		};

		Set<String> switchInsts = new HashSet<>();
		switchInsts.add("tableswitch");
		switchInsts.add("lookupswitch");

		int poolsize = tracepool.size();
		// poolindex is the identifier of the inst in all the data structure
		// now get predataflowmap
		for (int poolindex = 0; poolindex < poolsize; poolindex++) {
			Trace inst = tracepool.get(poolindex);
			if (inst.type == Trace.LogType.MethodLog)
				continue;
			// FIXME: cunyi
			if (inst.type == Trace.LogType.OutPoint) {
				List<Integer> edges = new ArrayList<>();
				predataflowmap.put(poolindex, edges);
				instset.add(poolindex);
				continue;
				// debugLogger.writeln("[SA] " + inst.toString());
			}
			// the entry of a method
			if (inst.index == 0)
				inset.add(poolindex);
			Integer storen = inst.store;
			if (storen != null)
				store_num.put(poolindex, storen);
			String domain = inst.getdomain();
			String opcode = Interpreter.map[inst.opcode].opcode;
			List<Integer> edges = new ArrayList<>();
			// deal with switch
			if (switchInsts.contains(opcode)) {
				Integer defaultbyte = inst.getdefault();
				if (defaultbyte != null) {
					String defaultinst = domain + (defaultbyte.intValue() + inst.index);
					Integer defaultindex = inverted_pool.get(defaultinst);
					edges.add(defaultindex);
				}
				String switchlist = inst.getswitch();
				if (switchlist != null) {
					String[] switchterms = switchlist.split(";");
					for (String switchterm : switchterms) {
						String jumpinst = domain
								+ (Integer.valueOf(switchterm.split(":")[1]).intValue() + inst.index);
						Integer jumpindex = inverted_pool.get(jumpinst);
						edges.add(jumpindex);
					}
				}
				predataflowmap.put(poolindex, edges);
				instset.add(poolindex);
				continue;
			}
			// add the sequential next inst
			if (!noNextInsts.contains(opcode)) {
				String nextinst = domain + inst.nextinst;
				Integer nextindex = inverted_pool.get(nextinst);
				edges.add(nextindex);
			}
			// deal with branch
			Integer branchbyte = inst.getbranchbyte();
			if (branchbyte != null) {
				String branchinst = domain + (branchbyte.intValue() + inst.index);
				Integer branchindex = inverted_pool.get(branchinst);
				edges.add(branchindex);
			}
			// deal with the insts which have no succs
			if (edges.isEmpty()) {
				String outname = "OUT_" + domain;
				Integer outindex = inverted_pool.get(outname);
				edges.add(outindex);
				outset.add(outindex);
			}
			predataflowmap.put(poolindex, edges);
			instset.add(poolindex);
		}

		// init the postmap, keys including OUT_xx
		for (Integer poolindex : instset) {
			List<Integer> edges = new ArrayList<>();
			postdataflowmap.put(poolindex, edges);
		}
		// get the postmap
		for (Map.Entry<Integer, List<Integer>> instname : predataflowmap.entrySet()) {
			List<Integer> preedges = instname.getValue();
			for (Integer prenode : preedges) {
				List<Integer> postedges = postdataflowmap.get(prenode);
				postedges.add(instname.getKey());
			}
		}
		this.inverted_pool = null;
	}

	private Deque<Integer> reverse_postorder = new ArrayDeque<>();
	private Set<Integer> visited = new HashSet<>();
	private Map<Integer, Integer> postorder = new HashMap<>();
	private int cnt;

	private void dfssearch(Integer inst) {
		Deque<Integer> searchstack = new ArrayDeque<>();
		searchstack.push(inst);
		visited.add(inst);
		while (!searchstack.isEmpty()) {
			Integer theinst = searchstack.peek();
			List<Integer> thesuccs = postdataflowmap.get(theinst);
			boolean isleaf = true;
			for (Integer succ : thesuccs) {
				if (!visited.contains(succ)) {
					visited.add(succ);
					searchstack.push(succ);
					isleaf = false;
				}
			}
			if (isleaf) {
				reverse_postorder.addFirst(theinst);
				postorder.put(theinst, cnt);
				cnt++;
				searchstack.pop();
			}
		}
	}

	private Integer intersect(Integer b1, Integer b2) {
		Integer finger1 = b1;
		Integer finger2 = b2;
		while (!finger1.equals(finger2)) {
			while (postorder.get(finger1).intValue() < postorder.get(finger2).intValue()) {
				finger1 = post_idom.get(finger1);
			}
			while (postorder.get(finger1).intValue() > postorder.get(finger2).intValue()) {
				finger2 = post_idom.get(finger2);
			}
		}
		return finger1;
	}

	// Keith D. Cooper algorithm
	public void get_post_idom() {
		cnt = 1;
		reverse_postorder = new ArrayDeque<>();
		visited = new HashSet<>();
		postorder = new HashMap<>();
		for (Integer outname : outset) {
			dfssearch(outname);
		}
		for (Integer inst : reverse_postorder) {
			post_idom.put(inst, -1); // -1 means "Undifined"
		}
		for (Integer outname : outset) {
			post_idom.put(outname, outname);
		}
		boolean changed = true;
		while (changed) {
			changed = false;
			for (Integer inst : reverse_postorder) {
				if (outset.contains(inst))
					continue;
				List<Integer> thepreds = predataflowmap.get(inst);
				int predsnum = thepreds.size();
				int tmpmax = -1;
				int tmpindex = 0;
				// seems should get the pred with the max postorder
				for (int i = 0; i < predsnum; i++) {
					// to deal with pre_idom, some nodes can not be visited in pre order in the
					// graph so it has no order after dfs
					if (postorder.get(thepreds.get(i)) == null) {
						continue;

						// resultLogger.writeln("null at inst %s, pred %s\n", inst, thepreds.get(i));
						// for(Map.Entry<String, Integer> entry : postorder.entrySet())
						// resultLogger.writeln("key = " + entry.getKey() + ", value = " +
						// entry.getValue());

						// for(String tmps: outset)
						// resultLogger.writeln("outset0 %s\n", tmps);
					}
					if (tmpmax < postorder.get(thepreds.get(i)).intValue()) {
						tmpmax = postorder.get(thepreds.get(i)).intValue();
						tmpindex = i;
					}
				}
				Integer new_idom = thepreds.get(tmpindex);
				for (int i = 0; i < predsnum; i++) {
					if (i == tmpindex)
						continue;
					Integer otherpred = thepreds.get(i);
					// to deal with pre_idom, some nodes can not be visited in pre order in the
					// graph so it has no order after dfs
					if (post_idom.get(otherpred) == null)
						continue;
					if (!post_idom.get(otherpred).equals(-1)) {
						new_idom = intersect(otherpred, new_idom);
					}
				}
				if (!post_idom.get(inst).equals(new_idom)) {
					post_idom.put(inst, new_idom);
					changed = true;
				}
			}
		}
		// System.out.println("size =" + post_idom.size());
		// for (String key : post_idom.keySet()) {
		// System.out.println("key_" + key);
		// System.out.println("post_idom = " + post_idom.get(key));
		// }
	}

	public void get_pre_idom() {
		Map<Integer, List<Integer>> tmp_map1 = this.predataflowmap;
		this.predataflowmap = this.postdataflowmap;
		this.postdataflowmap = tmp_map1;
		Set<Integer> tmp_set = this.outset;
		this.outset = this.inset;
		this.inset = tmp_set;
		Map<Integer, Integer> tmp_map2 = new HashMap<>(this.post_idom);
		this.get_post_idom();
		this.pre_idom = this.post_idom;
		this.post_idom = tmp_map2;
		tmp_map1 = this.predataflowmap;
		this.predataflowmap = this.postdataflowmap;
		this.postdataflowmap = tmp_map1;
		tmp_set = this.outset;
		this.outset = this.inset;
		this.inset = tmp_set;
	}

	public void find_loop() {
		for (Map.Entry<Integer, List<Integer>> entry : predataflowmap.entrySet()) {
			Integer edge_start = entry.getKey();
			for (Integer edge_end : entry.getValue()) {
				Integer end_dom = pre_idom.get(edge_end);
				if (end_dom != null && end_dom.equals(edge_start))
					continue;
				Integer dominator = pre_idom.get(edge_start);
				boolean isloop = false;
				while (dominator != null && !inset.contains(dominator)) {
					if (dominator.equals(edge_end)) {
						isloop = true;
						break;
					}
					dominator = pre_idom.get(dominator);
				}
				if (isloop) {
					BackEdge theloop = new BackEdge(edge_start, edge_end);
					loopset.add(theloop);
				}
			}
		}
	}

	// only keep post_idom for branching stmt
	public void filter_post_idom(){
		for (Integer inst : instset) {
			List<Integer> nextlist = predataflowmap.get(inst);
			if (nextlist.size() <= 1) {
				post_idom.remove(inst);
			}
		}
	}

	private Set<Integer> visited_for_stores = new HashSet<>();
	Integer theidom_for_stores;
	Set<Integer> thestores;

	private void dfs_for_stores(Integer inst) {
		if (inst.equals(theidom_for_stores))
			return;
		visited_for_stores.add(inst);
		Integer storen = store_num.get(inst);
		if (storen != null)
			thestores.add(storen);
		List<Integer> thenexts = predataflowmap.get(inst);
		for (Integer next : thenexts) {
			if (!visited_for_stores.contains(next))
				dfs_for_stores(next);
		}
	}

	public void get_stores() {
		for (Integer inst : instset) {
			List<Integer> nextlist = predataflowmap.get(inst);
			if (nextlist.size() > 1) {
				theidom_for_stores = post_idom.get(inst);
				thestores = new HashSet<>();
				visited_for_stores.clear();
				dfs_for_stores(inst);
				branch_stores.put(inst, thestores);
			}
		}
	}
}
