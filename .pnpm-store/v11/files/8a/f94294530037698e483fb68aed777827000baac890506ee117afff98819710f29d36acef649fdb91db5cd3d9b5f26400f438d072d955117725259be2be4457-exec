#!/usr/bin/env node

var fs = require('fs')
var parse = require('./parse')
var prettyHrtime = require('pretty-hrtime')
var chalk = require('chalk')

var grace = 0.05 // 5% grace
var a = parse(fs.readFileSync(process.argv[2]))
var b = parse(fs.readFileSync(process.argv[3]))

var upper = 1 + grace
var lower = 1 - grace

var mapA = toMap(a)
var mapB = toMap(b)
var list = []

a.benchmarks.forEach(function (bench) {
  compareBench(bench, mapB)
})

b.benchmarks.forEach(function (bench) {
  compareBench(bench, mapA)
})

write('NANOBENCH version 2', '|', 'NANOBENCH version 2')
write('> ' + a.command,      '|', '> ' + b.command)
write('',                    '|', '')

list.forEach(function (bench) {
  var left = bench
  var right = bench.other

  if (mapB[left.name] === left) {
    var tmp = left
    left = right
    right = tmp
  }

  var sep = '='
  if (left.winner) sep = '<'
  if (right && right.winner) sep = '>'

  write('# ' + left.name, sep, right && '# ' + right.name)

  var len = Math.max(left.output.length, right ? right.output.length : 0)
  for (var i = 0; i < len; i++) {
    write(left.output[i] && '# ' + left.output[i], sep, right.output[i] && '# ' + right.output[i])
  }

  write(result(left), sep, result(right))
  write('', '|', '')
})

var last = compare(a.time, b.time)
var sep = last === 1 ? '>' : last === -1 ? '<' : '='

write('all benchmarks completed', sep, 'all benchmarks completed')
write(result(a), sep, result(b))
write('', '|', '')

function result (b) {
  if (!b) return ''
  if (b.error) return 'fail ' + b.error
  return 'ok ~' + prettyHrtime(b.time) + ' ' + rawTime(b.time)
}

function write (a, sep, b) {
  if (!b) b = ''
  while (a.length < 33) a += ' '

  if (sep === '|') {
    sep = ' | '
    a = chalk.gray(a)
    b = chalk.gray(b)
  }
  if (sep === '<') {
    sep = '<<<'
    a = chalk.green(a)
    b = chalk.red(b)
  }
  if (sep === '>') {
    sep = '>>>'
    a = chalk.red(a)
    b = chalk.green(b)
  }
  if (sep === '=') sep = '==='

  console.log(a + sep + '   ' +b)
}

function compareBench (bench, map) {
  var other = map[bench.name]
  if (!other) return
  if (list.indexOf(other) > -1) return

  var cmp = compare(bench.time, other.time)

  bench.other = other
  list.push(bench)

  var winner = null
  if (cmp === 1) winner = other
  if (cmp === -1) winner = bench

  var loser = winner === bench ? other : bench

  if (winner) {
    winner.winner = true
    loser.loser = true
  }
}

function compare (a, b) {
  var pct = (a[0] * 1e9 + a[1]) / (b[0] * 1e9 + b[1])
  if (pct > upper) return 1
  if (pct < lower) return -1
  return 0
}

function toMap (output) {
  var map = {}
  output.benchmarks.forEach(function (b) {
    map[b.name] = b
  })
  return map
}

function rawTime (hr) {
  return '(' + hr[0] + ' s + ' + hr[1] + ' ns)'
}
