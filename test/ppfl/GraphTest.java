package ppfl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.Test;

class GraphTest {

	Graph gengraph(String name) {
		String ppflroot = ".";
		String filepatht = ppflroot + "\\test\\trace\\" + name + ".java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\" + name + ".test.log";

		Graph pgraph = new Graph();
		pgraph.setAutoOracle(false);

		// set this to false will make a cleaner graph of recursive test.
		pgraph.setAddReturnArgFactor(true);

		pgraph.parsesource(filepatht);
		pgraph.parsetrace(tracepatht, "test", false);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	void gcdtest() {
		boolean fail = false;
		Graph pgraph = gengraph("gcdtest");
		pgraph.observe("a#9#1", true);
		pgraph.observe("b#9#1", true);
		pgraph.observe("a#9#3", true);
		pgraph.observe("a#9#4", false);
		try {
			pgraph.bp_inference();
			pgraph.bp_printprobs();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			fail = true;
			e.printStackTrace();
		}
		assertFalse(fail);
	}

	@Test
	void rectest() {
		boolean fail = false;
		// should there be a tmp var of the return value?"
		Graph pgraph = gengraph("recursivetest");

	}

	@Test
	void sumtest() {
		boolean fail = false;
		// runtime error, seems to be caused by "for loop"
		Graph pgraph = gengraph("sumtest");
	}

	Graph dominit() {
		String ppflroot = ".";
		// String filepatht = ppflroot + "\\simpletests\\domaintest.java";
		String filepatht = ppflroot + "\\test\\trace\\DomainTest.java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\DomainTest.test.log";

		Graph pgraph = new Graph();
		pgraph.setAutoOracle(true);
		// pgraph.observe("foo.main#14", true);
		// pgraph.observe("a#3#3", false);
		// pgraph.observe("trace.DomainTest.test#18", true);
		// pgraph.observe("a#9#3", false);
		pgraph.parsesource(filepatht);
		// 2nd parameter : test name(must be the same as in junit test, otherwise
		// auto-oracle will fail)
		// 3rd parameter : test execution state (pass = true, fail = false)
		pgraph.parsetrace(tracepatht, "test", false);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	void domaintest() {
		boolean fail = false;
		Graph pgraph = dominit();
		try {
			pgraph.inference();
			pgraph.printprobs();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			fail = true;
			e.printStackTrace();
		}
		assertFalse(fail);
	}

	@Test
	void BFtest() {
		boolean fail = false;
		Graph pgraph = dominit();
		try {
			pgraph.bf_inference();
			pgraph.printprobs();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			fail = true;
			e.printStackTrace();
		}
		assertFalse(fail);
	}

	@Test
	void bptest() {
		Graph pgraph = dominit();
		pgraph.check_bp_with_bf(true);
	}

	Graph mergeinit() {
		boolean fail = false;
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\MergeTest.java";
		String failpath = passpath;
		String passtrace = ppflroot + "\\test\\trace\\logs\\MergeTest.pass.log";
		String failtrace = ppflroot + "\\test\\trace\\logs\\MergeTest.fail.log";

		Graph pgraph = new Graph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace, "pass", true);
		// pgraph.observe("trace.MergeTest.pass#18", true);
		// pgraph.observe("a#9#2", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace, "fail", false);
		// pgraph.observe("trace.MergeTest.fail#23", true);
		// pgraph.observe("a#9#3", false);
		pgraph.printgraph();

		return pgraph;
	}

	@Test
	void mergetest() {
		Graph pgraph = mergeinit();
		pgraph.check_bp_with_bf(true);
	}

	Graph sqrtinit() {
		String ppflroot = ".";
		// String filepatht = ppflroot + "\\simpletests\\domaintest.java";
		String filepatht = ppflroot + "\\test\\trace\\SqrtTest.java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\SqrtTest.test.log";

		Graph pgraph = new Graph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(filepatht);
		pgraph.parsetrace(tracepatht, "test", false);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	void sqrttest() {
		Graph pgraph = sqrtinit();
		pgraph.check_bp(true);
	}

	Graph heavyloopinit() {
		String ppflroot = ".";
		String filepatht = ppflroot + "\\test\\trace\\HeavyLoopTest.java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\HeavyLoopTest.test.log";

		Graph pgraph = new Graph();
		pgraph.setAutoOracle(true);

		pgraph.parsesource(filepatht);
		pgraph.parsetrace(tracepatht, "test", false);
		// pgraph.printgraph();
		return pgraph;
	}

	@Test
	void heavylooptest() {
		Graph pgraph = heavyloopinit();
		pgraph.check_bp(false);
	}

	Graph lightloopinit() {
		String ppflroot = ".";
		String filepatht = ppflroot + "\\test\\trace\\LightLoopTest.java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\LightLoopTest.test.log";

		Graph pgraph = new Graph();
		pgraph.setAutoOracle(true);

		pgraph.parsesource(filepatht);
		pgraph.parsetrace(tracepatht, "test", false);
		// pgraph.printgraph();
		return pgraph;
	}

	@Test
	void lightlooptest() {
		Graph pgraph = lightloopinit();
		pgraph.check_bp(true);
	}
	
	Graph breakinit() {
		boolean fail = false;
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\BreakTest.java";
		String failpath = passpath;
		String passtrace = ppflroot + "\\test\\trace\\logs\\BreakTest.pass.log";
		String failtrace = ppflroot + "\\test\\trace\\logs\\BreakTest.fail.log";

		Graph pgraph = new Graph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	void breaktest() {
		Graph pgraph = breakinit();
		pgraph.check_bp(true);
	}
	
	Graph badreturninit() {
		boolean fail = false;
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\BadReturnTest.java";
		String failpath = passpath;
		String passtrace = ppflroot + "\\test\\trace\\logs\\BadReturnTest.pass.log";
		String failtrace = ppflroot + "\\test\\trace\\logs\\BadReturnTest.fail.log";

		Graph pgraph = new Graph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	void badreturntest() {
		Graph pgraph = badreturninit();
		pgraph.check_bp(true);
	}

	Graph fourinit() {
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\FourTest.java";
		String failpath = passpath;
		String passtrace1 = ppflroot + "\\test\\trace\\logs\\FourTest.pass1.log";
		String passtrace2 = ppflroot + "\\test\\trace\\logs\\FourTest.pass2.log";
		String failtrace1 = ppflroot + "\\test\\trace\\logs\\FourTest.fail.log";
		String failtrace2 = ppflroot + "\\test\\trace\\logs\\FourTest.fail2.log";
		
		Graph pgraph = new Graph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace1, "pass1", true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace2, "pass2", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace1, "fail", false);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace2, "fail2", false);
		pgraph.printgraph();

		return pgraph;
	}
	
	@Test
	void fourtest() {
		Graph pgraph = fourinit();
		pgraph.check_bp(true);
		/*
		boolean fail = false;
		Graph pgraph = fourinit();
		try {
			pgraph.inference();
			pgraph.printprobs();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			fail = true;
			e.printStackTrace();
		}
		assertFalse(fail);
		*/
	}
	
	Graph modinit() {
		boolean fail = false;
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\ModTest.java";
		String failpath = passpath;
		String passtrace = ppflroot + "\\test\\trace\\logs\\ModTest.pass.log";
		String failtrace = ppflroot + "\\test\\trace\\logs\\ModTest.fail.log";

		Graph pgraph = new Graph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.printgraph();

		return pgraph;
	}

	@Test
	void modtest() {
		Graph pgraph = modinit();
		pgraph.check_bp(true);
	}
	
	Graph branchinit() {
		boolean fail = false;
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\BranchTest.java";
		String failpath = passpath;
		String passtrace = ppflroot + "\\test\\trace\\logs\\BranchTest.pass.log";
		String failtrace = ppflroot + "\\test\\trace\\logs\\BranchTest.fail.log";

		Graph pgraph = new Graph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.printgraph();

		return pgraph;
	}

	@Test
	void branchtest() {
		Graph pgraph = branchinit();
		pgraph.check_bp(true);
	}
	
	Graph simpleflowinit() {
		String ppflroot = ".";
		String filepatht = ppflroot + "\\test\\trace\\SimpleFlowTest.java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\SimpleFlowTest.fail.log";

		Graph pgraph = new Graph();
		pgraph.setAutoOracle(true);

		pgraph.parsesource(filepatht);
		pgraph.parsetrace(tracepatht, "fail", false);
		// pgraph.printgraph();
		return pgraph;
	}

	@Test
	void simpleflowtest() {
		Graph pgraph = simpleflowinit();
		pgraph.check_bp(true);
	}
	
	private static String readFileToString(String filePath) {
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filePath));
			char[] buf = new char[10];
			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileData.toString();
	}

}
