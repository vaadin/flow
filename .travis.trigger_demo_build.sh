#!/usr/bin/env bash

if [ "$TRAVIS_PULL_REQUEST" == "false" ]
then
    # https://docs.travis-ci.com/user/triggering-builds

    body='{
        "request": {
        "message": "New flow snapshot is published, automatically triggered flow-demo snapshot build",
        "branch":"master"
    }}'

    curl -s -X POST \
       -H "Content-Type: application/json" \
       -H "Accept: application/json" \
       -H "Travis-API-Version: 3" \
       -H "Authorization: token $TRAVIS_API_TOKEN" \
       -d "$body" \
       https://api.travis-ci.com/repo/vaadin%2Fflow-demo/requests
fi
