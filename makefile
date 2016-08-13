
all:
	javac -cp lib/core.jar:lib/java-json.jar -d . src/*.java
	jar cfmv BehaviorMate.jar Manifest.txt *.class

settings:
	javac -cp lib/core.jar:lib/java-json.jar -d . src/*.java
	jar cfmv Settings.jar SettingsManifest.txt *.class

clean:
	rm BehaviorMate.jar *.class
