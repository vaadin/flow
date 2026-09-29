# transform-ast

Transform an AST with source maps.
Basically @substack's [falafel](https://github.com/substack/node-falafel), but based on [magic-string][].

## Example

```js
var result = require('transform-ast')(`
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
result.toString() === `
  var multiply = function (a, b) {
    return a * b
  }
  var add = function (a, b) { return a + b }
`
fs.writeFile('output.js.map', JSON.stringify(result.map))
```

## Install

```bash
npm install --save transform-ast
```

## API

### `magicString = transformAst(source, opts = {}, fn = function () {})`

Parse and transform a `source` string.
`fn` will be called on each node.
The returned `magicString` is a [magic-string][] instance, with a `toString()` method to get the transformed string and a `.map` property to access the source map.

`opts.parser` sets the parser module to use. This should be an object with a `.parse(src, opts)` function. The default is [`require('acorn-node')`](https://github.com/browserify/acorn-node).

If you already have an AST, pass it in `opts.ast`. This will skip the parse step inside `transformAst()`.

```js
transformAst(source, { ast: parsedSource }, cb)
```

### `magicString.walk(fn)`

Walk the AST again.
`fn` will be called on each node.

### `magicString.map`

Generate and return a source map.
If the input `source` had an inline source map comment, this will be taken into account, and the final source map will point back to the original string.
The source map for _only_ the changes made by transform-ast can be accessed by using [magic-string][]'s `generateMap()` method.

### nodes

In addition to the usual AST node properties, each node object also has some additional methods.
Unlike falafel, these methods live on the `.edit` property, to prevent name conflicts (such as the `update()` method and the `.update` property of a ForStatement).
They're still also defined on the `node`s themselves, but only if there is no naming conflict.
It's better to use the `.edit` property.

### `node.getSource()`, `node.edit.source()`

Get the source string for a node, including transformations.

### `node.edit.update(string)`

Replace `node` with the given string.

### `node.edit.append(string)`

Append the source `string` after this node.

### `node.edit.prepend(string)`

Prepend the source `string` before this node.

## Custom Parser

You can pass in a custom parser using the `parser` option.
The parser should be an object with a `parse` function that takes a string and returns an AST.
Each AST node should have `.start` and `.end` properties indicating their position in the source string.

For example, parsing JSX using [babylon](https://github.com/babel/babylon):

```js
var babylon = require('babylon')
var transform = require('transform-ast')
var assert = require('assert')

assert.equal(transform(`
  var el = <div />;
`, { parser: babylon, plugins: [ 'jsx' ] }, function (node) {
  if (node.type === 'JSXElement') {
    node.edit.update(JSON.stringify(node.source()))
  }
}).toString(), `
  var el = "<div />";
`)
```

But parsers for other languages too, like [tacoscript](https://tacoscript.github.io)'s parser module [horchata](https://github.com/forivall/tacoscript/tree/master/packages/horchata):

```js
var horchata = require('horchata')
var transform = require('transform-ast')
var assert = require('assert')

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
```

## License

[MIT](./LICENSE)

[magic-string]: https://github.com/rich-harris/magic-string
