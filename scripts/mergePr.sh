#!/bin/bash

PR=$1
GITHUB_REPOSITORY=$(git config --get remote.origin.url|grep github.com:|sed "s/.*github.com://"|grep .git|sed "s/.git.*//")

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

if [ "$GITHUB_REPOSITORY" = "" ]
then
	echo "Unable to determine GitHub repository from the origin remote URL. Ensure you have cloned the project from GitHub"
	exit 3
fi

curl -s -H "Authorization: token $GITHUB_TOKEN" https://api.github.com/repos/$GITHUB_REPOSITORY/pulls/$PR > PR_DATA
target=$(cat PR_DATA|jq .base.ref|cut -d\" -f 2)
head=$(cat PR_DATA|jq .head.sha|cut -d\" -f 2)

BRANCH=pr-$PR
git branch -D $BRANCH
git fetch && git checkout origin/$target && git checkout -b $BRANCH
if [ "$?" != "0" ]
then
	echo "Failed to create branch $BRANCH"
	exit 5
fi

commits=$(git log origin/$target..$head --reverse --oneline|cut -d' ' -f 1)
prtitle=$(cat PR_DATA|jq .title|sed "s/^.//"|sed "s/.$//")

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
			exit 4
		fi
	else
		echo "Skipping merge commit $(git show -s --oneline $commit)"
	fi
done

echo >> COMMIT_MSG
echo "Closes #$PR" >> COMMIT_MSG

git commit --author "$AUTHOR"  -F COMMIT_MSG  --edit

echo
echo "If everything is ok, do"
echo
echo "git push origin $BRANCH:$target"

rm -f COMMIT_MSG PR_DATA
