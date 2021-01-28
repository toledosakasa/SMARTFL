package ppfl;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParseInfo {

	private static Logger debugLogger = LoggerFactory.getLogger("Debugger");

	Map<String, String> tracemap;
	public String traceclass;
	public String tracemethod;
	public int linenumber;
	public int byteindex;
	public int form;
	public String opcode;

	public ParseInfo() {
		tracemap = new HashMap<>();
	}

	public void setDomain(String traceClass, String traceMethod) {
		tracemap = new HashMap<>();
		this.traceclass = traceClass;
		this.tracemethod = traceMethod;
	}

	public String getlineinfo() {
		return String.format("%s#%s#%d#%d", this.traceclass, this.tracemethod, this.linenumber, this.byteindex);
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
		this.traceclass = lineinfos[0];
		this.tracemethod = lineinfos[1];
		this.linenumber = Integer.valueOf(lineinfos[2]);
		this.byteindex = Integer.valueOf(lineinfos[3]);
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

	public void print() {
		for (Map.Entry<String, String> s : tracemap.entrySet()) {
			debugLogger.info(s + "=" + s.getValue());
		}
	}
}
