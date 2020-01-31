const fs = require('fs');

const packageJson = JSON.parse(fs.readFileSync('./package.json', 'utf-8'));

const versionsFile = '[to-be-generated-by-flow]';

if (!fs.existsSync(versionsFile)) {
    return;
}
const versions = JSON.parse(fs.readFileSync(versionsFile, 'utf-8'));

module.exports = {
  hooks: {
    readPackage
  }
};

function getExactVersion(string) {
  return string.replace('^', '');
}


function readPackage(pkg) {
  const { dependencies } = pkg;

  if (dependencies) {
    for (let k in versions) {
      if (dependencies[k] && dependencies[k] !== versions[k]) {
        pkg.dependencies[k] = versions[k];
      }
    }
  }

  return pkg;
}