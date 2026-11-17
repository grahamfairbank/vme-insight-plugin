/*
 * vme-insight: make the embedded Grafana frames follow the viewer's Morpheus theme.
 *
 * Loaded on every page (VmeInsightThemeLoaderProvider) only while the plugin's Grafana theme setting is
 * "Match each user's Morpheus theme". The plugin API doesn't say which theme a user picked, so this reads it
 * off the page: how dark the background behind each frame is. Frames and links marked
 * data-vme-insight-theme="morpheus" get theme=dark or theme=light added to their URL.
 */
(function () {
	'use strict';
	var ATTR = 'data-vme-insight-theme';

	function rgba(color) {
		var m = /rgba?\(([^)]+)\)/.exec(color || '');
		if (!m) return null;
		var p = m[1].split(',').map(function (x) { return parseFloat(x); });
		return { r: p[0], g: p[1], b: p[2], a: p.length > 3 ? p[3] : 1 };
	}

	// Relative luminance (WCAG) of the first opaque background behind el.
	function backgroundLuminance(el) {
		for (var e = el; e && e.nodeType === 1; e = e.parentElement) {
			var c = rgba(window.getComputedStyle(e).backgroundColor);
			if (c && c.a > 0.5) {
				var ch = [c.r, c.g, c.b].map(function (v) {
					v = v / 255;
					return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
				});
				return 0.2126 * ch[0] + 0.7152 * ch[1] + 0.0722 * ch[2];
			}
		}
		return 1; // nothing opaque: the browser's white page
	}

	function themed(url, theme) {
		if (!url || /[?&]theme=/.test(url)) return url;
		return url + (url.indexOf('?') >= 0 ? '&' : '?') + 'theme=' + theme;
	}

	function apply(root) {
		var els = (root.querySelectorAll ? root.querySelectorAll('[' + ATTR + '="morpheus"]') : []);
		for (var i = 0; i < els.length; i++) {
			var el = els[i];
			var theme = backgroundLuminance(el.parentElement || el) < 0.18 ? 'dark' : 'light';
			el.setAttribute(ATTR, theme);
			if (el.tagName === 'IFRAME') {
				var src = el.getAttribute('src');
				var next = themed(src, theme);
				if (next !== src) el.setAttribute('src', next);
			} else if (el.tagName === 'A') {
				el.setAttribute('href', themed(el.getAttribute('href'), theme));
			}
		}
	}

	function start() {
		apply(document);
		// Tabs and dashboard widgets are added after the page loads.
		if (window.MutationObserver) {
			new MutationObserver(function (records) {
				for (var i = 0; i < records.length; i++) {
					var added = records[i].addedNodes;
					for (var j = 0; j < added.length; j++) {
						if (added[j].nodeType === 1) {
							if (added[j].getAttribute(ATTR) === 'morpheus') apply(added[j].parentElement || document);
							else apply(added[j]);
						}
					}
				}
			}).observe(document.body, { childList: true, subtree: true });
		}
	}

	if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start);
	else start();
})();
