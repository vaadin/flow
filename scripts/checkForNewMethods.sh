#!/bin/bash
mvn -f ../pom.xml revapi:report -ntp -Drevapi.failOnProblems=false -Drevapi.oldVersion=24.0.0  -pl '!com.vaadin:flow-gradle-plugin' -DskipTests
grep "java.method.addedToInterface" -B 1 ../report.txt | sed -n '1~3p' >> signaturesForAtSince.txt
grep "java.method.abstractMethodAdded" -B 1 ../report.txt | sed -n '1~3p' >> signaturesForAtSince.txt
grep "java.method.added" -B 1 ../report.txt | sed -n '1~3p' >> signaturesForAtSince.txt
# now iterate over file and execute rewrite recipe
cd ..
# mvn rewrite:run needs more memory
# but for some reason the recipe isn't applied yet -> check method signature

