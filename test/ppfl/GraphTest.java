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
		// String filepatht = ppflroot + "\\simpletests\\domaintest.java";
		String filepatht = ppflroot + "\\test\\trace\\"+name+".java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\"+name+".test.log";

		final String TraceFile = tracepatht;
		final String FilePath = filepatht;
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		final AST ast = AST.newAST(AST.JLS3);

		String source = readFileToString(FilePath);
		parser.setSource(source.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		LineInfo lineinfo = new LineInfo(cu);
		ASTVisitor visitor = new LineMappingVisitor(lineinfo);
		cu.accept(visitor);
		lineinfo.print();

		//3rd parameter : test name(must be the same as in junit test, otherwise auto-oracle will fail)
		//4th parameter : test execution state (pass = true, fail = false)
		//5th parameter : using auto-oracle
		Graph pgraph = new Graph(lineinfo, tracepatht, "test", false, false);
		// pgraph.observe("foo.main#14", true);
		// pgraph.observe("a#3#3", false);
		// pgraph.observe("trace.DomainTest.test#18", true);
		// pgraph.observe("a#9#3", false);
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
		//should there be a tmp var of the return value?"
        Graph pgraph = gengraph("recursivetest");
        
	}

    @Test
	void sumtest() {
		boolean fail = false;
		//runtime error, seems to be caused by "for loop"
        Graph pgraph = gengraph("sumtest");
	}


	Graph dominit() {
		String ppflroot = ".";
		// String filepatht = ppflroot + "\\simpletests\\domaintest.java";
		String filepatht = ppflroot + "\\test\\trace\\DomainTest.java";
		String tracepatht = ppflroot + "\\test\\trace\\logs\\DomainTest.test.log";

		final String TraceFile = tracepatht;
		final String FilePath = filepatht;
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		final AST ast = AST.newAST(AST.JLS3);

		String source = readFileToString(FilePath);
		parser.setSource(source.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		LineInfo lineinfo = new LineInfo(cu);
		ASTVisitor visitor = new LineMappingVisitor(lineinfo);
		cu.accept(visitor);
		lineinfo.print();

		//3rd parameter : test name(must be the same as in junit test, otherwise auto-oracle will fail)
		//4th parameter : test execution state (pass = true, fail = false)
		//5th parameter : using auto-oracle
		Graph pgraph = new Graph(lineinfo, tracepatht, "test", false, true);
		// pgraph.observe("foo.main#14", true);
		// pgraph.observe("a#3#3", false);
		// pgraph.observe("trace.DomainTest.test#18", true);
		// pgraph.observe("a#9#3", false);
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
//		String passpath = ppflroot + "\\simpletests\\passtest.java";
//		String passtrace = ppflroot + "\\test_traces\\passtest_trace.txt";
//		String failpath = ppflroot + "\\simpletests\\failtest.java";
//		String failtrace = ppflroot + "\\test_traces\\failtest_trace.txt";
		String passpath = ppflroot + "\\test\\trace\\MergeTest.java";
		String failpath = passpath;
		String passtrace = ppflroot + "\\test\\trace\\logs\\MergeTest.pass.log";
		String failtrace = ppflroot + "\\test\\trace\\logs\\MergeTest.fail.log";

		final String TraceFilep = passtrace;
		final String FilePathp = passpath;
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		final AST astp = AST.newAST(AST.JLS3);

		String sourcep = readFileToString(FilePathp);
		parser.setSource(sourcep.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		LineInfo lineinfop = new LineInfo(cu);
		ASTVisitor visitorp = new LineMappingVisitor(lineinfop);
		cu.accept(visitorp);
		lineinfop.print();

		final String TraceFilef = failtrace;
		final String FilePathf = failpath;
		final AST astf = AST.newAST(AST.JLS3);

		String sourcef = readFileToString(FilePathf);
		parser.setSource(sourcef.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		final CompilationUnit cuf = (CompilationUnit) parser.createAST(null);
		LineInfo lineinfof = new LineInfo(cu);
		ASTVisitor visitorf = new LineMappingVisitor(lineinfof);
		cu.accept(visitorf);
		lineinfof.print();

		Graph pgraph = new Graph(lineinfop, passtrace, "pass", true, true);
		//pgraph.observe("trace.MergeTest.pass#18", true);
		//pgraph.observe("a#9#2", true);
		pgraph.parsetrace(lineinfof, failtrace, "fail", false);
		//pgraph.observe("trace.MergeTest.fail#23", true);
		//pgraph.observe("a#9#3", false);

//		Graph pgraph = new Graph(lineinfop, passtrace, "Passtest");
//		pgraph.observe("foo.main#11", true);
//		pgraph.observe("a#3#2", true);
//
//		pgraph.parsetrace(lineinfof, failtrace, "Failtest");
//		pgraph.observe("foo.main#11", true);
//		pgraph.observe("a#3#3", false);
		return pgraph;
	}

	@Test
	void mergetest() {
		Graph pgraph = mergeinit();
		pgraph.check_bp_with_bf(true);
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
