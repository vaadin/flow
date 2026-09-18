# nanobench

Simple benchmarking tool with TAP-like output that is easy to parse.

```
npm install nanobench
```

## Usage

``` js
var bench = require('nanobench')

bench('sha256 200.000 times', function (b) {
  var crypto = require('crypto')
  var data = new Buffer('hello world')

  b.start()

  for (var i = 0; i < 200000; i++) {
    data = crypto.createHash('sha256').update(data).digest()
  }

  b.end()
})

bench('sha1 200.000 times', function (b) {
  var crypto = require('crypto')
  var data = new Buffer('hello world')

  b.start()

  for (var i = 0; i < 200000; i++) {
    data = crypto.createHash('sha1').update(data).digest()
  }

  b.end()
})
```

Running the above will produce output similar to this:

```
NANOBENCH version 2
> node example.js

# sha1 200.000 times
ok ~554 ms (0 s + 554449000 ns)

# sha256 200.000 times
ok ~598 ms (0 s + 597703365 ns)

all benchmarks completed
ok ~1.15 s (1 s + 152152365 ns)
```

## API

#### `benchmark(name, run)`

Add a new benchmark. `run` is called with a benchmark object, `b` that has the following methods

* `b.start()` - Start the benchmark. If not called the bench will be tracked from the beginning of the function.
* `b.end()` - End the benchmark.
* `b.error(err)` - Benchmark failed. Report error.
* `b.log(msg)` - Log out a message

#### `benchmark.skip(name, run)`

Skip a benchmark.

#### `benchmark.only(name, run)`

Only run this benchmark.

## CLI

If you have multiple benchmarks as different files you can use the cli benchmark runner to run them all

```
npm install -g nanobench
nanobench benchmarks/*.js
```

## Comparing benchmarks

A comparison tool for comparing two benchmark outputs is included as well.
This is useful if fx you ran a benchmark on two different git checkouts and want to compare which
one was the fastest one

``` sh
> git checkout hash-using-sha256
> node benchmark.js > output-sha256
> git checkout hash-using-blake2b
> node benchmark.js > output-blake2b

nanobench-compare output-sha256 output-blake2b
```

The compare tool will print out something like this

```
NANOBENCH version 2               |    NANOBENCH version 2
> node benchmark.js               |    > node benchmark.js
                                  |
# hashing 200.000 times          >>>   # hashing 200.000 times
# (using sha256)                 >>>   # (using blake2b)
ok ~591 ms (0 s + 590687187 ns)  >>>   ok ~95 ms (0 s + 95347216 ns)
                                  |
all benchmarks completed         >>>   all benchmarks completed
ok ~591 ms (0 s + 590687187 ns)  >>>   ok ~95 ms (0 s + 95347216 ns)
                                  |
```

Where `>>>` means that right one was faster, `<<<` that the left one was, and `===` that they were within 5% of other



## Parser

An parser for the output format is included as well. You can require it from node using

``` js
var parse = require('nanobench/parse')
var output = parse(outputAsString)
console.log(output)
```

If you parse the above example output an object similar to this will be printed out

``` js
{ type: 'NANOBENCH',
  version: 2,
  command: 'nanobench example.js',
  benchmarks:
   [ { name: 'sha256 200.000 times',
       output: [],
       error: null,
       time: [Object] },
     { name: 'sha1 200.000 times',
       output: [],
       error: null,
       time: [Object] },
     { name: 'sha256 200.000 times',
       output: [],
       error: null,
       time: [Object] } ],
  error: null,
  time: [ 1, 802600099 ] }
```

## License

MIT
