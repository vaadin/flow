var tape = require('tape')
var mutexify = require('./')
var mutexifyPromise = require('./promise')

tape('locks', function (t) {
  t.plan(21)

  var lock = mutexify()
  var used = false
  t.ok(!lock.locked, 'not locked')

  for (var i = 0; i < 10; i++) {
    lock(function (release) {
      t.ok(!used, 'one at the time')
      t.ok(lock.locked, 'locked')
      used = true
      setImmediate(function () {
        used = false
        release()
      })
    })
  }
})

tape('calls callback', function (t) {
  var lock = mutexify()

  var cb = function (err, value) {
    t.same(err, null)
    t.same(value, 'hello world')
    t.end()
  }

  lock(function (release) {
    release(cb, null, 'hello world')
  })
})

tape('calls the locking callbacks in a different stack', function (t) {
  t.plan(2)

  var lock = mutexify()

  var topScopeFinished = false
  var secondScopeFinished = false

  lock(function (release) {
    t.ok(topScopeFinished, 'the test function has already finished running')
    release()
    secondScopeFinished = true
  })

  lock(function (release) {
    t.ok(secondScopeFinished, 'the last lock\'s call stack is done')
    release()
    t.end()
  })

  topScopeFinished = true
})

tape('locks with promises', async function (t) {
  t.plan(21)

  var lock = mutexifyPromise()
  var used = false
  t.ok(!lock.locked, 'not locked')
  for (var i = 0; i < 10; i++) {
    var release = await lock()
    t.ok(!used, 'one at the time')
    t.ok(lock.locked, 'locked')
    used = true
    setImmediate(function () {
      used = false
      release()
    })
  }
})
