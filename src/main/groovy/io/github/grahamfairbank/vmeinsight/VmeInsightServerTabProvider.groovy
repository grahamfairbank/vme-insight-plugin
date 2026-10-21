package io.github.grahamfairbank.vmeinsight

import com.morpheusdata.core.AbstractServerTabProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.Account
import com.morpheusdata.model.ComputeServer
import com.morpheusdata.model.ComputeServerType
import com.morpheusdata.model.ContentSecurityPolicy
import com.morpheusdata.model.User
import com.morpheusdata.views.HTMLResponse

/**
 * "vme-insight" tab on host and VM pages (Infrastructure > Compute).
 * Hosts get the VME Hosts dashboard, VMs the VME Virtual Machines dashboard, both filtered to this
 * server. VME server ids are the ids vme-insight stores, so they map one to one.
 */
class VmeInsightServerTabProvider extends AbstractServerTabProvider {

	VmeInsightPlugin plugin
	MorpheusContext morpheus

	VmeInsightServerTabProvider(VmeInsightPlugin plugin, MorpheusContext morpheus) {
		this.plugin = plugin
		this.morpheus = morpheus
	}

	@Override Plugin getPlugin() { plugin }
	@Override MorpheusContext getMorpheus() { morpheus }
	@Override String getCode() { 'vme-insight-server-tab' }
	@Override String getName() { 'vme-insight' }

	@Override
	Boolean show(ComputeServer server, User user, Account account) {
		return server?.id != null && VmeInsightPlugin.canView(plugin.loadSettings(), user)
	}

	@Override
	HTMLResponse renderTemplate(ComputeServer server) {
		InsightSettings s = plugin.loadSettings()
		if (!s.configured || server?.id == null) return HTMLResponse.success('')
		if (isHost(s, server)) {
			return TabSupport.render(renderer, s, 'VME Hosts', s.hostDashboard(server.id), s.hostDashboard(server.id, false))
		}
		return TabSupport.render(renderer, s, 'VME Virtual Machines', s.vmDashboard([server.id]), s.vmDashboard([server.id], false))
	}

	@Override
	ContentSecurityPolicy getContentSecurityPolicy() {
		return TabSupport.contentSecurityPolicy(plugin)
	}

	private static boolean isHost(InsightSettings s, ComputeServer server) {
		ComputeServerType t = server.computeServerType
		return s.isHost(t?.code, t?.vmHypervisor, t?.containerHypervisor, t?.nodeType)
	}
}
