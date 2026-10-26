package io.github.grahamfairbank.vmeinsight

import com.morpheusdata.views.HandlebarsRenderer
import com.morpheusdata.views.ViewModel
import spock.lang.Specification

/** Renders the Handlebars templates the way VME does, to catch syntax and escaping mistakes. */
class TemplateSpec extends Specification {

	HandlebarsRenderer renderer = new HandlebarsRenderer('renderer', getClass().classLoader)

	private String render(String template, Map object) {
		ViewModel<Map> model = new ViewModel<>()
		model.object = object
		return renderer.renderTemplate(template, model).html
	}

	/** Attribute values as the browser sees them (Handlebars writes = as &#x3D; and & as &amp;). */
	private static String decoded(String html) {
		return html.replace('&#x3D;', '=').replace('&amp;', '&')
	}

	def "tab embeds the kiosk URL, escapes it, and links the full dashboard"() {
		given:
		def s = InsightSettings.fromJson('{"grafanaUrl":"https://g.example.com","grafanaOrgId":"1"}')

		when:
		String html = render('hbs/grafana-tab', [title: 'VME Hosts', embedUrl: s.hostDashboard(9L),
			openUrl: s.hostDashboard(9L, false), height: 1400, insecure: s.insecureUrl, grafana: s.baseUrl])

		then:
		decoded(html).contains('<iframe src="https://g.example.com/d/vme-hosts?var-host=9&kiosk&orgId=1"')
		decoded(html).contains('href="https://g.example.com/d/vme-hosts?var-host=9&orgId=1"')
		html.contains('height="1400"')
		html.contains('href="https://github.com/grahamfairbank/vme-insight-plugin/blob/main/docs/TROUBLESHOOTING.md"')
		!html.contains('Browsers block http frames')
	}

	def "tab warns when Grafana is plain http"() {
		expect:
		render('hbs/grafana-tab', [title: 't', embedUrl: 'http://g/d/x', openUrl: 'http://g/d/x', height: 500,
			insecure: true, grafana: 'http://g']).contains('Browsers block http frames')
	}

	def "widget renders one frame per panel, or a hint when unconfigured"() {
		given:
		def s = InsightSettings.fromJson('{"grafanaUrl":"https://g","widgetPanels":"36,39"}')

		when:
		String html = render('hbs/summary-widget', [configured: true, insecure: false, panels: s.widgetPanelUrls(),
			openUrl: s.overviewDashboard()])
		String empty = render('hbs/summary-widget', [configured: false, panels: []])

		then:
		html.count('<iframe') == 2
		decoded(html).contains('src="https://g/d-solo/vme-widget?panelId=39"')
		empty.contains('Set the vme-insight Grafana URL')
		!empty.contains('<iframe')
	}

	def "values are HTML-escaped"() {
		expect:
		!render('hbs/grafana-tab', [title: '<b>x</b>', embedUrl: 'https://g/"><script>', openUrl: 'https://g',
			height: 500, grafana: 'https://g']).contains('<script>')
	}

	def "launcher is a plain link"() {
		expect:
		render('hbs/launcher', [openUrl: 'https://g/d/vme-overview']).contains('href="https://g/d/vme-overview"')
	}
}
