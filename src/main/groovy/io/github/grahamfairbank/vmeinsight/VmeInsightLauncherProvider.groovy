package io.github.grahamfairbank.vmeinsight

import com.morpheusdata.core.AbstractGlobalUIComponentProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.Account
import com.morpheusdata.model.User
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel

/**
 * Optional (off by default): a small "vme-insight" link in the corner of every VME page that
 * opens Grafana in a new tab. No script, no frame: just a link.
 */
class VmeInsightLauncherProvider extends AbstractGlobalUIComponentProvider {

	VmeInsightPlugin plugin
	MorpheusContext morpheus

	VmeInsightLauncherProvider(VmeInsightPlugin plugin, MorpheusContext morpheus) {
		this.plugin = plugin
		this.morpheus = morpheus
	}

	@Override Plugin getPlugin() { plugin }
	@Override MorpheusContext getMorpheus() { morpheus }
	@Override String getCode() { 'vme-insight-launcher' }
	@Override String getName() { 'vme-insight link' }

	@Override
	Boolean show(User user, Account account) {
		InsightSettings s = plugin.loadSettings()
		return s.showLauncher && VmeInsightPlugin.canView(s, user)
	}

	@Override
	HTMLResponse renderTemplate(User user, Account account) {
		InsightSettings s = plugin.loadSettings()
		if (!s.configured) return HTMLResponse.success('')
		ViewModel<Map> model = new ViewModel<>()
		model.object = [openUrl: s.overviewDashboard()]
		return renderer.renderTemplate('hbs/launcher', model)
	}
}
