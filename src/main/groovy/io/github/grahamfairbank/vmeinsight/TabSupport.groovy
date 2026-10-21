package io.github.grahamfairbank.vmeinsight

import com.morpheusdata.model.ContentSecurityPolicy
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.Renderer
import com.morpheusdata.views.ViewModel

/**
 * What every vme-insight tab has in common: the frame-src policy and the embed template.
 */
class TabSupport {

	/** Lets VME's Content-Security-Policy accept frames from the configured Grafana origin. */
	static ContentSecurityPolicy contentSecurityPolicy(VmeInsightPlugin plugin) {
		String origin = plugin.frameOrigin()
		if (!origin) return null
		ContentSecurityPolicy csp = new ContentSecurityPolicy()
		csp.frameSrc = origin
		return csp
	}

	/**
	 * Renders hbs/grafana-tab: the dashboard in an iframe, with a link that opens it full size.
	 * @param embedUrl the kiosk URL for the frame
	 * @param openUrl the same dashboard without kiosk mode, for the "open in a new tab" link
	 */
	static HTMLResponse render(Renderer<?> renderer, InsightSettings settings, String title, String embedUrl, String openUrl) {
		ViewModel<Map> model = new ViewModel<>()
		model.object = [
			title    : title,
			embedUrl : embedUrl,
			openUrl  : openUrl,
			height   : settings.tabHeight,
			insecure : settings.insecureUrl,
			grafana  : settings.baseUrl
		]
		return renderer.renderTemplate('hbs/grafana-tab', model)
	}
}
