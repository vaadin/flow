"use strict";

var https = require("https");
const SAUCE_API_HOST = process.env["SAUCE_API_HOST"] || "saucelabs.com";

function sauceApiRequest(path, cb) {
  https.request({
    method: "GET",
    host: SAUCE_API_HOST,
    port: 443,
    auth: process.env.SAUCE_USERNAME + ":" + process.env.SAUCE_ACCESS_KEY,
    path: "/rest/v1/" + path
  }).on("response", function (res) {
    var body = "";
    res.on("data", function (chunk) {
      body += chunk;
    });
    res.on("end", function () {
      cb(null, res, JSON.parse(body));
    });
  }).on("error", cb).end();
}

function getTunnel(tunnelId, cb) {
  sauceApiRequest(process.env.SAUCE_USERNAME + "/tunnels/" + tunnelId, cb);
}


function getTunnels(cb) {
  sauceApiRequest(process.env.SAUCE_USERNAME + "/tunnels", cb);
}

module.exports = {
  getTunnel: getTunnel,
  getTunnels: getTunnels
};