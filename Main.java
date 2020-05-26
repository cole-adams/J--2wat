import java.io.*;
import java_cup.runtime.*;
import java.util.LinkedList;

class Main {
	public static void main(String args[]) {
		if(args.length == 0) {
			System.err.println("Please pass input file in");
			return;
		}

		String encodingName = "UTF-8";

		Symbol sym = null;
		try {
			FileInputStream stream = new FileInputStream(args[0]);
			Reader reader = new InputStreamReader(stream, encodingName);
			parser p = new parser(new Lexer(reader));
			sym = p.parse();
		} catch(FileNotFoundException e) {
			System.err.println("File not found: " + args[0]);
			System.exit(-1);
		} catch(IOException e) {
			System.err.println("IO error scanning file" + args[0]);
			System.exit(-1);
		} catch(Exception e) {
			System.err.println("An exception has occured while parsing file");
			System.exit(-1);
		}

		ASTNode tree = null;
		try {
			tree = (ASTNode)sym.value;
			tree.attachParents(1);

			SemanticChecker sc = new SemanticChecker();
			sc.semanticCheck(tree);
		} catch(SemanticException se) {
			System.err.println(se.getMessage());
			System.exit(-1);
		} catch(Exception e) {
			System.err.println("An exception has occured during semantic analysis");
			System.exit(-1);
		}

		CodeGen cg = new CodeGen();

		try {
			cg.generateCode(tree);
		} catch(Exception e) {
			System.err.println("An exception has occured during code generation");
		}
	}
}
