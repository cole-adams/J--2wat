public enum TokenType {
	EOF ("EOF", sym.EOF),
	//Identifiers
	ID ("id", sym.ID),

	//String literal
	STR ("string", sym.STR),

	//Number
	NUM ("number", sym.NUM),

	//Reserved words
	TRUE ("true", sym.TRUE),
	FALSE ("false", sym.FALSE),
	BOOL ("boolean", sym.BOOL),
	INT ("int", sym.INT),
	VOID ("void", sym.VOID),
	IF ("if", sym.IF),
	ELSE ("else", sym.ELSE),
	WHILE ("while", sym.WHILE),
	BREAK ("break", sym.BREAK),
	RETURN ("return", sym.RETURN),

	//Operators
	ADD ("+", sym.ADD),
	SUB ("-", sym.SUB),
	MULT ("*", sym.MULT),
	DIV ("/", sym.DIV),
	MOD ("%", sym.MOD),
	LT ("<", sym.LT),
	GT (">", sym.GT),
	LE ("<=", sym.LE),
	GE (">=", sym.GE),
	ASSIGN ("=", sym.ASSIGN),
	EQ ("==", sym.EQ),
	NE ("!=", sym.NE),
	NOT ("!", sym.NOT),
	AND ("&&", sym.AND),
	OR ("||", sym.OR),

	//Other
	OPENP ("(", sym.OPENP),
	CLOSEP (")", sym.CLOSEP),
	OPENB ("{", sym.OPENB),
	CLOSEB ("}", sym.CLOSEB),
	SEMICOLON (";", sym.SEMICOLON),
	COMMA (",", sym.COMMA);

	private final String display;
	private final int id;

	private TokenType(String display, int id) {
		this.display = display;
		this.id = id;
	}

	public String toString() {
		return this.display;
	}

	public int getID() {
		return id;
	}
}
