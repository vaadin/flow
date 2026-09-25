var test = require('tape')
var parse = require('acorn').parse
var transformAst = require('../')

test('transform parsed ast', function (t) {
  var src = `
    function a (b) {
      return b + b
    }
    a(10)
  `

  var ast = parse(src)
  var result = transformAst(src, {
    ast: ast,
    parser: { parse: t.fail }
  }, function (node) {
    if (node.type === 'Identifier') {
      node.edit.update(node.name.toUpperCase())
    }
  }).toString()

  t.equal(result, `
    function A (B) {
      return B + B
    }
    A(10)
  `)
  t.end()
})
