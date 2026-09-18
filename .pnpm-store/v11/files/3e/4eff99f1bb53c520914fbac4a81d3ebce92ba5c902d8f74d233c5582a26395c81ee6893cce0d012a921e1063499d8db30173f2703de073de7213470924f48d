"use strict";

const processOptions = require("../lib/process_options.js");

describe("processOptions", function () {
  var prevUser, prevKey;

  beforeAll(function () {
    prevUser = process.env.SAUCE_USERNAME;
    prevKey = process.env.SAUCE_ACCESS_KEY;
    delete process.env.SAUCE_USERNAME;
    delete process.env.SAUCE_ACCESS_KEY;
  });

  afterAll(function () {
    process.env.SAUCE_USERNAME = prevUser;
    process.env.SAUCE_ACCESS_KEY = prevKey;
  });

  it("should process user and password",  () =>  {
    var result = processOptions({username: "bermi", accessKey: "1234"});
    expect(Array.isArray(result)).toBe(true);
    expect(result).toEqual([
      "-u", "bermi",
      "-k", "1234"
    ]);
  });

  it("should process user and password as env vars",  () =>{
    process.env.SAUCE_USERNAME = "bermi";
    process.env.SAUCE_ACCESS_KEY = "1234";
    var result = processOptions({});
    expect(Array.isArray(result)).toBe(true);
    expect(result).toEqual([
      "-u", "bermi",
      "-k", "1234"
    ]);
    delete process.env.SAUCE_USERNAME;
    delete process.env.SAUCE_ACCESS_KEY;
  });

  it("should handle proxy ports", () => {
    expect(processOptions({port: 1234})).toEqual(["-P", 1234]);
  });

  it("should handle boolean flags",  () =>  {
    var result = processOptions({doctor: true});
    expect(result).toEqual(["--doctor"]);
  });

  it("should handle array values",  () => {
    var result = processOptions({directDomains: ["google.com", "asdf.com"]});
    expect(result).toEqual(["--direct-domains", "google.com,asdf.com"]);
  });

  it("should handle future options",  () => {
    var result = processOptions({
      someFutureOption: "asdf",
      futureBoolean: true
    });
    expect(result).toEqual(["--some-future-option", "asdf", "--future-boolean"]);
  });

  it("should omit special launcher flags",  () => {
    expect(processOptions({
      readyFileId: "1",
      verbose: true,
      logger: function () {},
      connectRetries: 1,
      connectRetryTimeout: 5000,
      downloadRetries: 1,
      downloadRetryTimeout: 1000,
      detached: true,
      connectVersion: "1.2.3",
      exe: "/opt/sc-4.3.12-linux/bin/sc"
    })).toEqual([]);
  });

  it("should handle 'vv' flag", () => {
    var result = processOptions({vv: true});
    expect(result).toEqual(["-vv"]);
  });

  it("should handle single-letter flag without changing to kebab-case", () => {
    var result = processOptions({
      B: "all",
      "-N": true
    });
    expect(result).toEqual(["-B", "all", "-N"]);
  });

  it("should pass flags starting with dash with no changes", () => {
    var result = processOptions({
      "-SomeOptionWithDash": true,
      "--SomeOptionWithTwoDashes": "foo"
    });
    expect(result).toEqual(["-SomeOptionWithDash", "--SomeOptionWithTwoDashes", "foo"]);
  });

});
