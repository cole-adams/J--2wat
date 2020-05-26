/**
* Cole Adams
* CPSC 411
* Milestone 3
* March 27th, 2020
*/
import java.util.LinkedList;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SemanticChecker {
	SymbolTable table;
	HashMap<String, List<OpType>> operatorTable;

	public SemanticChecker() {
		table = new SymbolTable();
		operatorTable = new HashMap<String, List<OpType>>();
		generateOperatorTable();
	}

	public void semanticCheck(ASTNode n) throws SemanticException{
		//Partial type propogation and global definitions
		passOne(n);
		//Filling in symbol table
		passTwo(n);
		//Full type check
		passThree(n);
		//Misc other
		passFour(n);
	}

	public void passOne(ASTNode n) throws SemanticException {
		LinkedList<ASTNode> trav = n.postOrderTraversal();

		for (ASTNode node : trav) {

			switch(node.construct) {
				case "int":
				case "boolean":
				case "void":
					node.parent.addAttribute("sig", node.construct);
					node.addAttribute("sig", node.construct);
					break;
				case "globVarDec":
					ASTNode id = node.children.get(1);
					String type = (String) node.attributes.remove("sig");
					String name = (String) id.attributes.get("attr");
					id.addAttribute("sig", type);
					SymbolTable.SymbolEntry se = table.new SymbolEntry(name, type, true);

					try {
						table.defineName(name, se);
					} catch (SemanticException semEx) {
						int lineno = (int) node.attributes.get("lineno");
						throw new SemanticException("An identifier is redefined within the same scope around line " + lineno);
					}
					id.addAttribute("sym", se);
					break;
				case "varDec":
					ASTNode idV = node.children.get(1);
					String typeV = (String) node.attributes.remove("sig");
					idV.addAttribute("sig", typeV);
					break;
				case "formal":
					if (node.parent.attributes.containsKey("formalList")) {
						String params = (String)node.parent.attributes.get("formalList");
						node.parent.attributes.replace("formalList", params + " " + (String)node.attributes.get("sig"));
					} else {
						String parameterList = (String)node.attributes.get("sig");
						node.parent.addAttribute("formalList", parameterList);
					}
					ASTNode idFo = node.children.get(1);
					String typeFo = (String) node.attributes.get("sig");
					idFo.addAttribute("sig", typeFo);
					break;
				case "formals":
					if (node.attributes.containsKey("formalList")) {
						String params = (String) node.attributes.remove("formalList");
						params = params.replaceAll(" ", ",");
						params = "f("+params+")";

						node.parent.addAttribute("funcSig", params);
					} else {
						node.parent.addAttribute("funcSig", "f()");
					}
					break;
				case "funcDec":
				case "mainDec":
					ASTNode idF = node.children.get(1);
					String typeF = (String) node.attributes.remove("sig");
					String sigF = (String) node.attributes.remove("funcSig");
					String nameF = (String) idF.attributes.get("attr");
					idF.addAttribute("sig", sigF);
					boolean isMain = node.construct.equals("mainDec");
					SymbolTable.FunctionSymbolEntry fse = table.new FunctionSymbolEntry(nameF, typeF, sigF, isMain);
					try {
						table.defineName(nameF, fse);
					} catch (SemanticException semEx) {
						int lineno = (int) node.attributes.get("lineno");
						throw new SemanticException("An identifier is redefined within the same scope around line " + lineno);
					}
					idF.addAttribute("sym", fse);
					break;
			}
		}
	}

	public void passTwo(ASTNode n) throws SemanticException{
		LinkedList<ASTNode> trav = n.prePostOrderTraversal();
		boolean inVarDec = false;
		boolean inFormDec = false;
		int lastLevel = 0;

		for (ASTNode node:trav) {

			switch(node.construct) {
				case "id":
					if (inVarDec || inFormDec) {
						String name = (String) node.attributes.get("attr");
						String sig = (String) node.attributes.get("sig");
						SymbolTable.SymbolEntry se = table.new SymbolEntry(name, sig, false);
						try {
							table.defineName(name, se);
						} catch (SemanticException semEx) {
							int lineno = (int) node.attributes.get("lineno");
							throw new SemanticException("An identifier is redefined within the same scope around line " + lineno);
						}
						node.addAttribute("sym", se);
					} else if(!node.attributes.containsKey("sym")){
						String name = (String) node.attributes.get("attr");
						SymbolTable.SymbolEntry se = table.lookUp(name);
						if (se == null) {
							int lineno = (int) node.attributes.get("lineno");
							throw new SemanticException("An undeclared identifier is used around line " + lineno);
						} else {
							node.addAttribute("sym", se);
						}
					}
					break;
				case "varDec":
					inVarDec = !inVarDec;
					break;
				case "formals":
					if (lastLevel <= node.level) {
						table.openScope();
						inFormDec = true;
					}
					break;
				case "block":
					if (lastLevel <= node.level && !inFormDec) {
						table.openScope();
					} else if (inFormDec) {
						inFormDec = false;
					} else {
						table.closeScope();
					}
					break;
			}

			lastLevel = node.level;
		}
	}

	public void passThree(ASTNode n) throws SemanticException{
		LinkedList<ASTNode> trav = n.postOrderTraversal();
		int mainDecs = 0;
		String returnType = null;

		for (ASTNode node : trav) {
			switch(node.construct) {
				case "==":
				case "!=":

				case "||":
				case "&&":
				case "=":
				case ">":
				case "<":
				case ">=":
				case "<=":
				case "+":
				case "*":
				case "/":
				case "%":
				case "!":
				case "-":
					List<OpType> cases = operatorTable.get(node.construct);
					String operators = (String) node.attributes.remove("ops");
					boolean match = false;
					OpType mCase = null;
					for (OpType c : cases) {
						if (c.op.equals(operators)){
							match = true;
							mCase = c;
							break;
						}
					}
					if (!match) {
						int lineno = (int) node.attributes.get("lineno");
						throw new SemanticException("There is a type mismatch for operator " + node.construct + " around line " + lineno);
					} else {
						if (!node.parent.construct.equals("stmtExpr")) {
							if (node.parent.attributes.containsKey("ops")) {
								String op = (String) node.parent.attributes.get("ops");
								node.parent.attributes.replace("ops", op + " " + mCase.res);
							} else {
								node.parent.addAttribute("ops", mCase.res);
							}
						}
					}
					break;
				case "num":
					if (node.parent.attributes.containsKey("ops")) {
						String op = (String) node.parent.attributes.get("ops");
						node.parent.attributes.replace("ops", op + " int");
					} else {
						node.parent.addAttribute("ops", "int");
					}
					node.attributes.put("sig", "int");
					break;
				case "true":
				case "false":
					if (node.parent.attributes.containsKey("ops")) {
						String op = (String) node.parent.attributes.get("ops");
						node.parent.attributes.replace("ops", op + " boolean");
					} else {
						node.parent.addAttribute("ops", "boolean");
					}
					node.attributes.put("sig", "boolean");
					break;
				case "string":
					if (node.parent.attributes.containsKey("ops")) {
						String op = (String) node.parent.attributes.get("ops");
						node.parent.attributes.replace("ops", op + " string");
					} else {
						node.parent.addAttribute("ops", "string");
					}
					break;
				case "id":
					if (node.parent.construct.equals("varDec")) {
						SymbolTable.SymbolEntry se = (SymbolTable.SymbolEntry) node.attributes.get("sym");
						node.parent.addAttribute("sig", se.type);
						node.addAttribute("sig", se.type);
					} else if (node.parent.construct.equals("funcCall")) {
						SymbolTable.FunctionSymbolEntry se = (SymbolTable.FunctionSymbolEntry) node.attributes.get("sym");
						node.addAttribute("sig", se.sig);
						node.parent.addAttribute("idType", se.sig);
						node.parent.addAttribute("sig", se.type);
					} else if (!node.parent.construct.equals("globVarDec") && !node.parent.construct.equals("formal")
										&& !node.parent.construct.equals("funcDec") && !node.parent.construct.equals("mainDec")) {
						SymbolTable.SymbolEntry se = (SymbolTable.SymbolEntry) node.attributes.get("sym");
						node.addAttribute("sig", se.type);
						if (node.parent.attributes.containsKey("ops")) {
							String op = (String) node.parent.attributes.get("ops");
							node.parent.attributes.replace("ops", op + " " + se.type);
						} else {
							node.parent.addAttribute("ops", se.type);
						}
					}
					break;
				case "funcCall":
					String idType = (String) node.attributes.remove("idType");
					String actualType = (String) node.attributes.remove("actualType");
					String sig = (String) node.attributes.get("sig");

					if (!idType.equals(actualType)) {
						int lineno = (int) node.attributes.get("lineno");
						throw new SemanticException("The number/type of arguments in a function call doesn't match the function's declaration around line " + lineno);
					}

					if (!node.parent.construct.equals("stmtExpr")) {
						if (node.parent.attributes.containsKey("ops")) {
							String op = (String) node.parent.attributes.get("ops");
							node.parent.attributes.replace("ops", op + " " + sig);
						} else {
							node.parent.addAttribute("ops", sig);
						}
					}

					break;
				case "while":
				case "if":
				case "ifElse":
					String ifOperators = (String) node.attributes.remove("ops");
					if (!ifOperators.equals("boolean")) {
						int lineno = (int) node.attributes.get("lineno");
						throw new SemanticException("An if or while condition around line " + lineno + " is not of boolean type");
					}
					break;
				case "actuals":
					String actOperators = (String) node.attributes.remove("ops");
					if (actOperators!=null) {
						actOperators = actOperators.replaceAll(" ", ",");
						String aSig = "f(" + actOperators + ")";
						node.parent.addAttribute("actualType", aSig);
					} else {
						node.parent.addAttribute("actualType", "f()");
					}
					break;
				case "mainDec":
					mainDecs++;
					break;
				case "return":
					String retType = (String) node.attributes.remove("ops");
					if (retType!=null && returnType == null) {
						returnType = retType;
					} else if (retType!=null && returnType!=null) {
						if (!returnType.equals(retType)) {
							int lineno = (int) node.attributes.get("lineno");
							throw new SemanticException("A value returned from the function around line " + lineno + " has the wrong type");
						}
					}
					break;
				case "funcDec":
					String funcType = node.children.get(0).construct;
					if (!funcType.equals("void") && !funcType.equals(returnType) && returnType!=null) {
						int lineno = (int) node.attributes.get("lineno");
						throw new SemanticException("A value returned from the function around line " + lineno + " has the wrong type");
					}
					returnType = null;
					break;
			}
		}

		if (mainDecs<1) {
			throw new SemanticException("No main declaration found");
		}
		if (mainDecs>1) {
			throw new SemanticException("Multiple main declarations found");
		}
	}

	public void passFour(ASTNode n) throws SemanticException{
		LinkedList<ASTNode> trav = n.prePostOrderTraversal();
		int scopeDepth = 0;
		int lastLevel = 0;
		boolean inFormDec = false;
		int whileCounter = 0;
		boolean isVoid = false;
		boolean hasReturn = false;
		boolean inFunction = false;

		for (ASTNode node : trav) {
			switch(node.construct) {
				case "formals":
					if (lastLevel <= node.level) {
						scopeDepth++;
						inFormDec = true;
					}
					break;
				case "block":
					if (lastLevel <= node.level && !inFormDec) {
						scopeDepth++;
					} else if (inFormDec) {
						inFormDec = false;
					} else {
						scopeDepth--;
					}
					break;
				case "varDec":
					if (scopeDepth > 1) {
						int lineno = (int) node.attributes.get("lineno");
						throw new SemanticException("A local declaration was not in the outermost block around line " + lineno);
					}
					break;
				case "mainDec":
					if (node.children.get(2).children.size() > 0) {
						throw new SemanticException("The main declaration can't have parameters");
					}
					isVoid = true;
					break;
				case "funcDec":
						if (inFunction) {
							if (!isVoid && !hasReturn) {
								int lineno = (int) node.attributes.get("lineno");
								throw new SemanticException("There is no return statement in the non-void function around line " + lineno);
							}
						} else {
							String type = node.children.get(0).construct;
							isVoid = type.equals("void");
						}
						inFunction = !inFunction;
						break;
				case "funcCall":
					ASTNode id = node.children.get(0);
					SymbolTable.FunctionSymbolEntry fse = (SymbolTable.FunctionSymbolEntry) id.attributes.get("sym");
					if (fse.isMain) {
						int lineno = (int) node.attributes.get("lineno");
						throw new SemanticException("The main function is called around line " + lineno +". The main function can't be called");
					}
					break;
				case "while":
					if (lastLevel <= node.level) {
						whileCounter++;
					} else {
						whileCounter--;
					}
					break;
				case "break":
					if (whileCounter == 0) {
						int lineno = (int) node.attributes.get("lineno");
						throw new SemanticException("The break statement around line " + lineno + " is not in a while loop");
					}
					break;
				case "return":
					if (isVoid && node.children.size() > 0) {
						int lineno = (int) node.attributes.get("lineno");
						throw new SemanticException("There is a value returned in a void function around line " + lineno);
					}
					if (!isVoid && node.children.size() == 0) {
						int lineno = (int) node.attributes.get("lineno");
						throw new SemanticException("There is no value returned in a non-void function around line " + lineno);
					}
					hasReturn = true;
					break;
			}
			lastLevel = node.level;
		}
	}

	public void generateOperatorTable() {
		operatorTable.put("||", new ArrayList<OpType>(Arrays.asList(new OpType("boolean boolean", "boolean"))));
		operatorTable.put("&&", new ArrayList<OpType>(Arrays.asList(new OpType("boolean boolean", "boolean"))));
		operatorTable.put("==", new ArrayList<OpType>(Arrays.asList(new OpType("boolean boolean", "boolean"), new OpType("int int", "boolean"))));
		operatorTable.put("!=", new ArrayList<OpType>(Arrays.asList(new OpType("boolean boolean", "boolean"), new OpType("int int", "boolean"))));
		operatorTable.put("=", new ArrayList<OpType>(Arrays.asList(new OpType("boolean boolean", "boolean"), new OpType("int int", "int"))));
		operatorTable.put("<", new ArrayList<OpType>(Arrays.asList(new OpType("int int", "boolean"))));
		operatorTable.put(">", new ArrayList<OpType>(Arrays.asList(new OpType("int int", "boolean"))));
		operatorTable.put("<=", new ArrayList<OpType>(Arrays.asList(new OpType("int int", "boolean"))));
		operatorTable.put(">=", new ArrayList<OpType>(Arrays.asList(new OpType("int int", "boolean"))));
		operatorTable.put("+", new ArrayList<OpType>(Arrays.asList(new OpType("int int", "int"))));
		operatorTable.put("*", new ArrayList<OpType>(Arrays.asList(new OpType("int int", "int"))));
		operatorTable.put("/", new ArrayList<OpType>(Arrays.asList(new OpType("int int", "int"))));
		operatorTable.put("%", new ArrayList<OpType>(Arrays.asList(new OpType("int int", "int"))));
		operatorTable.put("!", new ArrayList<OpType>(Arrays.asList(new OpType("boolean", "boolean"))));
		operatorTable.put("-", new ArrayList<OpType>(Arrays.asList(new OpType("int int", "int"), new OpType("int", "int"))));
	}

	private static class OpType {
		String op;
		String res;

		OpType(String op, String res) {
			this.op = op;
			this.res = res;
		}
	}
}
