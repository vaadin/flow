#!/usr/bin/env node

/**
 * This script is used for autoMerging PR for flow repo.
 * To run this script:  
 * 1.collect PR branch info from the flow PR validation build in TC
 * 2.Check the criteria for auto-merging : 
 *     - created by Vaadin-bot
 *     - has +0.0.1 label
 *     - PR is in the `open` state
 * 3.auto review the PR
 * 4.auto merge the PR
 *
 * RUN: ./script/autoMerge.js {branch}
 */

const axios = require('axios');

const repo = "flow";
const token = process.env['GITHUB_TOKEN'];
const reviewToken = process.env['GITHUB_REVIEW_TOKEN'];

if (!token) {
  console.log(`GITHUB_TOKEN is not set, skipping PR auto-merging`);
  process.exit(1);
}

if (!reviewToken) {
  console.log(`GITHUB_REVIEW_TOKEN is not set, skipping PR auto-merging`);
}

const branch = process.argv[2];
const url = `https://api.github.com/repos/vaadin/${repo}/pulls/${branch}`;

async function collectPullInfo(){

  try {
    const options = {
      headers:
      {
        'User-Agent': 'Vaadin Auto Merge',
        'Authorization': `token ${token}`,
        'Content-Type': 'application/json',
      }
    };
    
    res = await axios.get(url, options);
    data = res.data;
    
    if (data.length === 0) {
      console.log("No commits needs to be picked.");
      process.exit(0);
    }
    return data;
  } catch (error) {
    console.error(`Cannot get the info for pull request ${branch}. ${error}`);
    process.exit(1);
  }
}

function checkAutoMergeCondition(info){
  let apiDiff = false;
  let condition = false;
  
  if (info.state != "open"){
    console.log(`PR ${branch} is not in "open" state, skip auto-merging.`);
    return false;
  }
  
  if (info.user.login != "vaadin-bot"){
    console.log(`PR ${branch} is not created by vaadin-bot, skip auto-merging.`);
    return false;
  }
  
  for (let label of info.labels){
    if(label.name === "+0.0.1"){
      apiDiff = true;
    }
  }
  
  if(apiDiff){
    console.log(`PR ${branch} is eligible for auto merging`);
    return true;
  } else {
    console.log(`PR ${branch} doesn't have the "+0.0.1" label, skip auto-merging.`);
    return false;
  }
}

async function autoReviewPR(){
  let reviewURL = url+'/reviews';
  
  try {
    const options = {
      headers:
      {
        'User-Agent': 'Vaadin Auto Merge',
        'Authorization': `token ${reviewToken}`,
        'Content-Type': 'application/json',
      }
    };
    
    await axios.post(reviewURL, {'event':'APPROVE'}, options);
    await postComment(url, "This PR is eligible for auto-merging policy, so it has been approved automatically.[Message is sent from bot]");
    console.log(`PR ${branch} has been reviewed automatically.`);
    
  } catch (error) {
    console.error(`Cannot review pull request ${branch}. ${error}`);
    process.exit(1);
  }
}

async function mergePR(){
  let mergeURL = url+'/merge';
  
  try{
    const options = {
      headers:
      {
        'User-Agent': 'Vaadin Auto Merge',
        'Authorization': `token ${token}`,
        'Content-Type': 'application/json',
      }
    };
    
    await axios.put(mergeURL, {'merge_method':'squash'}, options);
    await postComment(url, "This PR has been merged with 'squash' method.[Message is sent from bot]");
    console.log(`PR ${branch} has been merged automatically.`);

  } catch (error) {
    console.error(`Cannot merge pull request ${branch}. ${error}`);
    process.exit(1);
  }
}

async function postComment(url, message){
  let issueURL = url.replace("pulls", "issues") + "/comments";
  const options = {
    headers:{
      'User-Agent': 'Vaadin Auto-merging',
      'Authorization': `token ${token}`,
    }
  };

  await axios.post(issueURL, {"body":message}, options);
}

async function main(){
  let PRInfo = await collectPullInfo();
  
  if(checkAutoMergeCondition(PRInfo)){
    await autoReviewPR();
    await mergePR();
  }
}

main();
