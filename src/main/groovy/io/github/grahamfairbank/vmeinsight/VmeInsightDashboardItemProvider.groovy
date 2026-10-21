package io.github.grahamfairbank.vmeinsight

import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.dashboard.AbstractDashboardItemTypeProvider
import com.morpheusdata.model.ContentSecurityPolicy
import com.morpheusdata.model.DashboardItem
import com.morpheusdata.model.DashboardItemType
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel
import groovy.util.logging.Slf4j

/**
 * Home dashboard widget: a row of single panels (Grafana /d-solo) from vme-insight's
 * VME Overview dashboard, by default the estate counts and CPU contention tiles.
 *
 * VME only shows widgets that belong to a Dashboard, so VmeInsightDashboardProvider wraps this one.
 */
@Slf4j
class VmeInsightDashboardItemProvider extends AbstractDashboardItemTypeProvider {

	static final String CODE = 'dashboard-item-vme-insight-summary'

	VmeInsightPlugin plugin
	MorpheusContext morpheus

	VmeInsightDashboardItemProvider(VmeInsightPlugin plugin, MorpheusContext morpheus) {
		this.plugin = plugin
		this.morpheus = morpheus
	}

	@Override Plugin getPlugin() { plugin }
	@Override MorpheusContext getMorpheus() { morpheus }
	@Override String getCode() { CODE }
	@Override String getName() { 'vme-insight summary' }

	@Override
	DashboardItemType getDashboardItemType() {
		DashboardItemType t = new DashboardItemType()
		t.name = name
		t.code = code
		t.category = 'vme-insight'
		t.title = 'vme-insight'
		t.description = 'Estate and CPU contention tiles from vme-insight'
		t.uiSize = 'full'
		t.templatePath = 'hbs/summary-widget'
		t.scriptPath = 'vme-insight-widget.js'
		// Same permission anchor HPE's own cluster widgets use: anyone who can read clusters.
		try {
			t.permission = morpheus.permission.getByCode('infrastructure-cluster').blockingGet()
			t.accessTypes = ['read', 'full']
		} catch (Exception e) {
			log.warn("vme-insight: cluster permission not found, widget visible to all dashboard users: ${e.message}")
		}
		return t
	}

	@Override
	HTMLResponse renderDashboardItem(DashboardItem dashboardItem, Map<String, Object> opts) {
		InsightSettings s = plugin.loadSettings()
		ViewModel<Map> model = new ViewModel<>()
		model.object = [
			configured: s.configured,
			insecure  : s.insecureUrl,
			panels    : s.widgetPanelUrls(),
			openUrl   : s.overviewDashboard()
		]
		model.opts = opts
		return renderer.renderTemplate('hbs/summary-widget', model)
	}

	@Override
	ContentSecurityPolicy getContentSecurityPolicy() {
		return TabSupport.contentSecurityPolicy(plugin) ?: new ContentSecurityPolicy()
	}
}
