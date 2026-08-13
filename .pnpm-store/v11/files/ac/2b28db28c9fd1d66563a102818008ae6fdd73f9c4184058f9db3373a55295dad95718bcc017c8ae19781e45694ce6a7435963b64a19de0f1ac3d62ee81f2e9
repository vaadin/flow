var assert = require('assert')
var babylon = require('babylon')
var horchata = require('horchata')
var transform = require('../')

assert.equal(transform(`
  var el = <div />;
`, { parser: babylon, plugins: [ 'jsx' ] }, function (node) {
  if (node.type === 'JSXElement') {
    node.edit.update(JSON.stringify(node.getSource()))
  }
}).toString(), `
  var el = "<div />";
`)

assert.equal(transform(`
X = () -> {
  @prop or= 'value'
}
new X
`, { parser: horchata }, function (node) {
  switch (node.type) {
  case 'FunctionExpression':
    node.edit.update('function () ' + node.body.getSource())
  }
}).toString(), `
X = function () {
  @prop or= 'value'
}
new X
`)
