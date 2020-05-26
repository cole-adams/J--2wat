/**
* Cole Adams
* CPSC 411
* Final Submission
* April 17, 2020
*/
import java.util.LinkedList;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.StringBuilder;
import java.io.*;

public class CodeGen {
	static int funcCounter;

	private StringBuilder strings;
	private int stringOffset;
	private int step;
	public CodeGen(){
		strings = new StringBuilder();
		stringOffset = 9;
		step = 0;
	}

	public void generateCode(ASTNode tree) {
		Context curContext = new Context(null, 0, "(module", -1);
		curContext.declareGlobal = true;

		insertDefaults(curContext);

		LinkedList<ASTNode> traversal = tree.preOrderTraversal();
		LinkedList<ASTNode> elseBlocks = new LinkedList<ASTNode>();

		int lastLevel = 0;
		step = 1;

		for (int i = 0; i < traversal.size(); i+=step) {
			step = 1;
			ASTNode node = traversal.get(i);

			while (node.level <= curContext.startLevel) {
				String sContext = curContext.closeContext();
				curContext.parent.insertStatement(sContext);
				curContext = curContext.parent;
			}

			if (elseBlocks.peek() != null && elseBlocks.peek().equals(node)) {
				String sContext = curContext.closeContext();
				curContext.parent.insertStatement(sContext);
				curContext = curContext.parent;
				Context elseContext = new Context(curContext, curContext.indents+1, "(else", node.level);
				elseBlocks.pop();
				curContext = elseContext;
			}


			switch(node.construct) {
				case "mainDec":
					ASTNode idMain = node.children.get(1);
					SymbolTable.SymbolEntry mainEntry = (SymbolTable.SymbolEntry) idMain.getAttribute("sym");
					if (mainEntry.label == null) {
						mainEntry.label = curContext.getFunc();
					}
					curContext.insertStatement("(start $" + mainEntry.label + ")", "start");
				case "funcDec":
					ASTNode idFunc = node.children.get(1);
					SymbolTable.SymbolEntry funcEntry = (SymbolTable.SymbolEntry) idFunc.getAttribute("sym");
					if (funcEntry.label == null) {
						funcEntry.label = curContext.getFunc();
					}
					curContext.insertStatement(";; " +funcEntry.name);
					Context newContext = new Context(curContext, 1, "(func $" + funcEntry.label, node.level);
					curContext = newContext;
					buildFunction(node, curContext);
					break;
				case "globVarDec":
					ASTNode idGlob = node.children.get(1);
					SymbolTable.SymbolEntry entryG = (SymbolTable.SymbolEntry) idGlob.getAttribute("sym");
					if (entryG.label == null){
						String newLabelGlob = curContext.getGlobal();
						entryG.label = newLabelGlob;
					}
					curContext.insertStatement("(global $" + entryG.label + " (mut i32) (i32.const 0))", "globalVar");
					step+=2;
					break;
				case "varDec":
					ASTNode idVar = node.children.get(1);
					String newLabel = curContext.getLabel();
					SymbolTable.SymbolEntry entry = (SymbolTable.SymbolEntry) idVar.getAttribute("sym");
					entry.label = newLabel;
					curContext.insertStatement("(local $" + newLabel + " i32)", "varDec");
					step+=2;
					break;
				case "stmtExpr":
					String assignReg = buildExpression(node.children.get(0), curContext);
					if (assignReg!=null) {
						curContext.deallocReg(assignReg);
					}
					break;
				case "while":
					ASTNode boolExp = node.children.get(0);
					String breakLabel = curContext.getBlockLabel();
					String loopLabel = curContext.getBlockLabel();
					Context newContextWb = new Context(curContext, curContext.indents+1, "(block $" + breakLabel, node.level);
					Context newContextWl = new Context(newContextWb, newContextWb.indents+1, "(loop $" + loopLabel, node.level);
					newContextWl.insertStatement("br $" + loopLabel, "repeat");
					newContextWl.breakLabel = breakLabel;
					curContext = newContextWl;
					String wResReg = buildExpression(boolExp, curContext);
					getRegValue(wResReg, curContext);
					curContext.deallocReg(wResReg);
					curContext.insertStatement("i32.eqz");
					curContext.insertStatement("br_if $" + breakLabel);
					break;
				case "break":
					curContext.insertStatement("br $" + curContext.getBreakLabel());
					break;
				case "return":
					if (node.children.size() > 0) {
						String retReg = buildExpression(node.children.get(0), curContext);
						getRegValue(retReg, curContext);
						curContext.deallocReg(retReg);
					}
					curContext.insertStatement("return");
					break;
				case "if":
				case "ifElse":
					String exp = buildExpression(node.children.get(0), curContext);
					getRegValue(exp, curContext);
					curContext.deallocReg(exp);
					Context ifContext = new Context(curContext, curContext.indents+1, "(if", node.level);
					Context thenContext = new Context(ifContext, ifContext.indents+1, "(then", node.level);
					if (node.children.size() > 2) {
						elseBlocks.push(node.children.get(2));
					}
					curContext = thenContext;
			}

			lastLevel = node.level;
		}

		while (curContext.parent!=null) {
			String sContext = curContext.closeContext();
			curContext.parent.insertStatement(sContext);
			curContext = curContext.parent;
		}
		String strs = closeStrings();
		curContext.insertStatement(strs, "string");

		String finalContext = curContext.closeContext();

		System.out.println(finalContext);
	}

	public void buildFunction(ASTNode node, Context c) {
		ASTNode formals = node.children.get(2);
		ASTNode returnType = node.children.get(0);
		step +=3;

		for (int i = 0; i < formals.children.size(); i++) {
			ASTNode param = formals.children.get(i);
			ASTNode id = param.children.get(1);
			String newLabel = c.getLabel();
			SymbolTable.SymbolEntry se = (SymbolTable.SymbolEntry) id.attributes.get("sym");
			se.label = newLabel;
			c.insertStatement("(param $" + newLabel + " i32)", "param");
			step+=3;
		}

		if (!returnType.construct.equals("void")) {
			c.insertStatement("(result i32)", "param");
			c.insertStatement("(i32.const -1)", "impReturn");
		}
	}

	public void buildFuncCall(ASTNode funcCall, Context c) {
		step += 2;
		ASTNode id = funcCall.children.get(0);
		SymbolTable.FunctionSymbolEntry idEntry = (SymbolTable.FunctionSymbolEntry) id.getAttribute("sym");
		if (idEntry.isRuntime) {
			idEntry.label = idEntry.name;
		} else if (idEntry.label == null) {
			idEntry.label = c.getFunc();
		}

		ASTNode actuals = funcCall.children.get(1);

		if (idEntry.label.equals("prints")) {
			ASTNode str = actuals.children.get(0);
			step++;
			StringParam sp = addString(str);
			c.insertStatement("i32.const " + sp.offset);
			c.insertStatement("i32.const " + sp.length);
			c.insertStatement("call $" + idEntry.label);
			return;
		}

		ArrayList<String> resultRegisters = new ArrayList<String>();

		for (ASTNode act : actuals.children) {
			String resReg = buildExpression(act, c);
			resultRegisters.add(resReg);
		}
		for (String reg : resultRegisters) {
			getRegValue(reg, c);
			c.deallocReg(reg);
		}

		c.insertStatement("call $" + idEntry.label);
	}

	public String buildExpression(ASTNode exp, Context c) {
		step += 1;

		boolean isOr = false;
		String op = null;

		switch(exp.construct) {
			case "funcCall":
				buildFuncCall(exp, c);
				String returnType = (String) exp.getAttribute("sig");
				if (!returnType.equals("void")) {
					String funcRes = c.allocReg();
					setRegValue(funcRes, c);
					return funcRes;
				}
				return null;
			case "num":
				String val = (String) exp.getAttribute("attr");
				c.insertStatement("i32.const " + val);
				String numResReg = c.allocReg();
				setRegValue(numResReg, c);
				return numResReg;
			case "id":
				SymbolTable.SymbolEntry entry = (SymbolTable.SymbolEntry) exp.getAttribute("sym");
				if (entry.label == null) {
					if (entry.isGlobal) {
						entry.label = c.getGlobal();
					} else {
						entry.label = c.getLabel();
					}
				}
				if (entry.isGlobal) {
					c.insertStatement("global.get $" + entry.label);
				} else {
					c.insertStatement("local.get $" + entry.label);
				}
				String idResReg = c.allocReg();
				setRegValue(idResReg, c);
				return idResReg;
			case "true":
				c.insertStatement("i32.const 1");
				String trueResReg = c.allocReg();
				setRegValue(trueResReg, c);
				return trueResReg;
			case "false":
				c.insertStatement("i32.const 0");
				String falseResReg = c.allocReg();
				setRegValue(falseResReg, c);
				return falseResReg;
			case "=":
				ASTNode idA = exp.children.get(0);
				step++;
				String assignResReg = buildExpression(exp.children.get(1), c);
				getRegValue(assignResReg, c);
				SymbolTable.SymbolEntry entryA = (SymbolTable.SymbolEntry) idA.getAttribute("sym");
				if (entryA.label==null && entryA.isGlobal) {
					entryA.label = c.getGlobal();
				}
				if (entryA.isGlobal) {
					c.insertStatement("global.set $" + entryA.label);
				} else {
					c.insertStatement("local.set $" + entryA.label);
				}
				return assignResReg;
			case "||":
				isOr = true;
			case "&&":
				String leftB = buildExpression(exp.children.get(0), c);
				getRegValue(leftB, c);
				if (isOr) {
					c.insertStatement("i32.eqz");
				}
				Context ifC = new Context(c, c.indents+1, "(if", exp.level);
				Context ifThenC = new Context(ifC, ifC.indents+1, "(then", exp.level);
				String rightB = buildExpression(exp.children.get(1), ifThenC);
				getRegValue(rightB, ifThenC);
				setRegValue(leftB, ifThenC);
				ifThenC.deallocReg(rightB);
				String ifThenCClose = ifThenC.closeContext();
				ifC.insertStatement(ifThenCClose);
				String ifCClose = ifC.closeContext();
				c.insertStatement(ifCClose);
				return leftB;
			case "!":
				String child = buildExpression(exp.children.get(0), c);
				getRegValue(child, c);
				c.insertStatement("i32.eqz");
				setRegValue(child, c);
				return child;
			case "-":
				if (exp.children.size() == 1) {
					String childM = buildExpression(exp.children.get(0), c);
					c.insertStatement("i32.const 0");
					getRegValue(childM, c);
					c.insertStatement("i32.sub");
					setRegValue(childM, c);
					return childM;
				} else {
					op = "sub";
				}
				break;
			case "==":
				op = "eq";
				break;
			case "!=":
				op = "ne";
				break;
			case "<":
				op = "lt_s";
				break;
			case ">":
				op = "gt_s";
				break;
			case "<=":
				op = "le_s";
				break;
			case ">=":
				op = "ge_s";
				break;
			case "+":
				op = "add";
				break;
			case "*":
				op = "mul";
				break;
			case "/":
				op = "div_s";
				break;
			case "%":
				op = "rem_s";
				break;
		}

		String left = buildExpression(exp.children.get(0), c);
		String right = buildExpression(exp.children.get(1), c);
		getRegValue(left, c);
		getRegValue(right, c);
		c.insertStatement("i32." + op);
		setRegValue(left, c);
		c.deallocReg(right);
		return left;
	}

	public void getRegValue(String reg, Context c) {
		c.insertStatement("local.get $" + reg);
	}

	public void setRegValue(String reg, Context c) {
		c.insertStatement("local.set $" + reg);
	}

	public StringParam addString(ASTNode str) {
		String value = (String) str.getAttribute("attr");
		int retOff = stringOffset;

		int length = 0;
		char[] charArr = value.toCharArray();
		for (int i = 0; i < charArr.length; i++) {
			length++;
			if (charArr[i] == '\\') {
				i++;
				if (i+1 < charArr.length) {
					String esc = "" + charArr[i-1] + charArr[i] + charArr[i+1];
					if (esc.matches("\\[A-Fa-f0-9]{2}")) {
						i++;
					}
				}
			}
		}
		strings.append("\t(data 0 (i32.const " + retOff +") \"" + value +"\")\n");
		stringOffset+=length;
		return new StringParam(retOff, length);
	}

	public String closeStrings() {
		int memory = (int)Math.ceil((double)stringOffset/65536.0);
		strings.append("\t(memory " + memory + ")\n");
		strings.deleteCharAt(0);
		return strings.toString();
	}

	public void insertDefaults(Context c) {
		c.insertStatement("(import \"host\" \"exit\" (func $halt))", "import");
		c.insertStatement("(import \"host\" \"getchar\" (func $getchar (result i32)))", "import");
		c.insertStatement("(import \"host\" \"putchar\" (func $printc (param i32)))", "import");

		try {
			BufferedReader bf = new BufferedReader(new FileReader("Runtime.wat"));
			String line;
			while ((line = bf.readLine()) != null) {
				c.insertStatement(line, "runtime");
			}
		} catch (IOException ioe) {
			System.err.println("Error loading runtime library");
			System.exit(-1);
		}
	}

	private class Context {

		Context parent;
		int indents;
		String start;
		int startLevel;

		String breakLabel;

		HashMap<String, StringBuilder> sections;

		StringBuilder main;

		boolean declareLocal;
		boolean declareGlobal;
		int labelCounter;
		int globalCounter;
		int regCounter;
		int blockCounter;

		ArrayList<String> registers;

		Context(Context parent, int indents, String start, int startLevel) {
			this.parent = parent;
			this.indents = indents;
			this.startLevel = startLevel;
			this.start = start;
			sections = new HashMap<String, StringBuilder>();
			main = new StringBuilder();

			labelCounter = 0;
			regCounter = 0;
			globalCounter = 0;
			blockCounter = 0;
			registers = new ArrayList<String>();

			if (parent != null && parent.declareGlobal) {
				declareLocal = true;
			}

			breakLabel = null;
		}

		public void insertStatement(String value, String section) {
			StringBuilder sb;
			if (sections.containsKey(section)) {
				sb = sections.get(section);
			} else {
				sb = new StringBuilder();
				sections.put(section, sb);
			}

			tabs(sb, indents+1);
			sb.append(value);
			sb.append("\n");
		}

		public void insertStatement(String value) {
			tabs(main, indents+1);
			main.append(value);
			main.append("\n");
		}

		public String allocReg() {
			if (declareLocal) {
				if (registers.size() > 0) {
					return registers.remove(0);
				} else {
					String newReg = "R" + regCounter;
					regCounter++;
					insertStatement("(local $" + newReg + " i32)", "register");
					return newReg;
				}
			} else {
				return parent.allocReg();
			}
		}

		public void deallocReg(String reg) {
			if (declareLocal) {
				registers.add(reg);
			} else{
				parent.deallocReg(reg);
			}
		}

		public String getLabel() {
			if (declareLocal) {
				String newLabel = "L" + labelCounter;
				labelCounter++;
				return newLabel;
			} else {
				return parent.getLabel();
			}
		}

		public String getBlockLabel() {
			if (declareLocal) {
				String newLabel = "B" + blockCounter;
				blockCounter++;
				return newLabel;
			} else {
				return parent.getBlockLabel();
			}
		}

		public String getBreakLabel() {
			if (breakLabel!=null) {
				return breakLabel;
			} else {
				return parent.getBreakLabel();
			}
		}

		public String getFunc() {
			String newFunc = "F" + funcCounter;
			funcCounter++;
			return newFunc;
		}

		public String getGlobal() {
			if (declareGlobal) {
				String newGlob = "G" + globalCounter;
				globalCounter++;
				return newGlob;
			} else {
				return parent.getGlobal();
			}
		}

		public String closeContext() {
			StringBuilder sb = new StringBuilder();
			sb.append(start);
			sb.append("\n");
			appendSection(sb, "param");
			appendSection(sb, "import");
			appendSection(sb, "globalVar");
			appendSection(sb, "varDec");
			appendSection(sb, "register");
			appendSection(sb, "start");
			sb.append(main.toString());
			sb.append("\n");
			appendSection(sb, "runtime");
			appendSection(sb, "string");
			appendSection(sb, "repeat");
			appendSection(sb, "impReturn");
			tabs(sb, indents);
			sb.append(")");
			sb.append("\n");

			return sb.toString();
		}

		private void appendSection(StringBuilder out, String section) {
			if (sections.containsKey(section)) {
				StringBuilder sec = sections.get(section);
				out.append(sec.toString());
			}
		}

		private void tabs(StringBuilder sb, int tabs) {
			for (int i = 0; i < tabs; i++) {
				sb.append("\t");
			}
		}
	}

	private class StringParam {
		int length;
		int offset;

		StringParam(int o, int l) {
			length = l;
			offset = o;
		}
	}
}
