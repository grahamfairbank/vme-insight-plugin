package io.github.grahamfairbank.vmeinsight

import com.morpheusdata.core.Plugin
import com.morpheusdata.model.OptionType
import com.morpheusdata.model.Permission
import com.morpheusdata.model.User
import groovy.util.logging.Slf4j

/**
 * vme-insight companion plugin for HPE Morpheus / VM Essentials 9.1+.
 *
 * Adds a "vme-insight" tab to host, VM, instance and cluster pages that embeds the matching
 * vme-insight Grafana dashboard, an optional home dashboard of vme-insight panels, and an optional
 * link to vme-insight on every page. Settings are in Administration > Integrations > Plugins.
 *
 * Community project: not affiliated with, endorsed or supported by HPE or Morpheus Data.
 */
@Slf4j
class VmeInsightPlugin extends Plugin {

	static final String CODE = 'vme-insight-plugin'
	static final String PERMISSION_CODE = 'vme-insight-view'

	@Override
	String getCode() {
		return CODE
	}

	@Override
	void initialize() {
		this.setName('vme-insight')
		this.setDescription('Shows vme-insight Grafana dashboards inside VM Essentials')
		this.setAuthor('vme-insight community project')
		this.setSourceCodeLocationUrl('https://github.com/grahamfairbank/vme-insight-plugin')
		this.setPermissions([
			new Permission('vme-insight: View dashboards', PERMISSION_CODE,
				[Permission.AccessType.none, Permission.AccessType.read])
		])
		this.registerProvider(new VmeInsightServerTabProvider(this, morpheus))
		this.registerProvider(new VmeInsightInstanceTabProvider(this, morpheus))
		this.registerProvider(new VmeInsightClusterTabProvider(this, morpheus))
		this.registerProvider(new VmeInsightDashboardItemProvider(this, morpheus))
		this.registerProvider(new VmeInsightDashboardProvider(this, morpheus))
		this.registerProvider(new VmeInsightLauncherProvider(this, morpheus))
	}

	@Override
	void onDestroy() {
	}

	/** Current settings. Read on every render so changes apply without reloading the plugin. */
	InsightSettings loadSettings() {
		try {
			InsightSettings s = InsightSettings.fromJson(morpheus.getSettings(this).blockingGet())
			if (s.origin) lastOrigin = s.origin
			return s
		} catch (Throwable e) {
			log.warn("vme-insight: could not read plugin settings: ${e}")
			return InsightSettings.fromJson(null)
		}
	}

	/** Grafana origin from the last successful settings read; see frameOrigin(). */
	private volatile String lastOrigin

	/**
	 * The Grafana origin for the Content-Security-Policy frame-src. VME may ask for the policy where
	 * plugin settings can't be read, so fall back to the origin seen on the last successful read.
	 */
	String frameOrigin() {
		String origin = null
		try {
			origin = loadSettings().origin
		} catch (Throwable e) {
			log.warn("vme-insight: frame-src from settings failed: ${e}")
		}
		String result = origin ?: lastOrigin
		log.debug("vme-insight: frame-src ${result ?: '(none)'} (settings: ${origin ?: 'unavailable'})")
		return result
	}

	/** Whether this user may see vme-insight content under the current settings. */
	static boolean canView(InsightSettings settings, User user) {
		if (!settings.configured) return false
		if (!settings.requirePermission) return true
		String level = user?.permissions?.get(PERMISSION_CODE)
		return level in ['read', 'full']
	}

	/** The settings form shown in Administration > Integrations > Plugins > vme-insight. */
	@Override
	List<OptionType> getSettings() {
		int order = 0
		return [
			new OptionType(
				name: 'Grafana URL', code: 'vme-insight-grafana-url', fieldName: InsightSettings.GRAFANA_URL,
				fieldLabel: 'vme-insight Grafana URL', inputType: OptionType.InputType.TEXT, required: false,
				displayOrder: order++,
				placeHolderText: 'https://vme-insight.example.com',
				helpText: 'Base URL of vme-insight\'s Grafana as your browser reaches it. Use https: browsers block http frames inside VME. Leave blank to hide all vme-insight tabs.'
			),
			new OptionType(
				name: 'Grafana organisation', code: 'vme-insight-grafana-org', fieldName: InsightSettings.ORG_ID,
				fieldLabel: 'Grafana organisation ID', inputType: OptionType.InputType.NUMBER, required: false,
				displayOrder: order++, placeHolderText: '1',
				helpText: 'Optional. Adds orgId= to every URL, for example when anonymous viewers use their own organisation.'
			),
			new OptionType(
				name: 'Theme', code: 'vme-insight-theme', fieldName: InsightSettings.THEME,
				fieldLabel: 'Grafana theme', inputType: OptionType.InputType.TEXT, required: false,
				displayOrder: order++, placeHolderText: 'light or dark',
				helpText: 'Optional: light or dark. Blank uses the Grafana default.'
			),
			new OptionType(
				name: 'Tab height', code: 'vme-insight-tab-height', fieldName: InsightSettings.TAB_HEIGHT,
				fieldLabel: 'Tab height (pixels)', inputType: OptionType.InputType.NUMBER, required: false,
				displayOrder: order++, defaultValue: "${InsightSettings.DEFAULT_TAB_HEIGHT}",
				placeHolderText: "${InsightSettings.DEFAULT_TAB_HEIGHT}",
				helpText: 'Height of the embedded dashboard (300 to 10000). It scrolls inside the tab.'
			),
			new OptionType(
				name: 'Home dashboard panels', code: 'vme-insight-widget-panels', fieldName: InsightSettings.WIDGET_PANELS,
				fieldLabel: 'Home dashboard panel IDs', inputType: OptionType.InputType.TEXT, required: false,
				displayOrder: order++, defaultValue: InsightSettings.DEFAULT_WIDGET_PANELS,
				placeHolderText: InsightSettings.DEFAULT_WIDGET_PANELS,
				helpText: 'Comma-separated panel IDs from the VME Overview dashboard to show in the vme-insight home dashboard.'
			),
			new OptionType(
				name: 'Host type codes', code: 'vme-insight-host-codes', fieldName: InsightSettings.HOST_TYPE_CODES,
				fieldLabel: 'Treat as hosts', inputType: OptionType.InputType.TEXT, required: false,
				displayOrder: order++,
				helpText: 'Optional. Server type codes to always treat as hosts. Match vme-insight\'s classification.host_type_codes.'
			),
			new OptionType(
				name: 'VM type codes', code: 'vme-insight-vm-codes', fieldName: InsightSettings.VM_TYPE_CODES,
				fieldLabel: 'Treat as VMs', inputType: OptionType.InputType.TEXT, required: false,
				displayOrder: order++,
				helpText: 'Optional. Server type codes to always treat as VMs. Match vme-insight\'s classification.vm_type_codes.'
			),
			new OptionType(
				name: 'Require permission', code: 'vme-insight-require-permission', fieldName: InsightSettings.REQUIRE_PERMISSION,
				fieldLabel: 'Only show to roles with the vme-insight permission', inputType: OptionType.InputType.CHECKBOX,
				required: false, displayOrder: order++, defaultValue: 'off',
				helpText: 'When ticked, only users whose role grants "vme-insight: View dashboards" see the tabs, dashboard and link.'
			),
			new OptionType(
				name: 'Show launcher', code: 'vme-insight-show-launcher', fieldName: InsightSettings.SHOW_LAUNCHER,
				fieldLabel: 'Show a vme-insight link on every page', inputType: OptionType.InputType.CHECKBOX,
				required: false, displayOrder: order++, defaultValue: 'off',
				helpText: 'Adds a small "vme-insight" link in the bottom corner of every page that opens Grafana in a new tab.'
			)
		]
	}
}
