
all:
	javac -cp lib/core.jar -d . src/*.java
	jar cfmv BehaviorMate.jar Manifest.txt bin/*.class

clean:
	rm BehaviorMate.jar *.class
