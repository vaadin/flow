#!/usr/bin/env node

var bench = require('./')
var path = require('path')

global.__NANOBENCH__ = require.resolve('./')

for (var i = 2; i < process.argv.length; i++) require(path.join(process.cwd(), process.argv[i]))
