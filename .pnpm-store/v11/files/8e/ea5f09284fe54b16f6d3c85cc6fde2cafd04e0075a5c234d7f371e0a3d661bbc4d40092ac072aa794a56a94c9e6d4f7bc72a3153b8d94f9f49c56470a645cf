var toString = require('../lang/toString');
var replaceAccents = require('./replaceAccents');
var removeNonWord = require('./removeNonWord');
var trim = require('./trim');
    /**
     * Convert to lower case, remove accents, remove non-word chars and
     * replace spaces with the specified delimiter.
     * Does not split camelCase text.
     */
    function slugify(str, delimiter){
        str = toString(str);

        if (delimiter == null) {
            delimiter = "-";
        }
        str = replaceAccents(str);
        str = removeNonWord(str);
        str = trim(str) //should come after removeNonWord
                .replace(/ +/g, delimiter) //replace spaces with delimiter
                .toLowerCase();
        return str;
    }
    module.exports = slugify;

