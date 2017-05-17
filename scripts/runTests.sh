#!/bin/sh

function usage
{
    echo "usage: $0 -branch branch_name -token api_token [-message commit_message] [-cfile certificate_file] [-javadoc] [-full]"
    echo "See README.md for parameter details"
}

while [ "$1" != "" ]; do
    case $1 in
         -branch )       shift
                         branch=$1
                         ;;
         -cfile )        shift
                         certificate=$1
                         ;;
         -message )      shift
                         message=$1
                         ;;
         -token )        shift
                         token=$1
                         ;;
         -javadoc )      shift
                         javadoc=1
                         ;;
         -all-tests )    shift
                         full=1
                         ;;
         -sonar )        shift
                         sonar=1
                         ;;
         -sonaronly )    shift
                         sonaronly=1
                         ;;
         * )             usage
                         exit 1
    esac
    shift
done

if [ -z $branch ]; then
   usage
   exit 1
fi

if [ -z $token ]; then
   usage
   exit 1
fi


if [ -z $certificate ]; then
   cert_option=" -k "
else

   cert_option=" --cacert $certificate "
fi

script="./.travis.validation.sh"

if [ "$javadoc" = "1" ]; then
   script=$script" -javadoc"
fi

if [ "$full" = "1" ]; then
   script=$script" -all-tests"
fi

if [ "$sonar" = "1" ]; then
   script=$script" -sonar"
fi

if [ "$sonaronly" = "1" ]; then
   script=$script" -sonaronly"
fi

if [ -z $message ]; then
body='{
"request": {
"config": {
    "script": '"\"$script\""'
},
"branch": '"\"$branch\"}}"
else
body='{
"request": {
"config": {
    "script": '"\"$script\""'
},
"message": '"\"$message\",
\"branch\": \"$branch\"}}"
fi

curl $cert_option -X POST \
   -H "Content-Type: application/json" \
   -H "Accept: application/json" \
   -H "Travis-API-Version: 3" \
   -H "Authorization: token $token" \
   -d "$body" \
   https://api.travis-ci.com/repo/vaadin%2Fflow/requests

