# Install

**The step-by-step setup is in vme-insight's guide: [VME-PLUGIN](https://github.com/grahamfairbank/vme-insight/blob/main/docs/VME-PLUGIN.md).** It covers both sides in order: vme-insight's https front end and anonymous viewing, trusting the certificate on your users' machines, then uploading and configuring this plugin in VME. vme-insight is the main project; this plugin is an optional add-on that depends on it.

This page covers what is specific to the plugin: building it, upgrading and removing it.

## Requirements

| | |
|---|---|
| VME | 9.1 or later (Plugin API 1.5.0). Older managers refuse the plugin (`Morpheus-Min-Appliance-Version: 9.1.0`). |
| vme-insight | 0.2.0 or later, set up as in [VME-PLUGIN](https://github.com/grahamfairbank/vme-insight/blob/main/docs/VME-PLUGIN.md), collecting from the same VME manager. See [Compatibility](../README.md#compatibility). |
| To install | A VME user with **Administration: Integrations** rights |
| To build | Docker, or JDK 11+ (the Gradle wrapper is included) |

## Build from source

```bash
git clone https://github.com/grahamfairbank/vme-insight-plugin.git
cd vme-insight-plugin
tools/build.sh
```

`tools/build.sh` runs the Gradle wrapper in a JDK 17 container, so you don't need Java installed. It runs the unit tests and writes `build/libs/vme-insight-plugin-<version>.jar` (about 60 KB). With JDK 11 or later installed, `./gradlew test shadowJar` does the same. More in [DEVELOPMENT](DEVELOPMENT.md).

## Install and configure

Follow steps 5 to 7 of [VME-PLUGIN](https://github.com/grahamfairbank/vme-insight/blob/main/docs/VME-PLUGIN.md#5-install-the-plugin). In short: **Administration → Integrations → Plugins → Add**, upload the jar, then edit the plugin and set **vme-insight Grafana URL**. HPE describes plugin management in [Plugins](https://github.com/HewlettPackard/morpheus-docs/blob/dev-9.1/administration/integrations/plugins.rst). Every setting is in [CONFIGURATION](CONFIGURATION.md).

## Upgrading

Check [Compatibility](../README.md#compatibility) and [CHANGELOG](../CHANGELOG.md) first; upgrade vme-insight before the plugin when a release asks for a newer one. Then upload the new jar on **Administration → Integrations → Plugins** the same way. VME replaces the older version and keeps the settings.

## Removing

On **Administration → Integrations → Plugins**, click the trash icon on the **vme-insight** row. The tabs, the dashboard and the role permission disappear. If you added the vme-insight dashboard to **Dashboards to Display**, check that list afterwards. To undo the vme-insight side, see [VME-PLUGIN → Removing](https://github.com/grahamfairbank/vme-insight/blob/main/docs/VME-PLUGIN.md#removing).
