module.exports = parse

function parse (data) {
  var lines = data.toString().trim().split('\n')
    .map(function (line) {
      return line.trim()
    })
    .filter(function (line) {
      return line
    })

  var output = {}

  while (lines.length && !output.type) {
    var header = lines.shift().match(/^NANOBENCH(?: version (\d+))?$/)
    if (!header) continue
    output.type = 'NANOBENCH'
    output.version = Number(header[1] || 1)
  }

  if (!output.type) {
    throw new Error('No NANOBENCH header')
  }
  if (output.version !== 2) {
    throw new Error('Can only parse version 2')
  }

  output.command = null
  output.benchmarks = []
  output.error = null
  output.time = null

  var benchmark = null

  while (lines.length) {
    var next = lines.shift()
    if (next[0] === '>') {
      output.command = next.slice(1).trim()
      continue
    }

    if (!benchmark && next[0] === '#') {
      benchmark = {name: null, output: [], error: null, time: null}
      benchmark.name = next.slice(1).trim()
      continue
    }

    if (benchmark && next[0] === '#') {
      benchmark.output.push(next.slice(1).trim())
      continue
    }
    if (!benchmark && /^fail /.test(next)) {
      output.error = next.slice(5).trim()
      continue
    }

    if (!benchmark && /^ok /.test(next)) {
      output.error = null
      output.time = time(next)
      continue
    }

    if (benchmark && /^ok /.test(next)) {
      benchmark.error = null
      benchmark.time = time(next)
      output.benchmarks.push(benchmark)
      benchmark = null
      continue
    }

    if (benchmark && /^fail /.test(next)) {
      output.error = next.slice(5).trim()
      output.benchmarks.push(benchmark)
      benchmark = null
      continue
    }
  }

  return output
}

function time (line) {
  var i = line.lastIndexOf('(')
  var j = line.lastIndexOf(')')
  if (i === -1 || j === -1 || j < i) throw new Error('Could not parse benchmark time')
  var parsed = line.slice(i + 1, j).match(/^(\d+)\s*s\s*\+\s*(\d+)\s*ns$/)
  if (!parsed) throw new Error('Could not parse benchmark time')
  return [Number(parsed[1]), Number(parsed[2])]
}
