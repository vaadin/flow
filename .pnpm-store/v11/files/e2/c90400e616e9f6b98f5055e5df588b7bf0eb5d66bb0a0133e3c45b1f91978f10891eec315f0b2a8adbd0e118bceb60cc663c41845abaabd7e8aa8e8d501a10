var findPort = require('../lib/findPort.js')
var http = require('http')
var assert = require('assert')

describe('findPort', function () {

	var server;

	beforeEach(function (done) {
		server = http.createServer(function (request, response) {
			response.end()
		})

		server.on('listening', done)

		server.listen(9000)
	})

	afterEach(function() {
		server.close()
	})

	it('throws an error if callback is missing with one argument', function () {
		assert.throws(function () {
			findPort(9000)
		})
	})

	it('throws an error if callback is missing with two arguments', function () {
		assert.throws(function () {
			findPort(9000, 9002)
		})
	})

	it('finds unused ports in a range', function (done) {

		findPort(9000, 9003, function(ports) {
			assert.deepEqual(ports, [9001, 9002, 9003])
			done()
		})
	})

	it('finds unused ports in an array', function (done) {

		findPort([9000, 9003], function(ports) {
			assert.deepEqual(ports, [9003])
			done()
		})
	})
})