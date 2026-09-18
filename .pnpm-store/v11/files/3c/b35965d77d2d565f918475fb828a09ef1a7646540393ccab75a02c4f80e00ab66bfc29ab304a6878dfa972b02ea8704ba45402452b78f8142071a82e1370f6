var toString = require('../lang/toString');
var escapeRegExp = require('./escapeRegExp');
    var DEFAULT_MARGIN_CHAR = '|';
    /**
     * Strip leading characters followed by 'marginChar' from every line in a String.
     *
     * marginChar defaults to '|'.
     */
    function stripMargin(str, marginChar) {
        var regexp;

        marginChar = escapeRegExp(marginChar || DEFAULT_MARGIN_CHAR);
        str = toString(str);

        regexp = new RegExp('^.*' + marginChar, 'gm');

        return str.replace(regexp, '');
    }

    module.exports = stripMargin;


