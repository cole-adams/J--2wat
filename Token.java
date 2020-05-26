import java_cup.runtime.*;

public class Token {
	TokenType type;
	String attr;
	int lineno;
	Symbol sym;

	Token(TokenType type, String attr, int lineno, Symbol sym) {
		this.type = type;
		this.attr = attr;
		this.lineno = lineno;
		this.sym = sym;
	}

	public String toString() {
		if (this.attr.length() > 0) {
			return "Token(" + this.type + ", " + this.lineno + ", " + this.attr +")";
		} else {
			return "Token(" + this.type + ", " + this.lineno + ", None)";
		}
	}

	public Symbol getSym() {
		return sym;
	}
}
