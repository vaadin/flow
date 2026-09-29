export interface Options {
	/**
	- `true` - Preserve important comments `/*! *\/`.
	- `false` - Strip all comments.
	- `RegExp` - Preserve comments where the comment body matches a regular expression.
	- `Function` - Preserve comments for which a function returns `true`. The function is called on each comment, gets the comment body as the first argument, and is expected to return a boolean of whether to preserve the comment.

	@default true
	*/
	readonly preserve?: boolean | RegExp | ((comment: string) => boolean);

	/**
	Replace comments with whitespace instead of stripping them entirely.

	@default true
	*/
	readonly whitespace?: boolean;
}

/**
Strip comments from CSS.

@param cssString - String with CSS.

@example
```
import stripCssComments from 'strip-css-comments';

// By default important comments `/*!` are preserved
stripCssComments('/*! <copyright> *\/ body { /* unicorns *\/color: hotpink; }');
//=> '/*! <copyright> *\/ body { color: hotpink; }'

// `preserve: false` will strip all comments including `/*!`
stripCssComments(
	'/*! <copyright> *\/ body { /* unicorns *\/color: hotpink; }',
	{preserve: false}
);
//=> 'body { color: hotpink; }'

// Preserve comments based on a regex
stripCssComments(
	'/*# preserved *\/ body { /* unicorns *\/color: hotpink; }',
	{preserve: /^#/}
);
//=> '/*# preserved *\/ body { color: hotpink; }'

// Preserve comments based on the return value of the supplied function
stripCssComments(
	'/*# preserved *\/ body { /* unicorns *\/color: hotpink; }',
	{
		preserve: comment => comment.charAt(0) === '#'
	}
);
//=> '/*# preserved *\/ body { color: hotpink; }'
```
*/
export default function stripCssComments(
	cssString: string,
	options?: Options
): string;
