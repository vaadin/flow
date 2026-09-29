var bench = require('nanobench')
var transformAst = require('../')
var src = require('fs').readFileSync(require.resolve('acorn'), 'utf8')

bench('parse a big file × 5', function (b) {
  b.start()
  for (var i = 0; i < 5; i++)
    transformAst(src).toString()
  b.end()
})

bench('dense source map for a big file × 5', function (b) {
  b.start()
  for (var i = 0; i < 5; i++)
    transformAst(src, function (node) {
      if (node.type === 'Identifier') node.edit.update(node.edit.source().toUpperCase())
    }).toString({ map: true })
  b.end()
})
