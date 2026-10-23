# Development

## Layout

The project follows the Morpheus plugin generator's layout: a Gradle build producing a single "fat jar" with the shadow plugin, Handlebars views, and static assets through the asset pipeline.

```
build.gradle, gradle.properties      versions: Plugin API, Groovy, Gradle plugins
tools/build.sh                       build in a JDK 17 container (no local Java needed)
src/main/groovy/io/github/grahamfairbank/vmeinsight/
  VmeInsightPlugin.groovy            entry point (Plugin-Class): settings form, permission, providers
  InsightSettings.groovy             parses settings, builds every Grafana URL (no VME classes; unit tested)
  TabSupport.groovy                  shared CSP and tab rendering
  VmeInsightServerTabProvider        host and VM pages
  VmeInsightInstanceTabProvider      instance pages
  VmeInsightClusterTabProvider       cluster pages
  VmeInsightDashboardItemProvider    home dashboard widget (DashboardItemTypeProvider)
  VmeInsightDashboardProvider        the "vme-insight" dashboard that holds the widget
  VmeInsightLauncherProvider         optional link on every page (GlobalUIComponentProvider)
src/main/resources/renderer/hbs/     Handlebars templates
src/assets/                          logo and two no-op scripts VME requires
src/test/groovy/                     Spock specs: URLs, settings parsing, template rendering
```

## Build and test

```bash
tools/build.sh                 # clean test shadowJar → build/libs/vme-insight-plugin-<version>.jar
tools/build.sh test            # tests only
./gradlew test shadowJar       # the same, with JDK 11+ installed
```

`tools/build.sh` keeps Gradle's cache in a Docker volume (`vme-insight-plugin-gradle`). Remove it with `docker volume rm vme-insight-plugin-gradle`.

Classes target Java 11, matching `morpheus-plugin-api` 1.5.0. Everything VME already provides (the plugin API, Groovy, RxJava, SLF4J) is `provided` and not bundled, so the jar holds only this plugin's classes, templates and assets.

## Versions

| Setting (`gradle.properties`) | Value | Why |
|---|---|---|
| `morphPluginApiVersion` | `1.5.0` | VME 9.1's Plugin API ([compatibility](https://github.com/HewlettPackard/morpheus-docs/blob/dev-9.1/release_notes/compatibility.rst)) |
| `groovyVersion` | `3.0.9` | What plugin-api 1.5.0 declares |
| Gradle | 8.14.4 (wrapper) | Same as morpheus-plugin-core |
| `shadowVersion` | `8.1.1` | Same as morpheus-plugin-core |
| `assetPipelineVersion` | `4.4.0` | Same as morpheus-plugin-core |

For a later VME release, check its Plugin API version in that release's notes, set `morphPluginApiVersion` and `Morpheus-Min-Appliance-Version` (in `build.gradle`) to match, rebuild and retest.

## The VME APIs used

From `morpheus-plugin-api` 1.5.0 ([source](https://github.com/HewlettPackard/morpheus-plugin-core/tree/master/morpheus-plugin-api)):

| Interface / base class | Used for | Notes |
|---|---|---|
| `Plugin` | Settings form (`getSettings()`), role permission (`setPermissions`), registering providers | Settings come back as JSON from `morpheus.getSettings(plugin)`, keyed by each option's `fieldName`. Read on every render, so changes apply immediately. |
| `AbstractServerTabProvider` | Host and VM pages | One provider covers both; the server type decides the dashboard. |
| `AbstractInstanceTabProvider` | Instance pages | VM ids from `instance.containers[].server.id`. |
| `AbstractClusterTabProvider` | Cluster pages | `ComputeServerGroup.id` = vme-insight's `clusters.id`. |
| `UIExtensionProvider.getContentSecurityPolicy()` | `frameSrc` = Grafana origin | VME 9.1's policy has no `frame-src` of its own, so frames fall back to `default-src 'self'` without this. |
| `AbstractDashboardItemTypeProvider` | Home dashboard widget | `renderDashboardItem` is overridden to pass the panel URLs to the template. |
| `AbstractDashboardProvider` | The dashboard holding the widget | VME only shows widgets that belong to a dashboard, and only dashboards selected in **Dashboards to Display**. Its `scriptPath` must not be null: HPE's VME Prometheus plugin notes that VME's dashboard sync fails otherwise, hence the no-op script. |
| `AbstractGlobalUIComponentProvider` | Optional corner link | Rendered into every page's layout; kept to a plain link. |

Templates are rendered with the plugin API's `HandlebarsRenderer`, which HTML-escapes values (including `=` as `&#x3D;`, which browsers decode in attributes). `TemplateSpec` renders each template the same way.

## What still needs a real VME

Unit tests cover URL building, settings parsing, host/VM classification, the permission rule and template output. These need a lab manager:

* the tabs appear on host, VM, instance and cluster pages, and `show()` hides them without a URL;
* VME applies `frameSrc` and the frame loads;
* the dashboard shows in **Dashboards to Display** and the widget renders inside it;
* settings field types (number, checkbox) save and read back as expected;
* the corner link's placement in VME 9.1's layout.

## Repositories, versions and releases

vme-insight and this plugin follow the same conventions.

### Two repositories

| Repository | What it is | Depends on |
|---|---|---|
| [grahamfairbank/vme-insight](https://github.com/grahamfairbank/vme-insight) | The platform: collector, TimescaleDB, Prometheus, Grafana, dashboards. Complete on its own. | VME |
| [grahamfairbank/vme-insight-plugin](https://github.com/grahamfairbank/vme-insight-plugin) | This plugin: an optional add-on installed in the VME manager. | vme-insight and VME |

They're versioned and released independently: they install into different systems (a Docker host and the VME manager), and many vme-insight users never install the plugin. What ties them together:

* **One setup guide**, in vme-insight: [VME-PLUGIN](https://github.com/grahamfairbank/vme-insight/blob/main/docs/VME-PLUGIN.md). This repository's docs link to it rather than repeat it.
* **Compatibility tables** in both READMEs, updated with every release of either project.
* **Contracts** the plugin relies on, which vme-insight keeps stable: dashboard uids and variables (`vme-hosts`/`var-host`, `vme-vms`/`var-vm`, `vme-overview`/`var-cluster`) and the `vme-widget` panel ids, which are never renumbered or reused. Changing one is a breaking change for the plugin and needs a compatibility row saying so.

### Versions

`MAJOR.MINOR.PATCH`. While the major version is 0: a new feature or setting bumps MINOR, fixes and documentation-only releases bump PATCH. The version lives in `gradle.properties` (`version=`) and becomes the jar name and the plugin version VME shows.

### Changelog

`CHANGELOG.md`, newest first. Each release has a heading `## <version> (<YYYY-MM-DD>)`, with bullets for what changed from a user's point of view. Note anything that needs a newer vme-insight or VME, or a settings change.

### Releasing

1. **Release commit.** Set `version` in `gradle.properties`, date the changelog heading, and add a row to the README's **Compatibility** table (and **Tested versions**, if you tested). Commit as `Release <version>: <summary>`.
2. **Build and test.** `tools/build.sh` must pass. The jar is `build/libs/vme-insight-plugin-<version>.jar`.
3. **Annotated tag** on the release commit, named `v<version>`, with a one-line summary:
   ```bash
   git tag -a v0.1.3 -m "vme-insight-plugin 0.1.3: <summary>"
   git push origin main v0.1.3
   ```
4. **GitHub release** for the tag, titled `vme-insight-plugin v<version>`, with the jar attached. The notes say what changed, what it requires (VME and vme-insight versions), how to install, the jar's SHA-256, and the community-project disclaimer:
   ```bash
   sha256sum build/libs/vme-insight-plugin-0.1.3.jar
   gh release create v0.1.3 build/libs/vme-insight-plugin-0.1.3.jar \
     --title "vme-insight-plugin v0.1.3" --notes-file release-notes.md
   ```
5. **vme-insight's compatibility table.** If the release changes what the plugin needs, add or update the row in vme-insight's README in the same week.

vme-insight releases the same way (release commit, dated changelog, compatibility row, annotated `v<version>` tag, GitHub release with notes), without a jar.
