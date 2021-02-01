package ppfl;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.graphstream.ui.view.Viewer;
import org.junit.jupiter.api.Test;

class GraphTest {

	SimpleProbGraph gengraph(String name) {
		String ppflroot = ".";
		String filepatht = ppflroot + "\\test\\trace\\" + name + ".java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\" + name + ".test.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(false);

		// set this to false will make a cleaner graph of recursive test.
		pgraph.setAddReturnArgFactor(true);

		pgraph.parsesource(filepatht);
		pgraph.parsetrace(tracepatht, "test", false);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void gcdtest() {
		boolean fail = false;
		SimpleProbGraph pgraph = gengraph("gcdtest");
		pgraph.observe("a#9#1", true);
		pgraph.observe("b#9#1", true);
		pgraph.observe("a#9#3", true);
		pgraph.observe("a#9#4", false);
		try {
			pgraph.bp_inference();
			pgraph.bp_printprobs();
		} catch (Exception e) {
			fail = true;
			e.printStackTrace();
		}
		assertFalse(fail);
	}

	@Test
	public void rectest() {
		boolean fail = false;
		// should there be a tmp var of the return value?"
		try {
			SimpleProbGraph pgraph = gengraph("recursivetest");
			pgraph.bp_inference();
			pgraph.bp_printprobs();
		} catch (Exception e) {
			fail = true;
			e.printStackTrace();
		}
		assertFalse(fail);

	}

	@Test
	public void sumtest() {
		boolean fail = false;
		// runtime error, seems to be caused by "for loop"
		try {
			SimpleProbGraph pgraph = gengraph("sumtest");
			pgraph.bp_inference();
			pgraph.bp_printprobs();
		} catch (Exception e) {
			fail = true;
			e.printStackTrace();
		}
		assertFalse(fail);
	}

	SimpleProbGraph dominit() {
		String ppflroot = ".";
		// String filepatht = ppflroot + "\\simpletests\\domaintest.java";
		String filepatht = ppflroot + "\\test\\trace\\DomainTest.java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\DomainTest.test.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
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

	ByteCodeGraph dominit_bytecode() {
		String ppflroot = ".";
		// String filepatht = ppflroot + "\\test\\trace\\DomainTest.java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\mytrace\\DomainTest.test.log";

		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		// pgraph.observe("foo.main#14", true);
		// pgraph.observe("a#3#3", false);
		// pgraph.observe("trace.DomainTest.test#18", true);
		// pgraph.observe("a#9#3", false);
		// pgraph.parsesource(filepatht);
		// 2nd parameter : test name(must be the same as in junit test, otherwise
		// auto-oracle will fail)
		// 3rd parameter : test execution state (pass = true, fail = false)
		pgraph.parsetrace(tracepatht, "test", false);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void domaintest() {
		boolean fail = false;
		SimpleProbGraph pgraph = dominit();
		try {
			pgraph.inference();
			pgraph.printprobs();
		} catch (Exception e) {
			fail = true;
			e.printStackTrace();
		}
		assertFalse(fail);
	}

	@Test
	public void domaintest_bytecode() {
		boolean fail = false;
		ByteCodeGraph pgraph = dominit_bytecode();
		try {
			pgraph.inference();
			pgraph.printprobs();
		} catch (Exception e) {
			fail = true;
			e.printStackTrace();
		}
		assertFalse(fail);
	}

	@Test
	public void BFtest() {
		boolean fail = false;
		SimpleProbGraph pgraph = dominit();
		try {
			pgraph.bf_inference();
			pgraph.printprobs();
		} catch (Exception e) {
			fail = true;
			e.printStackTrace();
		}
		assertFalse(fail);
	}

	@Test
	public void bptest() {
		SimpleProbGraph pgraph = dominit();
		pgraph.check_bp_with_bf(true);
	}

	@Test
	public void bptest_bytegraph() {
		ByteCodeGraph pgraph = dominit_bytecode();
		pgraph.check_bp_with_bf(true);
	}

	SimpleProbGraph mergeinit() {
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\MergeTest.java";
		String failpath = passpath;
		String passtrace = ppflroot + "\\test\\trace\\logs\\MergeTest.pass.log";
		String failtrace = ppflroot + "\\test\\trace\\logs\\MergeTest.fail.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
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
	public void mergetest() {
		SimpleProbGraph pgraph = mergeinit();
		pgraph.check_bp_with_bf(true);
	}

	ByteCodeGraph gcdinit_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\gcdtest.test.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.gcdtest.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.dataflow();
		// pgraph.parsesource(passpath);
		pgraph.parsetrace(failtrace, "test", false);
		pgraph.printgraph();

		return pgraph;
	}

	@Test
	public void gcdtest_bc() {
		ByteCodeGraph pgraph = gcdinit_bc();
	}

	ByteCodeGraph d4jinit() {
		String ppflroot = ".";
		String traceBaseDir = ppflroot + "\\trace\\logs\\mytrace\\";
		String configpath = traceBaseDir + "d4jtrace.log";

		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.setTraceAllClassed(false);

		pgraph.initFromConfigFile(traceBaseDir, configpath);
		// pgraph.buildNWrongFactor();
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void d4jTest() {

		String resultfile = "InfResult";
		ByteCodeGraph.setResultLogger(resultfile);
		String graphfile = "ProbGraph";
		ByteCodeGraph.setGraphLogger(graphfile);

		ByteCodeGraph bgraph = d4jinit();
		bgraph.check_bp(true);
	}

	@Test
	public void d4jLogTest() {
		ByteCodeGraph pgraph = new ByteCodeGraph();

		String resultfile = "InfResult";
		ByteCodeGraph.setResultLogger(resultfile);
		String graphfile = "ProbGraph";
		ByteCodeGraph.setGraphLogger(graphfile);

		pgraph.setAutoOracle(true);
		pgraph.setTraceAllClassed(false);
		pgraph.initD4jProject("Lang", 3);
		pgraph.printgraph();
		pgraph.check_bp(true);
	}

	ByteCodeGraph mergeinit_bc() {
		String ppflroot = ".";
		// String passpath = ppflroot + "\\test\\trace\\MergeTest.java";
		// String failpath = passpath;
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\MergeTest.pass.log";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\MergeTest.fail.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.MergeTest.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		// pgraph.parsesource(passpath);
		pgraph.parsesource(sourcetrace);
		// pgraph.dataflow();
		pgraph.get_idom();
		pgraph.parsetrace(passtrace, "pass", true);
		// pgraph.observe("trace.MergeTest.pass#18", true);
		// pgraph.observe("a#9#2", true);
		// pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace, "fail", false);
		// pgraph.observe("trace.MergeTest.fail#23", true);
		// pgraph.observe("a#9#3", false);
		pgraph.printgraph();

		return pgraph;
	}

	@Test
	public void mergetest_bc() {
		ByteCodeGraph pgraph = mergeinit_bc();
		System.setProperty("org.graphstream.ui", "swing");
		// for(org.graphstream.graph.Node n:pgraph.viewgraph) {
		// System.out.println(n.getId());
		// n.attributeKeys().forEach(attr -> {
		// System.out.println(attr + ": "+ n.getAttribute(attr));
		// });
		// }
		pgraph.check_bp(true);
		pgraph.addviewlabel();
		Viewer viewer = pgraph.viewgraph.display();
		try {
			Thread.sleep(8000);
		} catch (Exception e) {
			System.exit(0);
		}
		pgraph.viewgraph.setAttribute("ui.screenshot", ".\\view\\test1.png");
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			System.exit(0);
		}
		// Viewer viewer2 = pgraph.viewgraph.display();
		// // pgraph.viewgraph.setAttribute("ui.screenshot", ".\\view\\test2.png");
		// try {
		// Thread.sleep(6000);
		// } catch (Exception e) {
		// System.exit(0);
		// }
		// try {
		// Thread.sleep(1000);
		// } catch (Exception e) {
		// System.exit(0);
		// }
	}

	ByteCodeGraph controlinit_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\ControlTest.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\ControlTest.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.ControlTest.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		// pgraph.parsesource(tt);
		// pgraph.dataflow();
		pgraph.get_idom();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void controltest_bc() {
		ByteCodeGraph pgraph = controlinit_bc();
	}

	ByteCodeGraph conditioninit_bc() {
		String ppflroot = ".";
		String fail1trace = ppflroot + "\\trace\\logs\\mytrace\\ConditionTest.fail1.log";
		String pass1trace = ppflroot + "\\trace\\logs\\mytrace\\ConditionTest.pass1.log";
		String pass2trace = ppflroot + "\\trace\\logs\\mytrace\\ConditionTest.pass2.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.ConditionTest.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		// pgraph.parsesource(tt);
		pgraph.get_idom();
		pgraph.parsetrace(fail1trace, "fail1", false);
		pgraph.parsetrace(pass1trace, "pass1", true);
		pgraph.parsetrace(pass2trace, "pass2", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void conditiontest_bc() {
		ByteCodeGraph pgraph = conditioninit_bc();
	}

	ByteCodeGraph switchinit_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\SwitchTest.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\SwitchTest.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.SwitchTest.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void switchtest_bc() {
		ByteCodeGraph pgraph = switchinit_bc();
		pgraph.check_bp(true);
	}

	ByteCodeGraph predinit_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\PredTest.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\PredTest.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.PredTest.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void predtest_bc() {
		ByteCodeGraph pgraph = predinit_bc();

		System.setProperty("org.graphstream.ui", "swing");
		pgraph.check_bp(true);
		pgraph.addviewlabel();
		Viewer viewer = pgraph.viewgraph.display();
		try {
			Thread.sleep(8000);
		} catch (Exception e) {
			System.exit(0);
		}
		pgraph.viewgraph.setAttribute("ui.screenshot", ".\\view\\predtest.png");
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			System.exit(0);
		}
	}

	ByteCodeGraph pred2init_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\PredTest2.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\PredTest2.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.PredTest2.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void pred2test_bc() {
		ByteCodeGraph pgraph = pred2init_bc();
		pgraph.check_bp(true);
	}

	ByteCodeGraph pred3init_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\PredTest3.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\PredTest3.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.PredTest3.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void pred3test_bc() {
		ByteCodeGraph pgraph = pred3init_bc();
		pgraph.check_bp(true);
	}

	ByteCodeGraph pred4init_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\PredTest4.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\PredTest4.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.PredTest4.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void pred4test_bc() {
		ByteCodeGraph pgraph = pred4init_bc();
		pgraph.check_bp(true);
	}

	ByteCodeGraph pred5init_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\PredTest5.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\PredTest5.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.PredTest5.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void pred5test_bc() {
		ByteCodeGraph pgraph = pred5init_bc();
		pgraph.check_bp(true);
	}

	ByteCodeGraph pred6init_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\PredTest6.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\PredTest6.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.PredTest6.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void pred6test_bc() {
		ByteCodeGraph pgraph = pred6init_bc();
		pgraph.check_bp(true);
	}

	ByteCodeGraph pred7init_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\PredTest7.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\PredTest7.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.PredTest7.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.get_stores();
		// pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		// pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void pred7test_bc() {
		ByteCodeGraph pgraph = pred7init_bc();
	}

	// ByteCodeGraph unexcutedinit_bc() {
	// String ppflroot = ".";
	// String traceBaseDir = ppflroot + "\\trace\\logs\\mytrace\\";
	// String configpath = traceBaseDir + "unexcuted.log";

	// ByteCodeGraph pgraph = new ByteCodeGraph();
	// pgraph.setAutoOracle(true);
	// pgraph.setTraceAllClassed(false);

	// pgraph.initFromConfigFile(traceBaseDir, configpath);
	// // pgraph.buildNWrongFactor();
	// pgraph.printgraph();
	// return pgraph;
	// }

	ByteCodeGraph unexecutedinit_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\Unexecuted.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\Unexecuted.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.Unexecuted.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.get_stores();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void unexecutedtest_bc() {
		ByteCodeGraph pgraph = unexecutedinit_bc();
		pgraph.check_bp(true);
	}

	ByteCodeGraph unexecuted1init_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\Unexecuted1.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\Unexecuted1.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.Unexecuted1.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.get_stores();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void unexecuted1test_bc() {
		ByteCodeGraph pgraph = unexecuted1init_bc();
		pgraph.check_bp(true);
    }
    

    ByteCodeGraph multiinit_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\MultiTest.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\MultiTest.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.MultiTest.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
        pgraph.get_idom();
        pgraph.get_stores();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void multitest_bc() {
        String resultfile = "InfResult";
		ByteCodeGraph.setResultLogger(resultfile);
		String graphfile = "ProbGraph";
		ByteCodeGraph.setGraphLogger(graphfile);
		ByteCodeGraph pgraph = multiinit_bc();
		pgraph.check_bp(true);
	}

	ByteCodeGraph forinit_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\ForTest.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\ForTest.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.ForTest.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.get_stores();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		// for(StmtNode stmt: pgraph.stmts){
		// if(stmt.getLineNumber() == 7){
		// pgraph.buildStmtFactor(stmt, 3e-5);
		// }
		// }
		// pgraph.buildNWrongFactor();
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void fortest_bc() {
		ByteCodeGraph pgraph = forinit_bc();
		pgraph.check_bp(true);
	}

	ByteCodeGraph whileinit_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\WhileTest.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\WhileTest.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.WhileTest.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void whiletest_bc() {
		ByteCodeGraph pgraph = whileinit_bc();
		pgraph.check_bp(true);
	}

	ByteCodeGraph callinit_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\CallTest.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\CallTest.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.CallTest.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void calltest_bc() {
		ByteCodeGraph pgraph = callinit_bc();
		pgraph.check_bp(true);
	}

	ByteCodeGraph call2init_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\CallTest2.fail.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\CallTest2.pass.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.CallTest2.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void call2test_bc() {
		ByteCodeGraph pgraph = call2init_bc();
		pgraph.check_bp(true);
	}

	ByteCodeGraph badreturninit_bc() {
		String ppflroot = ".";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\BadReturnTest.fail.log";
		String passtrace1 = ppflroot + "\\trace\\logs\\mytrace\\BadReturnTest.pass1.log";
		String passtrace2 = ppflroot + "\\trace\\logs\\mytrace\\BadReturnTest.pass2.log";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.BadReturnTest.source.log";
		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.parsetrace(passtrace1, "pass1", true);
		pgraph.parsetrace(passtrace2, "pass2", true);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void badreturntest_bc() {
		ByteCodeGraph pgraph = badreturninit_bc();
		pgraph.check_bp(true);
	}

	ByteCodeGraph fourinit_bc() {
		String ppflroot = ".";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.FourTest.source.log";
		String passtrace1 = ppflroot + "\\trace\\logs\\mytrace\\FourTest.pass1.log";
		String passtrace2 = ppflroot + "\\trace\\logs\\mytrace\\FourTest.pass2.log";
		String failtrace1 = ppflroot + "\\trace\\logs\\mytrace\\FourTest.fail1.log";
		String failtrace2 = ppflroot + "\\trace\\logs\\mytrace\\FourTest.fail2.log";

		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.parsetrace(passtrace1, "pass1", true);
		pgraph.parsetrace(passtrace2, "pass2", true);
		pgraph.parsetrace(failtrace1, "fail1", false);
		pgraph.parsetrace(failtrace2, "fail2", false);
		pgraph.printgraph();

		return pgraph;
	}

	@Test
	public void fourtest_bc() {
		ByteCodeGraph pgraph = fourinit_bc();
		pgraph.check_bp(true);
	}

	SimpleProbGraph sqrtinit() {
		String ppflroot = ".";
		// String filepatht = ppflroot + "\\simpletests\\domaintest.java";
		String filepatht = ppflroot + "\\test\\trace\\SqrtTest.java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\SqrtTest.test.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(filepatht);
		pgraph.parsetrace(tracepatht, "test", false);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void sqrttest() {
		SimpleProbGraph pgraph = sqrtinit();
		pgraph.check_bp(true);
	}

	SimpleProbGraph heavyloopinit() {
		String ppflroot = ".";
		String filepatht = ppflroot + "\\test\\trace\\HeavyLoopTest.java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\HeavyLoopTest.test.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(true);

		pgraph.parsesource(filepatht);
		pgraph.parsetrace(tracepatht, "test", false);
		// pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void heavylooptest() {
		SimpleProbGraph pgraph = heavyloopinit();
		pgraph.check_bp(false);
	}

	SimpleProbGraph lightloopinit() {
		String ppflroot = ".";
		String filepatht = ppflroot + "\\test\\trace\\LightLoopTest.java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\LightLoopTest.test.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(true);

		pgraph.parsesource(filepatht);
		pgraph.parsetrace(tracepatht, "test", false);
		// pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void lightlooptest() {
		SimpleProbGraph pgraph = lightloopinit();
		pgraph.check_bp(true);
	}

	SimpleProbGraph breakinit() {
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\BreakTest.java";
		String failpath = passpath;
		String passtrace = ppflroot + "\\test\\trace\\logs\\BreakTest.pass.log";
		String failtrace = ppflroot + "\\test\\trace\\logs\\BreakTest.fail.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void breaktest() {
		SimpleProbGraph pgraph = breakinit();
		pgraph.check_bp(true);
	}

	SimpleProbGraph badreturninit() {
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\BadReturnTest.java";
		String failpath = passpath;
		String passtrace = ppflroot + "\\test\\trace\\logs\\BadReturnTest.pass.log";
		String failtrace = ppflroot + "\\test\\trace\\logs\\BadReturnTest.fail.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void badreturntest() {
		SimpleProbGraph pgraph = badreturninit();
		pgraph.check_bp(true);
	}

	SimpleProbGraph fourinit() {
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\FourTest.java";
		String failpath = passpath;
		String passtrace1 = ppflroot + "\\test\\trace\\logs\\FourTest.pass1.log";
		String passtrace2 = ppflroot + "\\test\\trace\\logs\\FourTest.pass2.log";
		String failtrace1 = ppflroot + "\\test\\trace\\logs\\FourTest.fail.log";
		String failtrace2 = ppflroot + "\\test\\trace\\logs\\FourTest.fail2.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
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
	public void fourtest() {
		SimpleProbGraph pgraph = fourinit();
		pgraph.check_bp(true);
		/*
		 * boolean fail = false; Graph pgraph = fourinit(); try { pgraph.inference();
		 * pgraph.printprobs(); } catch (Exception e) { // TODO Auto-generated catch
		 * block fail = true; e.printStackTrace(); } assertFalse(fail);
		 */
	}

	SimpleProbGraph newinit() {
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\NewTest.java";
		String failpath = passpath;
		String passtrace1 = ppflroot + "\\test\\trace\\logs\\btrace\\NewTest.pass.log";
		String failtrace1 = ppflroot + "\\test\\trace\\logs\\btrace\\NewTest.fail.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace1, "pass", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace1, "fail", false);
		pgraph.printgraph();

		return pgraph;
	}

	@Test
	public void newtest() {
		SimpleProbGraph pgraph = newinit();
		pgraph.check_bp(true);
	}

	SimpleProbGraph mulcallinit() {
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\MulcallTest.java";
		String failpath = passpath;
		String passtrace1 = ppflroot + "\\test\\trace\\logs\\btrace\\MulcallTest.pass.log";
		String failtrace1 = ppflroot + "\\test\\trace\\logs\\btrace\\MulcallTest.fail.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace1, "pass", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace1, "fail", false);
		pgraph.printgraph();

		return pgraph;
	}

	// btrace:need to have different signs;
	// TODO test: use the same sign for the new mytrace
	@Test
	public void mulcalltest() {
		SimpleProbGraph pgraph = mulcallinit();
		pgraph.check_bp(true);
	}

	SimpleProbGraph trycatchinit() {
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\TrycatchTest.java";
		// String failpath = passpath;
		String passtrace1 = ppflroot + "\\test\\trace\\logs\\btrace\\TrycatchTest.pass.log";
		// String failtrace1 = ppflroot +
		// "\\test\\trace\\logs\\btrace\\MulcallTest.fail.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace1, "pass", true);
		// pgraph.parsesource(failpath);
		// pgraph.parsetrace(failtrace1, "fail", false);
		pgraph.printgraph();

		return pgraph;
	}

	@Test
	public void trycatchtest() {
		SimpleProbGraph pgraph = trycatchinit();
		pgraph.check_bp(true);
	}

	SimpleProbGraph switchinit() {
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\SwitchTest.java";
		String failpath = passpath;
		String passtrace1 = ppflroot + "\\test\\trace\\logs\\mytrace\\SwitchTest.pass.log";
		String failtrace1 = ppflroot + "\\test\\trace\\logs\\mytrace\\SwitchTest.fail.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace1, "pass", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace1, "fail", false);
		pgraph.printgraph();

		return pgraph;
	}

	@Test
	public void switchtest() {
		SimpleProbGraph pgraph = switchinit();
		pgraph.check_bp(true);
	}

	SimpleProbGraph fullinit() {
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\FullTest.java";
		String failpath = passpath;
		String passtrace = ppflroot + "\\test\\trace\\logs\\FullTest.pass.log";
		String passtrace1 = ppflroot + "\\test\\trace\\logs\\FullTest.pass1.log";
		// String passtrace2 = ppflroot + "\\test\\trace\\logs\\FullTest.pass2.log";
		// String passtrace3 = ppflroot + "\\test\\trace\\logs\\FullTest.pass3.log";
		String passtrace4 = ppflroot + "\\test\\trace\\logs\\FullTest.pass4.log";
		String failtrace1 = ppflroot + "\\test\\trace\\logs\\FullTest.fail1.log";
		// String failtrace2 = ppflroot + "\\test\\trace\\logs\\FullTest.fail2.log";
		String failtrace3 = ppflroot + "\\test\\trace\\logs\\FullTest.fail3.log";
		String failtrace4 = ppflroot + "\\test\\trace\\logs\\FullTest.fail4.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace1, "pass1", true);
		// pgraph.parsesource(passpath);
		// pgraph.parsetrace(passtrace2, "pass2", true);
		// pgraph.parsesource(passpath);
		// pgraph.parsetrace(passtrace3, "pass3", true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace4, "pass4", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace1, "fail1", false);
		// pgraph.parsesource(failpath);
		// pgraph.parsetrace(failtrace2, "fail2", false);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace3, "fail3", false);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace4, "fail4", false);
		pgraph.printgraph();

		return pgraph;
	}

	@Test
	public void fulltest() {
		SimpleProbGraph pgraph = fullinit();
		pgraph.check_bp(true);
	}

	ByteCodeGraph fullinit_bc() {
		String ppflroot = ".";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.FullTest.source.log";
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\FullTest.pass.log";
		String passtrace1 = ppflroot + "\\trace\\logs\\mytrace\\FullTest.pass1.log";
		String passtrace2 = ppflroot + "\\trace\\logs\\mytrace\\FullTest.pass2.log";
		String passtrace3 = ppflroot + "\\trace\\logs\\mytrace\\FullTest.pass3.log";
		String passtrace4 = ppflroot + "\\trace\\logs\\mytrace\\FullTest.pass4.log";
		String failtrace1 = ppflroot + "\\trace\\logs\\mytrace\\FullTest.fail1.log";
		String failtrace2 = ppflroot + "\\trace\\logs\\mytrace\\FullTest.fail2.log";
		String failtrace3 = ppflroot + "\\trace\\logs\\mytrace\\FullTest.fail3.log";
		String failtrace4 = ppflroot + "\\trace\\logs\\mytrace\\FullTest.fail4.log";

		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.parsetrace(passtrace1, "pass1", true);
		pgraph.parsetrace(passtrace2, "pass2", true);
		pgraph.parsetrace(passtrace3, "pass3", true);
		pgraph.parsetrace(passtrace4, "pass4", true);
		pgraph.parsetrace(failtrace1, "fail1", false);
		pgraph.parsetrace(failtrace2, "fail2", false);
		pgraph.parsetrace(failtrace3, "fail3", false);
		pgraph.parsetrace(failtrace4, "fail4", false);
		pgraph.printgraph();

		return pgraph;
	}

	@Test
	public void fulltest_bc() {
		ByteCodeGraph pgraph = fullinit_bc();
		pgraph.check_bp(true);
	}

	ByteCodeGraph easyifinit_bc() {
		String ppflroot = ".";
		String sourcetrace = ppflroot + "\\trace\\logs\\mytrace\\trace.EasyIfTest.source.log";
		String passtrace1 = ppflroot + "\\trace\\logs\\mytrace\\EasyIfTest.pass1.log";
		// String passtrace2 = ppflroot +
		// "\\trace\\logs\\mytrace\\EasyIfTest.pass2.log";
		String failtrace1 = ppflroot + "\\trace\\logs\\mytrace\\EasyIfTest.fail1.log";

		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(sourcetrace);
		pgraph.get_idom();
		pgraph.parsetrace(passtrace1, "pass1", true);
		// pgraph.parsetrace(passtrace2, "pass2", true);
		pgraph.parsetrace(failtrace1, "fail1", false);
		pgraph.printgraph();

		return pgraph;
	}

	@Test
	public void easyiftest_bc() {
		ByteCodeGraph pgraph = easyifinit_bc();
		pgraph.check_bp(true);
	}

	SimpleProbGraph parainit() {
		// boolean fail = false;
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\ParaTest.java";
		String failpath = passpath;
		String passtrace = ppflroot + "\\test\\trace\\logs\\ParaTest.pass.log";
		String failtrace = ppflroot + "\\test\\trace\\logs\\ParaTest.fail.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void paratest() {
		SimpleProbGraph pgraph = parainit();
		pgraph.check_bp(true);
	}

	ByteCodeGraph parainit_bc() {
		String ppflroot = ".";
		// String passpath = ppflroot + "\\test\\trace\\ParaTest.java";
		// String failpath = passpath;
		String passtrace = ppflroot + "\\trace\\logs\\mytrace\\ParaTest.pass.log";
		String failtrace = ppflroot + "\\trace\\logs\\mytrace\\ParaTest.fail.log";

		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		// TODO find the error in this parsetrace, missing key is
		// "trace.ParaTest:func2#1:2"
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void paratest_bc() {
		ByteCodeGraph pgraph = parainit_bc();
		pgraph.check_bp(true);
	}

	SimpleProbGraph modinit() {
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\ModTest.java";
		String failpath = passpath;
		String passtrace = ppflroot + "\\test\\trace\\logs\\ModTest.pass.log";
		String failtrace = ppflroot + "\\test\\trace\\logs\\ModTest.fail.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.printgraph();

		return pgraph;
	}

	@Test
	public void modtest() {
		SimpleProbGraph pgraph = modinit();
		pgraph.check_bp(true);
	}

	ByteCodeGraph modinit_bc() {
		String ppflroot = ".";
		// String passpath = ppflroot + "\\test\\trace\\ModTest.java";
		// String failpath = passpath;
		String passtrace = ppflroot + "\\test\\trace\\logs\\mytrace\\ModTest.pass.log";
		String failtrace = ppflroot + "\\test\\trace\\logs\\mytrace\\ModTest.fail.log";

		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void modtest_bc() {
		ByteCodeGraph pgraph = modinit_bc();
		pgraph.check_bp(true);
	}

	SimpleProbGraph branchinit() {
		String ppflroot = ".";
		String passpath = ppflroot + "\\test\\trace\\BranchTest.java";
		String failpath = passpath;
		String passtrace = ppflroot + "\\test\\trace\\logs\\BranchTest.pass.log";
		String failtrace = ppflroot + "\\test\\trace\\logs\\BranchTest.fail.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsesource(passpath);
		pgraph.parsetrace(passtrace, "pass", true);
		pgraph.parsesource(failpath);
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.printgraph();

		return pgraph;
	}

	@Test
	public void branchtest() {
		SimpleProbGraph pgraph = branchinit();
		pgraph.check_bp(true);
	}

	SimpleProbGraph simpleflowinit() {
		String ppflroot = ".";
		String filepatht = ppflroot + "\\test\\trace\\SimpleFlowTest.java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\SimpleFlowTest.fail.log";

		SimpleProbGraph pgraph = new SimpleProbGraph();
		pgraph.setAutoOracle(true);

		pgraph.parsesource(filepatht);
		pgraph.parsetrace(tracepatht, "fail", false);
		// pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void simpleflowtest() {
		SimpleProbGraph pgraph = simpleflowinit();
		pgraph.check_bp(true);
	}

	ByteCodeGraph simpleflowinit_bc() {
		String ppflroot = ".";
		// String passpath = ppflroot + "\\test\\trace\\SimpleFlowTest.java";
		// String failpath = passpath;
		String failtrace = ppflroot + "\\test\\trace\\logs\\mytrace\\SimpleFlowTest.fail.log";

		ByteCodeGraph pgraph = new ByteCodeGraph();
		pgraph.setAutoOracle(true);
		pgraph.parsetrace(failtrace, "fail", false);
		pgraph.printgraph();
		return pgraph;
	}

	@Test
	public void simpleflowtest_bc() {
		ByteCodeGraph pgraph = simpleflowinit_bc();
		pgraph.check_bp(true);
	}

	public static String readFileToString(String filePath) {
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
