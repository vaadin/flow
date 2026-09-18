var transform = require('../')

var result = transform(`
var multiply = (a, b) => {
  return a * b
}
var add = (a, b) => a + b
`, function (node) {
  if (node.type === 'ArrowFunctionExpression') {
    var params = node.params.map(function (param) { return param.getSource() })
    if (node.body.type !== 'BlockStatement') {
      node.body.edit.update(`{ return ${node.body.getSource()} }`)
    }
    node.edit.update(`function (${params.join(', ')}) ${node.body.getSource()}`)
  }
})

console.log(
  result.toString() +
  '\n//# sourceMappingURL=' +
  result.generateMap({ hires: true }).toUrl()
)
