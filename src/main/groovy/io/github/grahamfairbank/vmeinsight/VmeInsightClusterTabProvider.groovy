package io.github.grahamfairbank.vmeinsight

import com.morpheusdata.core.AbstractClusterTabProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.Account
import com.morpheusdata.model.ComputeServerGroup
import com.morpheusdata.model.ContentSecurityPolicy
import com.morpheusdata.model.User
import com.morpheusdata.views.HTMLResponse

/**
 * "vme-insight" tab on cluster pages (Infrastructure > Clusters): the VME Overview dashboard
 * filtered to this cluster.
 */
class VmeInsightClusterTabProvider extends AbstractClusterTabProvider {

	VmeInsightPlugin plugin
	MorpheusContext morpheus

	VmeInsightClusterTabProvider(VmeInsightPlugin plugin, MorpheusContext morpheus) {
		this.plugin = plugin
		this.morpheus = morpheus
	}

	@Override Plugin getPlugin() { plugin }
	@Override MorpheusContext getMorpheus() { morpheus }
	@Override String getCode() { 'vme-insight-cluster-tab' }
	@Override String getName() { 'vme-insight' }

	@Override
	Boolean show(ComputeServerGroup cluster, User user, Account account) {
		return cluster?.id != null && VmeInsightPlugin.canView(plugin.loadSettings(), user)
	}

	@Override
	HTMLResponse renderTemplate(ComputeServerGroup cluster) {
		InsightSettings s = plugin.loadSettings()
		if (!s.configured || cluster?.id == null) return HTMLResponse.success('')
		return TabSupport.render(renderer, s, 'VME Overview', s.clusterDashboard(cluster.id), s.clusterDashboard(cluster.id, false))
	}

	@Override
	ContentSecurityPolicy getContentSecurityPolicy() {
		return TabSupport.contentSecurityPolicy(plugin)
	}
}
