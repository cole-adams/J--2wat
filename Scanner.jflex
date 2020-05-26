/**
* Cole Adams
* CPSC 411
* Milestone 1
* February 7th, 2020
*/
import java.io.FileInputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.lang.StringBuffer;
import java_cup.runtime.*;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.Location;
%%

%type Symbol
%class Lexer
%line
%cup

%init{
	yyline = 1;
	yycolumn = 1;
%init}

%{
	//Buffer for generating strings
	StringBuffer str = new StringBuffer();
	ComplexSymbolFactory sf = new ComplexSymbolFactory();

	//Counting warnings, to output error if too many
	int lastWarningLine = 0;
	int warningCount = 0;

	/**
	* Helper function for generating token without attributes
	*/
	private Symbol token(TokenType type) {
		return new Symbol(type.getID(), yyline, yycolumn);
	}

	/**
	* Helper function for generating token with attributes
	*/
	private Symbol token(TokenType type, String attr) {
		return new Symbol(type.getID(), yyline, yycolumn, attr);
	}

	/**
	* Outputs error to stderr and quits
	*/
	private void error(String s) {
		System.err.println("error: " + s + " at or near line " + yyline);
		System.exit(-1);
	}

	/**
	* Outputs warning to stderr. Calls error if too many warnings have been issued
	*/
	private void warning(String s) {
		if (yyline != lastWarningLine) {
			lastWarningLine = yyline;
			warningCount = 1;
		} else {
			warningCount++;
		}

		System.err.println("warning: " + s + " around line " + yyline);
		//Checking how many warnings there have been this line
		if (warningCount > 12) {
			error("too many warnings");
		}
	}
%}

alpha = [a-zA-Z]
alphanum = [a-zA-Z0-9]
num = [0-9]+
ID = ({alpha}|_)({alphanum}|_)*
comment = "//"[^\n\r]*

%state STRING

%%

<YYINITIAL> {
	//Comments
	{comment} 		{}

	//Whitespace
	[ \t\r]+			{}
	\n						{}

	//String literals
	\"						{str.setLength(0); yybegin(STRING);}

	//Integers
	{num} 				{return token(TokenType.NUM, yytext());}

	//Reserved words
	"true"				{return token(TokenType.TRUE);}
	"false"				{return token(TokenType.FALSE);}
	"boolean"			{return token(TokenType.BOOL);}
	"int"					{return token(TokenType.INT);}
	"void"				{return token(TokenType.VOID);}
	"if"					{return token(TokenType.IF);}
	"else"				{return token(TokenType.ELSE);}
	"while"				{return token(TokenType.WHILE);}
	"break"				{return token(TokenType.BREAK);}
	"return"			{return token(TokenType.RETURN);}

	//Operators
	"+"						{return token(TokenType.ADD);}
	"-"						{return token(TokenType.SUB);}
	"*"						{return token(TokenType.MULT);}
	"/"						{return token(TokenType.DIV);}
	"%"						{return token(TokenType.MOD);}
	"<"						{return token(TokenType.LT);}
	">"						{return token(TokenType.GT);}
	"<="					{return token(TokenType.LE);}
	">="					{return token(TokenType.GE);}
	"="						{return token(TokenType.ASSIGN);}
	"=="					{return token(TokenType.EQ);}
	"!="					{return token(TokenType.NE);}
	"!"						{return token(TokenType.NOT);}
	"&&"					{return token(TokenType.AND);}
	"||"					{return token(TokenType.OR);}

	//Other
	"("						{return token(TokenType.OPENP);}
	")"						{return token(TokenType.CLOSEP);}
	"{"						{return token(TokenType.OPENB);}
	"}"						{return token(TokenType.CLOSEB);}
	";"						{return token(TokenType.SEMICOLON);}
	","						{return token(TokenType.COMMA);}

	//Identifiers
	{ID}					{return token(TokenType.ID, yytext());}

	.							{warning("ignoring bad character '" + yytext() + "'");}
}
<STRING> {
	\"						{yybegin(YYINITIAL);
									return token(TokenType.STR, str.toString());}
	\\b						{str.append("\\08");}
	\\f						{str.append("\\0C");}
	\\t						{str.append("\\t");}
	\\r						{str.append("\\r");}
	\\n						{str.append("\\n");}
	\\'						{str.append("\\'");}
	\\\"					{str.append("\\\"");}
	\\\\					{str.append("\\\\");}
	\0						{}
	[^\n\r\"\\\0]+	{str.append(yytext());}

	//Case were the next line has a quote, but the current doesn't
	[\n\r]{1,2}[^]*\" 	{error("newline in string literal");}
	[\n\r] 							{error("string missing closing quote");}
}

<STRING><<EOF>> {
	//For the case that a string missing a quote is at EOF
	error("string missing closing quote");
}

<<EOF>>		{return token(TokenType.EOF); }
