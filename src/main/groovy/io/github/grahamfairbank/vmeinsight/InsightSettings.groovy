package io.github.grahamfairbank.vmeinsight

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

/**
 * The plugin's settings (Administration > Integrations > Plugins > vme-insight), parsed once per
 * render, and every Grafana URL the plugin builds from them.
 *
 * Kept free of VME classes so it can be unit tested on its own.
 */
@CompileStatic
class InsightSettings {

	// Field names used in VmeInsightPlugin's option types. VME stores settings as JSON keyed by these.
	static final String GRAFANA_URL = 'grafanaUrl'
	static final String ORG_ID = 'grafanaOrgId'
	static final String THEME = 'grafanaTheme'
	static final String TAB_HEIGHT = 'tabHeight'
	static final String WIDGET_PANELS = 'widgetPanels'
	static final String HOST_TYPE_CODES = 'hostTypeCodes'
	static final String VM_TYPE_CODES = 'vmTypeCodes'
	static final String REQUIRE_PERMISSION = 'requirePermission'
	static final String SHOW_LAUNCHER = 'showLauncher'

	static final int DEFAULT_TAB_HEIGHT = 1400
	// vme-overview panels: Clusters, Hosts, VMs, VMs running, Active alarms, VME manager,
	// Worst VM CPU ready, VMs over 10% ready. Panel ids come from vme-insight 0.1's dashboard build.
	static final String DEFAULT_WIDGET_PANELS = '2,3,4,5,8,9,36,39'

	/** Grafana base URL without a trailing slash, or null when unset or not http(s). */
	final String baseUrl
	final Long orgId
	final String theme
	final int tabHeight
	final List<Integer> widgetPanels
	final Set<String> hostTypeCodes
	final Set<String> vmTypeCodes
	final boolean requirePermission
	final boolean showLauncher

	InsightSettings(Map<String, Object> raw) {
		Map<String, Object> s = raw ?: [:]
		baseUrl = normaliseUrl(str(s[GRAFANA_URL]))
		orgId = positiveLong(str(s[ORG_ID]))
		String t = str(s[THEME])?.toLowerCase()
		theme = (t in ['light', 'dark']) ? t : null
		Long h = positiveLong(str(s[TAB_HEIGHT]))
		tabHeight = (h != null && h >= 300 && h <= 10000) ? h.intValue() : DEFAULT_TAB_HEIGHT
		widgetPanels = parsePanels(str(s[WIDGET_PANELS]) ?: DEFAULT_WIDGET_PANELS)
		hostTypeCodes = codes(str(s[HOST_TYPE_CODES]))
		vmTypeCodes = codes(str(s[VM_TYPE_CODES]))
		requirePermission = truthy(s[REQUIRE_PERMISSION])
		showLauncher = truthy(s[SHOW_LAUNCHER])
	}

	/** Parses the JSON string VME returns from {@code morpheus.getSettings(plugin)}. */
	static InsightSettings fromJson(String json) {
		Object parsed = null
		if (json?.trim()) {
			try {
				parsed = new JsonSlurper().parseText(json)
			} catch (Exception ignored) {
				parsed = null
			}
		}
		Map<String, Object> map = parsed instanceof Map ? (Map<String, Object>) parsed : new HashMap<String, Object>()
		return new InsightSettings(map)
	}

	boolean isConfigured() {
		return baseUrl != null
	}

	/** True when the URL is plain http, which browsers block inside VME's https pages (mixed content). */
	boolean isInsecureUrl() {
		return baseUrl?.startsWith('http://')
	}

	/** scheme://host[:port] of the Grafana URL, for the Content-Security-Policy frame-src. */
	String getOrigin() {
		if (!baseUrl) return null
		URI u = new URI(baseUrl)
		return u.port > 0 ? "${u.scheme}://${u.host}:${u.port}".toString() : "${u.scheme}://${u.host}".toString()
	}

	String hostDashboard(Long serverId, boolean kiosk = true) {
		return dashboard('vme-hosts', [new Param('var-host', serverId)], kiosk)
	}

	String vmDashboard(Collection<Long> serverIds, boolean kiosk = true) {
		return dashboard('vme-vms', serverIds.collect { Long id -> new Param('var-vm', id) }, kiosk)
	}

	String clusterDashboard(Long clusterId, boolean kiosk = true) {
		return dashboard('vme-overview', [new Param('var-cluster', clusterId)], kiosk)
	}

	String overviewDashboard() {
		return dashboard('vme-overview', new ArrayList<Param>(), false)
	}

	/** Single-panel URLs (Grafana /d-solo) for the home dashboard widget. */
	List<String> widgetPanelUrls() {
		if (!baseUrl) return []
		return widgetPanels.collect { Integer id -> url('/d-solo/vme-overview', [new Param('panelId', id)]) }
	}

	/** One query parameter; a null value is written as a bare flag (e.g. kiosk). */
	private static class Param {
		final String key
		final Object value
		Param(String key, Object value) { this.key = key; this.value = value }
		String toString() { value == null ? enc(key) : "${enc(key)}=${enc(value.toString())}".toString() }
	}

	private String dashboard(String uid, List<Param> vars, boolean kiosk) {
		if (!baseUrl) return null
		List<Param> params = new ArrayList<Param>(vars)
		if (kiosk) params.add(new Param('kiosk', null))
		return url("/d/${uid}".toString(), params)
	}

	private String url(String path, List<Param> params) {
		List<Param> all = new ArrayList<Param>(params)
		if (orgId != null) all.add(new Param('orgId', orgId))
		if (theme) all.add(new Param('theme', theme))
		String query = all.collect { Param p -> p.toString() }.join('&')
		return query ? "${baseUrl}${path}?${query}".toString() : "${baseUrl}${path}".toString()
	}

	/** Mirrors vme-insight's collector: which servers count as hypervisor hosts. */
	boolean isHost(String typeCode, Boolean vmHypervisor, Boolean containerHypervisor, String nodeType) {
		String code = (typeCode ?: '').toLowerCase()
		if (code in vmTypeCodes) return false
		if (code in hostTypeCodes || vmHypervisor || containerHypervisor) return true
		return code.endsWith('host') || code.contains('hypervisor') || nodeType == 'morpheus-node'
	}

	// ---------------------------------------------------------------- parsing helpers

	static String normaliseUrl(String s) {
		if (!s) return null
		String v = s.trim()
		while (v.endsWith('/')) v = v.substring(0, v.length() - 1)
		try {
			URI u = new URI(v)
			String scheme = u.scheme?.toLowerCase()
			if (!(scheme in ['http', 'https']) || !u.host) return null
			if (u.rawQuery || u.rawFragment || u.rawUserInfo) return null
		} catch (URISyntaxException ignored) {
			return null
		}
		return v
	}

	private static String str(Object o) {
		if (o == null) return null
		String s = o.toString().trim()
		return s ? s : null
	}

	private static Long positiveLong(String s) {
		if (!s || !s.isLong()) return null
		Long v = s.toLong()
		return v > 0 ? v : null
	}

	private static boolean truthy(Object o) {
		return o != null && o.toString().trim().toLowerCase() in ['on', 'true', '1', 'yes']
	}

	private static List<Integer> parsePanels(String s) {
		return (s ?: '').split(/[,\s]+/).findAll { String p -> p.isInteger() && p.toInteger() > 0 }
			.collect { String p -> p.toInteger() }.unique()
	}

	private static Set<String> codes(String s) {
		return ((s ?: '').split(/[,\s]+/) as List<String>).findAll { it }.collect { it.toLowerCase() } as Set<String>
	}

	private static String enc(String s) {
		return URLEncoder.encode(s, 'UTF-8')
	}
}
