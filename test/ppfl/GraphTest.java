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

	@Test
	void domaintest() {
		boolean fail = false;
		String ppflroot = ".";
		String filepatht = ppflroot + "\\simpletests\\domaintest.java";
		String tracepatht = ppflroot + "\\test_traces\\domaintest_trace.txt";

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

		Graph pgraph = new Graph(lineinfo, tracepatht, "Simpletest");
		pgraph.observe("foo.main#14", true);
		pgraph.observe("a#3#3", false);
		pgraph.printgraph();
		pgraph.inference();
		pgraph.printprobs();

		assertFalse(fail);
	}
	
	@Test
	void mergetest() {
		boolean fail = false;
		String ppflroot = ".";
		String passpath = ppflroot + "\\simpletests\\passtest.java";
		String passtrace = ppflroot + "\\test_traces\\passtest_trace.txt";
		String failpath = ppflroot + "\\simpletests\\failtest.java";
		String failtrace = ppflroot + "\\test_traces\\failtest_trace.txt";

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

		Graph pgraph = new Graph(lineinfop, passtrace, "Passtest");
		pgraph.observe("foo.main#11", true);
		pgraph.observe("a#3#2", true);
		//pgraph.printgraph();
		//pgraph.inference();
		//pgraph.printprobs();

		pgraph.parsetrace(lineinfof, failtrace, "Failtest");
		pgraph.observe("foo.main#11", true);
		pgraph.observe("a#3#3", false);
		pgraph.printgraph();
		pgraph.inference();
		pgraph.printprobs();
		
		assertFalse(fail);
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
