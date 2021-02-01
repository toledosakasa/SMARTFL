package ppfl.defects4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class Instrumenter {

	private static String readFileToString(String filePath) {
		StringBuilder fileData = new StringBuilder(1000);
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			char[] buf = new char[10];
			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileData.toString();
	}

	private static void writeStringToFile(String FilePath, String output) {
		try (FileWriter fw = new FileWriter(FilePath)) {
			fw.write(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String source = new String();

	private static void getFilelist(String DirPath, List<String> FileList) {
		File RootDir = new File(DirPath);
		File[] files = RootDir.listFiles();

		for (File f : files) {
			if (f.isDirectory()) {
				getFilelist(f.getAbsolutePath(), FileList);
			} else {
				if (f.getName().endsWith(".java") && (f.getName().contains("Test") || f.getName().contains("test")))
					FileList.add(f.getAbsolutePath());
			}
		}
	}

	private static String getTestDir(String projdir) {
		// System.out.println("getting test dir from:" + projdir);
		String s = projdir + "defects4j.build.properties";
		try (BufferedReader reader = new BufferedReader(new FileReader(s))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("d4j.dir.src.tests")) {
					String testdir = line.split("=")[1];
					return projdir + testdir;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String args[]) {

		boolean verboset = false;

		String proj = args[0];
		String id = args[1];

		// consistent with s.py
		String dirPath = String.format("./tmp_checkout/%s%s/", proj, id);
		String outputPath = String.format("./d4j_resources/metadata_cached/%s%s.alltests.log", proj, id);

		List<String> filelist = new ArrayList<>();
		getFilelist(getTestDir(dirPath), filelist);

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		final AST ast = AST.newAST(AST.JLS3);

		int TotalNum = filelist.size();
		int CurNum = 0;

		StringBuilder outputBuilder = new StringBuilder("");
		for (final String FilePath : filelist)

		{
			System.out.println(FilePath);
			source = readFileToString(FilePath);

			parser.setSource(source.toCharArray());
			parser.setKind(ASTParser.K_COMPILATION_UNIT);

			final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

			String PackageNamet = null;
			PackageNamet = cu.getPackage().getName().toString();
			final String PackageName = PackageNamet;
			String ClassName = cu.getClass().getName();
			String printMSG = String.format("%s.%s::", PackageName, ClassName);
			outputBuilder.append(printMSG);

			cu.accept(new ASTVisitor() {

				public boolean isInnerClass(ASTNode node) {
					while (node != null) {
						node = node.getParent();
						if (node instanceof TypeDeclaration)
							if (((TypeDeclaration) node).isInterface() == false)
								return true;
					}
					return false;
				}

				public boolean visit(MethodDeclaration node) {
					if (node.isConstructor())//
						return false;

					boolean flag = true;
					if (node.getName().toString().startsWith("test"))
						flag = false;
					else {
						List<ASTNode> l = node.modifiers();
						for (ASTNode n : l) {
							if (n.toString().startsWith("@Test")) {
								flag = false;
								break;
							}
						}
					}

					if (flag)
						return false;

					// An @test method. output it's name
					String printMSG = String.format("%s", node.getName());
					outputBuilder.append(printMSG);
					return true;
				}

			});
			CurNum++;
			System.out.println(CurNum + "/" + TotalNum);
		}
		outputBuilder.append(String.format("%n"));
		writeStringToFile(outputPath, outputBuilder.toString());
	}

}
