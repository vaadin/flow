# mutexify

Bike shed mutex lock implementation in node.js

```
npm install mutexify
```

[![build status](http://img.shields.io/travis/mafintosh/mutexify.svg?style=flat)](http://travis-ci.org/mafintosh/mutexify)

Hasn't this been done before? Yes, but the specific semantics of this made some of my code simpler.

## Usage


``` js
var mutexify = require('mutexify')
var lock = mutexify()

lock(function(release) {
  console.log('i am now locked')
  setTimeout(function() {
    release()
  }, 1000)
})

lock(function(release) {
  console.log('1 second later')
  release()
})
```

A common pattern is to call a callback after you release the lock.
To do this in a one-liner pass the callback and the value to `release(cb, err, value)`

``` js
var write = function(data, cb) {
  lock(function(release) {
    fs.writeFile('locked-file.txt', data, release.bind(null, cb))
  })
}
```

`mutexify` guarantees that the order that a mutex was requested in is the order that access will be given.

You can read the lock's current state on the `lock.locked` property.

### Usage with promises

`mutexify` provides a Promise-based alternative.

```js
const mutexify = require('mutexify/promise')

;(async () => {
  var lock = mutexify()

  var release = await lock()
  console.log('i am now locked')
  setTimeout(function () {
    release()
  }, 1000)

  release = await lock()
  console.log('1 second later')
  release()
})()
```

## License

MIT
