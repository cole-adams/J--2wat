JFLEX=jflex

CUP_CP=./java-cup-11b.jar

ifeq ($(OS),Windows_NT)
	CUP_RUNTIME_CP=".;./java-cup-11b-runtime.jar"
else
	CUP-CUP_RUNTIME_CP=".:./java-cup-11b-runtime.jar"
endif

CUP=java -jar ${CUP_CP}

JAVA=java -cp ${CUP_RUNTIME_CP}
JAVAC=javac

all: Lexer.java parser.java
	${JAVAC} -cp ${CUP_RUNTIME_CP} *.java

Lexer.java:
	${JFLEX} Scanner.jflex

parser.java:
	${CUP} parser.cup

run:
ifdef file
	${JAVA} Main ${file}
else
	${JAVA} Main
endif

clean:
	rm -f *.class
	rm -f parser.java
	rm -f Lexer.java
	rm -f sym.java
