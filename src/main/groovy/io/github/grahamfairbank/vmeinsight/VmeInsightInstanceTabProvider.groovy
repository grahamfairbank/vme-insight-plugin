package io.github.grahamfairbank.vmeinsight

import com.morpheusdata.core.AbstractInstanceTabProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.Account
import com.morpheusdata.model.ContentSecurityPolicy
import com.morpheusdata.model.Instance
import com.morpheusdata.model.User
import com.morpheusdata.views.HTMLResponse

/**
 * "vme-insight" tab on instance pages (Provisioning > Instances): the VME Virtual Machines
 * dashboard filtered to the instance's VMs. Instances without a VM (none yet, or not VME-backed)
 * get no tab.
 */
class VmeInsightInstanceTabProvider extends AbstractInstanceTabProvider {

	VmeInsightPlugin plugin
	MorpheusContext morpheus

	VmeInsightInstanceTabProvider(VmeInsightPlugin plugin, MorpheusContext morpheus) {
		this.plugin = plugin
		this.morpheus = morpheus
	}

	@Override Plugin getPlugin() { plugin }
	@Override MorpheusContext getMorpheus() { morpheus }
	@Override String getCode() { 'vme-insight-instance-tab' }
	@Override String getName() { 'vme-insight' }

	@Override
	Boolean show(Instance instance, User user, Account account) {
		return serverIds(instance) && VmeInsightPlugin.canView(plugin.loadSettings(), user)
	}

	@Override
	HTMLResponse renderTemplate(Instance instance) {
		InsightSettings s = plugin.loadSettings()
		List<Long> ids = serverIds(instance)
		if (!s.configured || !ids) return HTMLResponse.success('')
		return TabSupport.render(renderer, s, 'VME Virtual Machines', s.vmDashboard(ids), s.vmDashboard(ids, false))
	}

	@Override
	ContentSecurityPolicy getContentSecurityPolicy() {
		return TabSupport.contentSecurityPolicy(plugin)
	}

	/** Ids of the VMs behind this instance (one per node for multi-node layouts). */
	static List<Long> serverIds(Instance instance) {
		return (instance?.containers ?: []).collect { it?.server?.id }.findAll { it != null }.unique() as List<Long>
	}
}
