var net = require('net')
var async = require('async')
var util = require('util')

module.exports = function(start, endOrCallback, callback) {

	if (arguments.length < 2)
		throw new Error('missing callback')

	if (arguments.length === 2 && typeof endOrCallback !== 'function')
		throw new Error('missing callback')

	if (arguments.length >= 3 && typeof callback !== 'function')
		throw new Error('missing callback')

	if (typeof endOrCallback === 'function') {
		callback = endOrCallback
		endOrCallback = 0
	}

	var ports

	if (util.isArray(start)) {
		ports = start
	} else {

		ports = []

		for (var i = start; i <= endOrCallback; i++)
			ports.push(i)
	}

	async.filter(ports, probe, callback)
}

function probe(port, callback) {

	var server = net.createServer().listen(port)

	var calledOnce = false

	var timeoutRef = setTimeout(function () {
		calledOnce = true
		callback(false)
	}, 2000)

	timeoutRef.unref()

	var connected = false

	server.on('listening', function() {
		clearTimeout(timeoutRef)

		if (server)
			server.close()

		if (!calledOnce) {
			calledOnce = true
			callback(true)
		}
	})

	server.on('error', function(err) {
		clearTimeout(timeoutRef)

		var result = true
		if (err.code === 'EADDRINUSE')
			result = false

		if (!calledOnce) {
			calledOnce = true
			callback(result)
		}
	})
}



