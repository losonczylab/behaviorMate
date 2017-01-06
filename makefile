
all:
	echo "{" >version.json
	{ echo "\t \"version\": \""; git tag | tail -n 1 | cat; echo "\","; } | tr -d "\n" >>version.json
	echo  >>version.json
	{ echo "\t \"git_revision\": \""; git rev-parse HEAD | cat; echo "\""; } | tr -d "\n" >>version.json
	echo "\n}" >>version.json
	javac -cp lib/core.jar:lib/java-json.jar -d . src/*.java
	jar cfmv BehaviorMate.jar Manifest.txt *.class

settings:
	javac -cp lib/core.jar:lib/java-json.jar -d . src/SettingsLoader.java
	jar cfmv Settings.jar SettingsManifest.txt *.class

form:
	javac -cp lib/core.jar:lib/java-json.jar -d . src/SettingsCreator.java
	jar cfmv SettingsForm.jar FormManifest.txt *.class

attrs:
	javac -cp lib/core.jar:lib/java-json.jar -d . src/TrialAttrsForm.java
	jar cfmv TrialAttrsForm.jar FormManifest.txt *.class

clean:
	rm BehaviorMate.jar *.class
