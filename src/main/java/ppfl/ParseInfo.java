package ppfl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ppfl.instrumentation.TraceDomain;

public class ParseInfo {

	private static Logger debugLogger = LoggerFactory.getLogger("Debugger");

	Map<String, String> tracemap;
	public TraceDomain domain;
	// public String traceclass;
	// public String tracemethod;
	// public String signature;
	public int linenumber;
	public int byteindex;
	public int form;
	public String opcode;

	// for untraced calls

	// if set to true, then an exception is throw in the callee.
	public boolean exceptionThrow = false;
	// if set to true, then this is an dummy trace suggesting a normal return point
	// for an untraced call.
	public boolean isReturnMsg = false;
	// if set to true, then this untraced call is neither catched nor returned.
	// e.g. thrown but not catched in current method.
	public boolean noMatch = false;

	public ParseInfo() {
		tracemap = new HashMap<>();
	}

	public void setDomain(TraceDomain domain) {
		tracemap = new HashMap<>();
		this.domain = domain;
	}

	public String getlineinfo() {
		return String.format("%s#%d#%d", this.domain, this.linenumber, this.byteindex);
	}

	public boolean isInvoke() {
		return this.form >= 182 && this.form <= 186;
	}

	// FIXME: this could be unsound.
	// catch block --> astore, but not vice versa.
	public boolean isCatch() {
		return this.form == 58 || (this.form >= 75 && this.form <= 78);
	}

	public String getCallClass() {
		return this.tracemap.get("callclass");
	}

	public boolean isUntracedInvoke(Set<String> tracedClass) {
		String callclass = this.tracemap.get("callclass");
		return this.isInvoke() && !tracedClass.contains(callclass);
	}

	public boolean matchDomain(ParseInfo oth) {
		return oth != null && oth.domain.equals(this.domain);
	}

	public boolean matchReturn(ParseInfo returnMsg, boolean matchCatch) {
		if (!this.matchDomain(returnMsg) || (!returnMsg.isReturnMsg && !matchCatch)) {
			return false;
		}
		String calltype = this.tracemap.get("calltype");
		String callclass = this.tracemap.get("callclass");
		String callname = this.tracemap.get("callname");

		if (callclass != null && calltype != null && callname != null) {
			if (matchCatch && returnMsg.isCatch()) {
				return true;
			}
			String msgcalltype = returnMsg.tracemap.get("calltype");
			String msgcallclass = returnMsg.tracemap.get("callclass");
			String msgcallname = returnMsg.tracemap.get("callname");
			return (calltype.equals(msgcalltype) && callclass.equals(msgcallclass) && callname.equals(msgcallname));
		}
		return false;
	}

	public boolean isNextInst(ParseInfo other) {
		return this.matchDomain(other) && other.getintvalue("nextinst").equals(this.byteindex);
	}

	public void setException() {
		this.exceptionThrow = true;
	}

	public void setNoMatch() {
		this.noMatch = true;
	}

	private void checkAndPut(Map<String, String> m, String key, String value) {
		if (Integer.parseInt(value) >= 0) {
			m.put(key, value);
		}
	}

	public void parseString(String encoded) {
		String[] split = encoded.split(",");
		// int comp[] = { opc, popn, pushn, load, store, nextinst };
		this.form = Integer.parseInt(split[0]);
		checkAndPut(tracemap, "popnum", split[1]);
		checkAndPut(tracemap, "pushnum", split[2]);
		checkAndPut(tracemap, "load", split[3]);
		checkAndPut(tracemap, "store", split[4]);
		checkAndPut(tracemap, "nextinst", split[7]);
		for (int i = 8; i < split.length; i++) {
			String[] splitinstinfo = split[i].split("=");
			if (splitinstinfo.length < 2) {
				System.err.println(encoded);
			}
			String infotype = splitinstinfo[0];
			String infovalue = splitinstinfo[1];
			tracemap.put(infotype, infovalue);
		}
	}

	public ParseInfo(String trace) {
		tracemap = new HashMap<>();
		String returnMsgPrefix = "RET@";
		if (trace.startsWith(returnMsgPrefix)) {
			trace = trace.substring(returnMsgPrefix.length());
			this.isReturnMsg = true;
		}
		String[] split = trace.split(",");
		for (String instinfo : split) {
			String[] splitinstinfo = instinfo.split("=");
			if (splitinstinfo.length < 2) {
				System.err.println(trace);
			}
			String infotype = splitinstinfo[0];
			String infovalue = splitinstinfo[1];
			tracemap.put(infotype, infovalue);
		}
		String[] lineinfos = this.getvalue("lineinfo").split("#");
		// this.traceclass = lineinfos[0];
		// this.tracemethod = lineinfos[1];
		// this.signature = this.getvalue("sig");
		this.domain = new TraceDomain(lineinfos[0], lineinfos[1], lineinfos[2]);
		this.linenumber = Integer.valueOf(lineinfos[3]);
		this.byteindex = Integer.valueOf(lineinfos[4]);
		String[] opcodeinfos = this.getvalue("opcode").split("\\(|\\)");
		this.form = Integer.valueOf(opcodeinfos[0]);
		this.opcode = opcodeinfos[1];
	}

	public String getvalue(String stype) {
		if (this.tracemap.containsKey(stype))
			return this.tracemap.get(stype);
		return null;
	}

	public Integer getintvalue(String stype) {
		if (this.tracemap.containsKey(stype))
			return Integer.valueOf(this.tracemap.get(stype));
		return null;
	}

	public Integer getAddressFromStack() {
		if (this.tracemap.containsKey("stack")) {
			String[] stackValue = this.tracemap.get("stack").split(":");
			if (stackValue[0].contentEquals("Obj"))
				return Integer.valueOf(stackValue[1]);
		}
		return null;
	}

	public void debugprint() {
		if (this.isReturnMsg)
			System.err.println("@return:");
		for (Map.Entry<String, String> s : tracemap.entrySet()) {
			System.err.println("\t" + s);
		}
	}

	public void print() {
		for (Map.Entry<String, String> s : tracemap.entrySet()) {
			debugLogger.info(s + "=" + s.getValue());
		}
	}
}
