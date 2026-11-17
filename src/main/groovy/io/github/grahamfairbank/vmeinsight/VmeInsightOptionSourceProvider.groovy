package io.github.grahamfairbank.vmeinsight

import com.morpheusdata.core.AbstractOptionSourceProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin

/**
 * Choice lists for the plugin's dropdown settings.
 *
 * A Morpheus SELECT option type can't list its choices inline: it names an option source, and for
 * optionSourceType 'plugin' Morpheus calls the method of that name here. Each method returns
 * [name, value] maps.
 */
class VmeInsightOptionSourceProvider extends AbstractOptionSourceProvider {

	static final String THEMES = 'vmeInsightThemes'

	/** The stored values match what InsightSettings accepts; 'default' (or blank) adds no theme, 'morpheus' leaves it to the browser script. */
	static final List<Map> THEME_CHOICES = [
		[name: 'Match each user\'s Morpheus theme', value: InsightSettings.THEME_MORPHEUS],
		[name: 'Grafana default', value: InsightSettings.THEME_DEFAULT],
		[name: 'Light', value: 'light'],
		[name: 'Dark', value: 'dark'],
	].asImmutable()

	final Plugin plugin
	final MorpheusContext morpheus

	VmeInsightOptionSourceProvider(Plugin plugin, MorpheusContext morpheus) {
		this.plugin = plugin
		this.morpheus = morpheus
	}

	@Override
	String getCode() {
		return 'vme-insight-option-source'
	}

	@Override
	String getName() {
		return 'vme-insight option source'
	}

	@Override
	List<String> getMethodNames() {
		return [THEMES]
	}

	List<Map> vmeInsightThemes(args) {
		return THEME_CHOICES
	}
}
