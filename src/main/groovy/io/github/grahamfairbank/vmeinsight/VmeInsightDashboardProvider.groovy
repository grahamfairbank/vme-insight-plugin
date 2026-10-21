package io.github.grahamfairbank.vmeinsight

import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.dashboard.AbstractDashboardProvider
import com.morpheusdata.model.ContentSecurityPolicy
import com.morpheusdata.model.Dashboard
import com.morpheusdata.model.DashboardItem
import com.morpheusdata.model.DashboardItemType
import groovy.util.logging.Slf4j

/**
 * A "vme-insight" home dashboard holding the summary widget. Admins add it under
 * Administration > Settings > Appliance > Dashboards to Display.
 */
@Slf4j
class VmeInsightDashboardProvider extends AbstractDashboardProvider {

	VmeInsightPlugin plugin
	MorpheusContext morpheus

	VmeInsightDashboardProvider(VmeInsightPlugin plugin, MorpheusContext morpheus) {
		this.plugin = plugin
		this.morpheus = morpheus
	}

	@Override Plugin getPlugin() { plugin }
	@Override MorpheusContext getMorpheus() { morpheus }
	@Override String getCode() { 'vme-insight-dashboard' }
	@Override String getName() { 'vme-insight' }

	@Override
	Dashboard getDashboard() {
		Dashboard d = new Dashboard()
		d.name = name
		d.code = code
		d.dashboardId = 'vme-insight'
		d.category = 'vme-insight'
		d.title = 'vme-insight'
		d.description = 'Estate and CPU contention summary from vme-insight'
		d.defaultDashboard = false
		d.enabled = true
		d.sourceType = 'system'
		d.templatePath = 'hbs/vme-insight-dashboard'
		// VME's dashboard sync fails on a null scriptPath, so point it at a no-op asset.
		d.scriptPath = 'vme-insight-dashboard.js'
		List<DashboardItem> items = []
		DashboardItemType type = null
		try {
			type = morpheus.dashboard.getDashboardItemType(VmeInsightDashboardItemProvider.CODE).blockingGet()
		} catch (Exception e) {
			log.info("vme-insight: widget type not registered yet (${e.message}); it is added on the next sync")
		}
		if (type) {
			DashboardItem item = new DashboardItem()
			item.type = type
			item.itemRow = 0
			item.itemColumn = 0
			item.itemGroup = 'main'
			item.groupRow = 0
			items << item
		}
		d.dashboardItems = items
		return d
	}

	@Override
	ContentSecurityPolicy getContentSecurityPolicy() {
		return TabSupport.contentSecurityPolicy(plugin) ?: new ContentSecurityPolicy()
	}
}
