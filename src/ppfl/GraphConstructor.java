package ppfl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;

public class GraphConstructor {

	public static void main(String[] args) {
		boolean verboset = false;

		// Create a Parser
		CommandLineParser cmdlparser = new DefaultParser();
		Options options = new Options();
		options.addOption("S", "DirPath", true, "input file");
		options.addOption("T", "TraceFile", true, "output file");
		options.addOption("P", "PatchFile", true, "patch file");
		options.addOption("F", "PatchedFile", true, "patched file");
		options.addOption("v", "Verbose", false, "verbose debug");
		// Parse the program arguments
		CommandLine commandLine = null;
		try {
			commandLine = cmdlparser.parse(options, args);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		// Set the appropriate variables based on supplied options
		String ppflroot = ".";
		String filepatht = ppflroot + "\\simpletests\\foo.java";
		String tracepatht = ppflroot + "\\test_traces\\tmp.txt";

		if (commandLine.hasOption('S')) {
			filepatht = commandLine.getOptionValue('S');
		}
		if (commandLine.hasOption('T')) {
			tracepatht = commandLine.getOptionValue('T');
		}
		if (commandLine.hasOption('v')) {
			verboset = true;
		}

		final String TraceFile = tracepatht;
		final boolean verbose = verboset;
		final String FilePath = filepatht;
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		final AST ast = AST.newAST(AST.JLS3);

		System.out.println(FilePath);
		String source = readFileToString(FilePath);
		parser.setSource(source.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		LineInfo lineinfo = new LineInfo(cu);
		ASTVisitor visitor = new LineMappingVisitor(lineinfo);
		cu.accept(visitor);
		lineinfo.print();
		// TODO construct Pgraph by lineinfo and trace.

		Graph pgraph = new Graph(lineinfo, tracepatht, "Simpletest");
		pgraph.observe("foo.main#14", true);
		pgraph.observe("a#3#3", false);
		pgraph.printgraph();

		pgraph.inference();

		pgraph.printprobs();
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
