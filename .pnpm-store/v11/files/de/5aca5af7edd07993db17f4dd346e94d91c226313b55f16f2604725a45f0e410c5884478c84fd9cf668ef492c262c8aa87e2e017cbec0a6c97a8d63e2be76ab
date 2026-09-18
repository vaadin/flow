var test = require('tape')
var transformAst = require('../')

test('walk() method', function testfn (t) {
  t.plan(1)

  var count = 0
  var result = transformAst(testfn.toString(), function (node) {
    count++
    node.walked = true
  })

  var check = 0
  result.walk(function (node) {
    check += node.walked ? 1 : 0
  })

  t.is(count, check)

  t.end()
})

test('no callback on parse', function (t) {
  t.plan(1)

  var result = transformAst(`
    module.exports = function a () { return 10 }
  `)

  result.walk(function (node) {
    if (node.type === 'Program') {
      node.edit.prepend('(function (module, exports) {')
      node.edit.append('})(xyz, xyz.exports)')
    }
  })

  t.is(result.toString(), `(function (module, exports) {
    module.exports = function a () { return 10 }
  })(xyz, xyz.exports)`)
  t.end()
})
