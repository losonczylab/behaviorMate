
all:
	printf "{\n" >version.json
	printf "\t \"version\": \"" >> version.json
	git tag | tail -n 1 | cat | tr -d "\n" >>version.json
	printf "\",\n" >>version.json
	printf "\t \"git_revision\": \"" >>version.json
	git rev-parse HEAD | cat | tr -d "\n" >>version.json
	printf "\"\n}" >>version.json

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

cleanbuild:
	rm -f *.class
	
clean: cleanbuild
	rm version.json BehaviorMate.jar
