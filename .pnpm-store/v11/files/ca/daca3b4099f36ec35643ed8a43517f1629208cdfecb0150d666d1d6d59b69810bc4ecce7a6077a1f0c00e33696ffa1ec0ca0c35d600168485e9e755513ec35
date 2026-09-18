import isRegExp from 'is-regexp';

export default function stripCssComments(cssString, {preserve = true, whitespace = true, all} = {}) {
	if (all) {
		throw new Error('The `all` option is no longer supported. Use the `preserve` option instead.');
	}

	let preserveImportant = preserve;
	let preserveFilter;
	if (typeof preserve === 'function') {
		preserveImportant = false;
		preserveFilter = preserve;
	} else if (isRegExp(preserve)) {
		preserveImportant = false;
		preserveFilter = comment => preserve.test(comment);
	}

	let isInsideString = false;
	let currentCharacter = '';
	let comment = '';
	let returnValue = '';

	for (let index = 0; index < cssString.length; index++) {
		currentCharacter = cssString[index];

		if (cssString[index - 1] !== '\\' && (currentCharacter === '"' || currentCharacter === '\'')) {
			if (isInsideString === currentCharacter) {
				isInsideString = false;
			} else if (!isInsideString) {
				isInsideString = currentCharacter;
			}
		}

		// Find beginning of `/*` type comment
		if (!isInsideString && currentCharacter === '/' && cssString[index + 1] === '*') {
			// Ignore important comment when configured to preserve comments using important syntax: /*!
			const isImportantComment = cssString[index + 2] === '!';
			let index2 = index + 2;

			// Iterate over comment
			for (; index2 < cssString.length; index2++) {
				// Find end of comment
				if (cssString[index2] === '*' && cssString[index2 + 1] === '/') {
					if ((preserveImportant && isImportantComment) || (preserveFilter && preserveFilter(comment))) {
						returnValue += `/*${comment}*/`;
					} else if (!whitespace) {
						if (cssString[index2 + 2] === '\n') {
							index2++;
						} else if (cssString[index2 + 2] + cssString[index2 + 3] === '\r\n') {
							index2 += 2;
						}
					}

					comment = '';

					break;
				}

				// Store comment text
				comment += cssString[index2];
			}

			// Resume iteration over CSS string from the end of the comment
			index = index2 + 1;

			continue;
		}

		returnValue += currentCharacter;
	}

	return returnValue;
}
