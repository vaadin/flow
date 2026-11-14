/**
 * Formatting shared by the scripts that report measurements in pull request
 * comments, see memoryFootprint.js and bundleSize.js.
 */

function formatValue(value, unit) {
  const number = value.toLocaleString('en-US');
  if (unit === 'bytes') {
    return `${number} B`;
  }
  return unit ? `${number} ${unit}` : number;
}

/**
 * The difference from base to head, in bold and colored green when the value
 * shrinks and red when it grows, followed by the percentage.
 */
function formatDifference(base, head, unit) {
  const difference = head - base;
  const sign = difference > 0 ? '+' : '−';
  const percent = ((Math.abs(difference) / base) * 100).toFixed(1);
  const color = difference > 0 ? 'red' : 'green';
  // GitHub colors text only in math, where \textbf keeps the spaces. The
  // percentage stays outside, as Markdown would unescape the \% math needs
  return `$\\color{${color}}\\textbf{${sign}${formatValue(Math.abs(difference), unit)}}$ (${sign}${percent}%)`;
}

module.exports = { formatValue, formatDifference };
