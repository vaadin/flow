var bench = require('./')

bench('sha1 200.000 times', function (b) {
  var crypto = require('crypto')
  var data = new Buffer('hello world')

  b.start()

  for (var i = 0; i < 200000; i++) {
    data = crypto.createHash('sha1').update(data).digest()
  }

  b.end()
})

bench('sha256 200.000 times', function (b) {
  var crypto = require('crypto')
  var data = new Buffer('hello world')

  b.start()

  for (var i = 0; i < 200000; i++) {
    data = crypto.createHash('sha256').update(data).digest()
  }

  b.end()
})
