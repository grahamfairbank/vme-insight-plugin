package io.github.grahamfairbank.vmeinsight

import spock.lang.Specification
import spock.lang.Unroll

class InsightSettingsSpec extends Specification {

	def "unset or unusable URLs leave the plugin unconfigured"() {
		expect:
		!InsightSettings.fromJson(json).configured

		where:
		json << [null, '', 'not json', '{}', '{"grafanaUrl":""}', '{"grafanaUrl":"javascript:alert(1)"}',
		         '{"grafanaUrl":"ftp://x"}', '{"grafanaUrl":"https://"}', '{"grafanaUrl":"https://g/?a=b"}',
		         '{"grafanaUrl":"https://user:pw@g"}']
	}

	def "host, VM and cluster dashboards use vme-insight's variables in kiosk mode"() {
		given:
		def s = InsightSettings.fromJson('{"grafanaUrl":"https://grafana.example.com:3443/"}')

		expect:
		s.configured
		!s.insecureUrl
		s.origin == 'https://grafana.example.com:3443'
		s.hostDashboard(12L) == 'https://grafana.example.com:3443/d/vme-hosts?var-host=12&kiosk'
		s.vmDashboard([34L]) == 'https://grafana.example.com:3443/d/vme-vms?var-vm=34&kiosk'
		s.vmDashboard([34L, 35L], false) == 'https://grafana.example.com:3443/d/vme-vms?var-vm=34&var-vm=35'
		s.clusterDashboard(5L) == 'https://grafana.example.com:3443/d/vme-overview?var-cluster=5&kiosk'
		s.overviewDashboard() == 'https://grafana.example.com:3443/d/vme-overview'
	}

	def "Grafana under a sub-path keeps the path, and the origin drops it"() {
		given:
		def s = InsightSettings.fromJson('{"grafanaUrl":"https://tools.example.com/grafana"}')

		expect:
		s.origin == 'https://tools.example.com'
		s.clusterDashboard(1L) == 'https://tools.example.com/grafana/d/vme-overview?var-cluster=1&kiosk'
	}

	def "orgId and theme are appended when set"() {
		given:
		def s = InsightSettings.fromJson('{"grafanaUrl":"https://g","grafanaOrgId":"2","grafanaTheme":"Dark"}')

		expect:
		s.hostDashboard(7L) == 'https://g/d/vme-hosts?var-host=7&kiosk&orgId=2&theme=dark'
		s.widgetPanelUrls()[0] == 'https://g/d-solo/vme-widget?panelId=1&orgId=2&theme=dark'
	}

	def "bad theme, org and height values fall back to defaults"() {
		given:
		def s = InsightSettings.fromJson('{"grafanaUrl":"http://g:3000","grafanaOrgId":"x","grafanaTheme":"blue","tabHeight":"50"}')

		expect:
		s.insecureUrl
		s.orgId == null
		s.theme == null
		s.tabHeight == InsightSettings.DEFAULT_TAB_HEIGHT
		s.origin == 'http://g:3000'
	}

	def "widget panels parse from a list and default when blank"() {
		expect:
		InsightSettings.fromJson('{"grafanaUrl":"https://g","widgetPanels":"36, 39 x 0 36"}').widgetPanels == [36, 39]
		InsightSettings.fromJson('{"grafanaUrl":"https://g"}').widgetPanels == [1, 2, 3, 4, 7, 8, 9, 12]
	}

	def "the 0.1.x default panel list is read as the new default"() {
		expect:
		InsightSettings.fromJson('{"grafanaUrl":"https://g","widgetPanels":"2,3,4,5,8,9,36,39"}').widgetPanels == [1, 2, 3, 4, 7, 8, 9, 12]
		InsightSettings.fromJson('{"grafanaUrl":"https://g","widgetPanels":"2, 3, 4, 5, 8, 9, 36, 39"}').widgetPanels == [1, 2, 3, 4, 7, 8, 9, 12]
	}

	@Unroll
	def "checkbox value #v reads as #expected"() {
		expect:
		InsightSettings.fromJson("{\"grafanaUrl\":\"https://g\",\"showLauncher\":${v}}").showLauncher == expected

		where:
		v       | expected
		'"on"'  | true
		'true'  | true
		'"off"' | false
		'false' | false
		'null'  | false
	}

	@Unroll
	def "server type #code (vmHypervisor=#hyp, nodeType=#node) is a host: #expected"() {
		given:
		def s = InsightSettings.fromJson('{"grafanaUrl":"https://g","hostTypeCodes":"oddHost2","vmTypeCodes":"mvmHost-special"}')

		expect:
		s.isHost(code, hyp, false, node) == expected

		where:
		code              | hyp   | node            | expected
		'mvmHost'         | false | 'morpheus-node' | true
		'kvmHypervisor'   | false | null            | true
		'whatever'        | true  | null            | true
		'oddhost2'        | false | null            | true
		'mvm-special'     | false | null            | false
		'mvmHost-special' | false | 'morpheus-node' | false
		'mvmVm'           | false | 'morpheus-vm'   | false
		null              | null  | null            | false
	}

	def "plugin permission is only enforced when the setting is on"() {
		given:
		def open = InsightSettings.fromJson('{"grafanaUrl":"https://g"}')
		def strict = InsightSettings.fromJson('{"grafanaUrl":"https://g","requirePermission":"on"}')
		def granted = new com.morpheusdata.model.User(permissions: ['vme-insight-view': 'read'])
		def denied = new com.morpheusdata.model.User(permissions: ['vme-insight-view': 'none'])

		expect:
		VmeInsightPlugin.canView(open, denied)
		VmeInsightPlugin.canView(strict, granted)
		!VmeInsightPlugin.canView(strict, denied)
		!VmeInsightPlugin.canView(strict, null)
		!VmeInsightPlugin.canView(InsightSettings.fromJson('{}'), granted)
	}
}
