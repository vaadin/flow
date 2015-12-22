#!/bin/bash

# Helper script for installing all Bower dependencies for a Maven project
# Goes through all defined Maven deps and scans any bower.json found in the root of a jar
# In the end, runs "bower install <all found dependencies>"

toinstall=""

echo "Finding dependency jars..."
jars=`mvn dependency:list -DoutputAbsoluteArtifactFilename=true|egrep ":.*:.*:.*:.*m2/"|cut -d: -f6`
echo "Finding bower.json files..."
for jar in $jars
do
	deps=`unzip -p $jar bower.json 2>/dev/null`
	if [ "$?" != "0" ]
	then
		# No bower.json
		continue
	fi

	for dep in `echo $deps|jq ".dependencies" |grep ":"|sed "s/ //g"|sed "s/,//g"`
	do
		name=`echo $dep|cut -d: -f 1|sed 's/"//g'`
		value=`echo $dep|cut -d: -f2-|sed 's/"//g'`
		if [[ $value == *"#"* ]]
		then
			# Value is of type PolymerElements/paper-slider#1.0.4 and we can install that
#			bower install --save $value
			toinstall="$toinstall $value"
		else
			# Dep is of type "vaadin-grid":"1.0.0-rc1", install using name and version
#			bower install --save $name"#"$value
			toinstall="$toinstall $name"#"$value"
		fi
	done
done
echo "Installing dependencies..."

mkdir -p src/main/generated-bower
pushd src/main/generated-bower
cat > bower.json << EOF
{
  "name": "Imported-Maven-Bower-dependencies", 
  "private": true,
  "ignore": [
    "**/.*",
    "node_modules",
    "bower_components",
    "test",
    "tests"
  ] 
}
EOF

# Must remove cache as Bower fetches versions from there also and generates conflicts for the fun of it
rm -rf bower_components
bower install --save $toinstall
popd
bower install --force --save ./src/main/generated-bower

