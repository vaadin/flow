$0.init = function() {
  var firstSizeReported = false;

  function isCSS1Compat() {
    return document.compatMode == "CSS1Compat";
  }

  function onResize() {
    if (isCSS1Compat()) {
      $0.$server.windowResized(document.documentElement.clientWidth,
              document.documentElement.clientHeight);
    } else {
      $0.$server.windowResized(document.body.clientWidth,
              document.body.clientHeight);
    }
  }

  var resizeTimeout = null;

  function resizeThrottler() {
    if (firstSizeReported) {
      if (!resizeTimeout) {
        resizeTimeout = setTimeout(function() {
            resizeTimeout = null;
            onResize();
        }, 100);
      }
    } else {
      firstSizeReported = true;
      onResize();
    }
  }

  window.addEventListener('resize', resizeThrottler);

  $0.resizeRemove = function() {
    window.removeEventListener('resize', resizeThrottler);
  };
};

$0.init();
