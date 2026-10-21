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

## Releasing

1. Update `version` in `gradle.properties` and `CHANGELOG.md`.
2. `tools/build.sh`.
3. Tag `v<version>` and attach `build/libs/vme-insight-plugin-<version>.jar` to the release.
