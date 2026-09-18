# strip-css-comments

> Strip comments from CSS

Also available as a [Gulp](https://github.com/sindresorhus/gulp-strip-css-comments)/[Grunt](https://github.com/sindresorhus/grunt-strip-css-comments)/[Broccoli](https://github.com/sindresorhus/broccoli-strip-css-comments) plugin.

## Usage

```
$ npm install strip-css-comments
```

## Usage

```js
import stripCssComments from 'strip-css-comments';

// By default important comments `/*!` are preserved
stripCssComments('/*! <copyright> */ body { /* unicorns */color: hotpink; }');
//=> '/*! <copyright> */ body { color: hotpink; }'

// `preserve: false` will strip all comments including `/*!`
stripCssComments(
	'/*! <copyright> */ body { /* unicorns */color: hotpink; }',
	{preserve: false}
);
//=> 'body { color: hotpink; }'

// Preserve comments based on a regex
stripCssComments(
	'/*# preserved */ body { /* unicorns */color: hotpink; }',
	{preserve: /^#/}
);
//=> '/*# preserved */ body { color: hotpink; }'

// Preserve comments based on the return value of the supplied function
stripCssComments(
	'/*# preserved */ body { /* unicorns */color: hotpink; }',
	{
		preserve: comment => comment.charAt(0) === '#'
	}
);
//=> '/*# preserved */ body { color: hotpink; }'
```

## API

### stripCssComments(cssString, options?)

## cssString

Type: `string`

String with CSS.

## options

Type: `object`

### preserve

Type: `boolean | RegExp | Function`\
Default: `true`

- `true` - Preserve important comments `/*! */`.
- `false` - Strip all comments.
- `RegExp` - Preserve comments where the comment body matches a regular expression.
- `Function` - Preserve comments for which a function returns `true`. The function is called on each comment, gets the comment body as the first argument, and is expected to return a boolean of whether to preserve the comment.

### whitespace

Type: `boolean`\
Default: `true`

Replace comments with whitespace instead of stripping them entirely.

## Benchmark

```
$ npm run bench
```

## Related

- [strip-css-comments-cli](https://github.com/sindresorhus/strip-css-comments-cli) - CLI for this module
- [strip-json-comments](https://github.com/sindresorhus/strip-json-comments) - Strip comments from JSON
