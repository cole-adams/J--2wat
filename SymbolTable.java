import java.util.LinkedList;
import java.util.HashMap;

public class SymbolTable {
	LinkedList<HashMap<String, SymbolEntry>> scopeStack;

	public SymbolTable() {
		scopeStack = new LinkedList<HashMap<String, SymbolEntry>>();
		initializeScopeStack();
	}

	private void initializeScopeStack() {
		HashMap<String, SymbolEntry> predefined = new HashMap<String, SymbolEntry>();

		predefined.put("getchar", new FunctionSymbolEntry("getchar", "int", "f()", false, true));
		predefined.put("halt", new FunctionSymbolEntry("halt", "void", "f()", false, true));
		predefined.put("printb", new FunctionSymbolEntry("printb", "void", "f(boolean)", false, true));
		predefined.put("printc", new FunctionSymbolEntry("printc", "void", "f(int)", false, true));
		predefined.put("printi", new FunctionSymbolEntry("printi", "void", "f(int)", false, true));
		predefined.put("prints", new FunctionSymbolEntry("prints", "void", "f(string)", false, true));

		scopeStack.push(predefined);

		HashMap<String, SymbolEntry> global = new HashMap<String, SymbolEntry>();
		scopeStack.push(global);
	}

	public void openScope() {
		scopeStack.push(new HashMap<String, SymbolEntry>());
	}

	public void closeScope() {
		scopeStack.pop();
	}

	public SymbolEntry lookUp(String name) {
		for (HashMap<String, SymbolEntry> scope : scopeStack) {
			if (scope.containsKey(name)) {
				return scope.get(name);
			}
		}

		return null;
	}

	public void defineName(String name, SymbolEntry se) throws SemanticException{
		if (scopeStack.getFirst().containsKey(name)) {
			throw new SemanticException("An identifier is redefined within the same scope");
		}

		scopeStack.getFirst().put(name, se);
	}

	public class SymbolEntry {
		String type;
		String name;
		boolean isGlobal;

		String label;

		public SymbolEntry(String name, String type, boolean isGlobal) {
			this.name = name;
			this.type = type;
			this.isGlobal = isGlobal;
		}
	}

	public class FunctionSymbolEntry extends SymbolEntry {
		String sig;
		boolean isMain;
		boolean isRuntime;

		public FunctionSymbolEntry(String name, String type, String sig, boolean isMain) {
			super(name, type, false);
			this.sig = sig;
			this.isMain = isMain;
			this.isRuntime = false;
		}

		public FunctionSymbolEntry(String name, String type, String sig, boolean isMain, boolean isRuntime) {
			super(name, type, false);
			this.sig = sig;
			this.isMain = isMain;
			this.isRuntime = isRuntime;
		}
	}

}
