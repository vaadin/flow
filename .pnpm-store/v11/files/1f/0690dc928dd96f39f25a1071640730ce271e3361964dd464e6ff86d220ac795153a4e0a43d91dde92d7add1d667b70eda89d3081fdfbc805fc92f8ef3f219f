function debug (title, content) {
  if (process.env.CI === 'true') {
    console.log(`::group::${title}`)
    console.log(JSON.stringify(content, null, 3))
    console.log('::endgroup::')
  }
}

module.exports = {
  debug: debug
}
