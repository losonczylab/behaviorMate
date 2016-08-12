
all:
	javac -cp core.jar:controlP5.jar  *.java
	jar cfmv ProcessingTest.jar Manifest.txt *.class
