var test = require('tape')
var transform = require('../')

test('getSource()', function (t) {
  t.plan(2)
  var source = `
    import something from 'somewhere'
    
    something.whatever()
  `

  transform(source, { sourceType: 'module' }, function (node) {
    if (node.type === 'ImportDeclaration') {
      t.is(node.getSource(), "import something from 'somewhere'")
      t.is(node.source.value, 'somewhere')
    }
  })

  t.end()
})

test('keys that are both functions and objects', function (t) {
  t.plan(3)

  var source = `
    for (;; xyz) {} // update.name === "xyz"
    for (;; xyz(1, 2)) {} // update.arguments === [1, 2]
  `

  t.doesNotThrow(function () {
    transform(source, function (node) {
      if (node.type === 'ForStatement' && node.update.type === 'Identifier') {
        t.is(node.update.name, 'xyz')
      }
      if (node.type === 'ForStatement' && node.update.type === 'CallExpression') {
        t.is(node.update.arguments.length, 2)
      }
    })
  })

  t.end()
})
