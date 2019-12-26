package ppfl;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.CompilationUnit;

public class LineInfo {
	private Map<Integer,Line> linemap;
	public final CompilationUnit cu;
	public LineInfo(CompilationUnit _cu)
	{
		linemap = new TreeMap<Integer,Line>();
		cu = _cu;
	}
	public Line getLineByPos(int pos)
	{
		int l = cu.getLineNumber(pos);
		return getLine(l);
	}
	
	public Line getLine(int l)
	{
		if(!linemap.containsKey(l))addLine(l);
		return linemap.get(l);
	}
	
	public int getLineNumber(int pos)
	{
		return cu.getLineNumber(pos);
	}
	
	public void addLine(Integer i)
	{
		linemap.put(i, new Line());
	}
	public void print()
	{
		System.out.println("lineinfo: ");
		for(Integer k : linemap.keySet())
		{
			System.out.println(k + ":");
			linemap.get(k).print();
		}
	}
}
