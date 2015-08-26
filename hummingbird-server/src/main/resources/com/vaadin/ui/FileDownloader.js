module.attachDownload =  function(element, url) {
	
	element.addEventListener('click', function(e) {
	
		if (false) { 
			window.open(url, '_blank', '');
		} else {
			var iframe = document.createElement('iframe');
			iframe.style.visibility = 'hidden';
			iframe.style.height = '0px';
			iframe.style.width = '0px';
			iframe.frameBorder = 0;
			iframe.tabIndex = -1;
			iframe.src = url;
			document.body.appendChild(iframe);
			if (!element.downloadFrames) {
				element.downloadFrames = [];
			}
			element.downloadFrames.push(iframe);
		}
	});
	
};

