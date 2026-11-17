package io.github.grahamfairbank.vmeinsight

import com.morpheusdata.core.AbstractGlobalUIComponentProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.model.Account
import com.morpheusdata.model.User
import com.morpheusdata.views.HTMLResponse
import com.morpheusdata.views.ViewModel
import groovy.util.logging.Slf4j

/**
 * Loads vme-insight-theme.js on every page while the theme setting is "Match each user's Morpheus theme".
 *
 * The plugin API doesn't expose a user's Morpheus theme, so the script reads it off the page: it checks how
 * dark the page behind each vme-insight frame is and adds theme=light or theme=dark to the frame. The script
 * is a plugin asset loaded through a global UI component, because inline <script> in tab HTML can be dropped.
 */
@Slf4j
class VmeInsightThemeLoaderProvider extends AbstractGlobalUIComponentProvider {

	VmeInsightPlugin plugin
	MorpheusContext morpheus

	VmeInsightThemeLoaderProvider(VmeInsightPlugin plugin, MorpheusContext morpheus) {
		this.plugin = plugin
		this.morpheus = morpheus
	}

	@Override Plugin getPlugin() { plugin }
	@Override MorpheusContext getMorpheus() { morpheus }
	@Override String getCode() { 'vme-insight-theme-loader' }
	@Override String getName() { 'vme-insight theme script' }

	@Override
	Boolean show(User user, Account account) {
		InsightSettings s = plugin.loadSettings()
		return s.configured && s.matchMorpheusTheme && VmeInsightPlugin.canView(s, user)
	}

	@Override
	HTMLResponse renderTemplate(User user, Account account) {
		try {
			ViewModel<Map> vm = new ViewModel<>()
			vm.object = [version: plugin.version ?: 'dev']
			return renderer.renderTemplate('hbs/theme-loader', vm)
		} catch (Throwable e) {
			log.warn("vme-insight: theme script loader failed: ${e}")
			return HTMLResponse.success('')
		}
	}
}
