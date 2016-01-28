#!/bin/bash

PR=$1

if [ "$PR" = "" ]
then
	echo "Usage: $0 <PR number>"
	exit 1
fi

if [ "$GITHUB_TOKEN" = "" ]
then
	echo "You must define a GitHub auth key using GITHUB_TOKEN=..."
	exit 2
fi


BRANCH=pr-$PR
git branch -D $BRANCH
git fetch && git checkout origin/master && git checkout -b $BRANCH
if [ "$?" != "0" ]
then
	echo "Failed to create branch $BRANCH"
	exit 5
fi

commits=$(curl -s -H "Authorization: token $GITHUB_TOKEN" https://api.github.com/repos/vaadin/hummingbird/pulls/$PR/commits |jq .[].sha|cut -d\" -f 2)
prtitle=$(curl -s -H "Authorization: token $GITHUB_TOKEN" https://api.github.com/repos/vaadin/hummingbird/pulls/$PR|jq .title|sed "s/^.//"|sed "s/.$//")

echo "$prtitle" > COMMIT_MSG
echo >> COMMIT_MSG
AUTHOR=
for commit in $commits
do
	if [ "$AUTHOR" = "" ]
	then
		AUTHOR=$(git show -s $commit --pretty='%an <%ae>')
	fi
	git show --summary $commit|grep "^Merge: " > /dev/null
	merge=$?
	if [ "$merge" == 1 ]
	then
		# skip merges
		echo "Picking $(git show -s --oneline $commit)"
		echo "* $(git show -s $commit --pretty='%B')" >> COMMIT_MSG

		git cherry-pick -n $commit
		if [ "$?" != "0" ]
		then
			echo "Cherry-pick of $commit failed, aborting"
			exit 3
		fi
	else
		echo "Skipping merge commit $(git show -s --oneline $commit)"
	fi
done

echo >> COMMIT_MSG
echo "Closes #$PR" >> COMMIT_MSG

git commit --author "$AUTHOR"  -F COMMIT_MSG  --edit

echo "If everything is ok, do"
echo "git push origin head:master"

rm -f COMMIT_MSG
