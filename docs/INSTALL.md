# Install

This takes about 15 minutes once vme-insight is running. Steps 1 to 4 give you the tabs. Step 5 (home dashboard) and step 6 (role permission) are optional.

Built against: VME 9.1 Plugin API 1.5.0, vme-insight 0.1, Grafana 11.5.2. See [Tested versions](../README.md#tested-versions).

## Prerequisites

| | Why |
|---|---|
| VME 9.1 or later | Plugin API 1.5.0. Older managers refuse the plugin (`Morpheus-Min-Appliance-Version: 9.1.0`). |
| [vme-insight](https://github.com/grahamfairbank/vme-insight) 0.1 or later, collecting from the **same** VME manager | The tabs pass VME's object ids to vme-insight's dashboards, so both must describe the same estate. |
| vme-insight's Grafana reachable from your users' browsers over **https**, embeddable, with anonymous Viewer access | See [AUTH](AUTH.md#recommended-setup). Without these the tabs stay blank. |
| A VME user with **Administration: Integrations** rights | To upload and configure plugins. |
| Docker (or JDK 11+) on any machine | To build the jar, unless you download a release. |

## 1. Prepare vme-insight's Grafana

Follow [AUTH → Recommended setup](AUTH.md#recommended-setup): allow embedding, enable anonymous Viewer access, put https in front, and restrict the networks that can reach it.

Check from your workstation, in a private browser window: `https://<grafana>/d/vme-overview` should show the VME Overview dashboard **without** a sign-in page.

## 2. Build the plugin

```bash
git clone https://github.com/grahamfairbank/vme-insight-plugin
cd vme-insight-plugin
tools/build.sh
```

`tools/build.sh` runs the Gradle wrapper in a JDK 17 container, so you don't need Java installed. It runs the unit tests and writes `build/libs/vme-insight-plugin-0.1.0.jar` (about 60 KB). With JDK 11 or later installed, `./gradlew test shadowJar` does the same.

## 3. Upload it to VME

HPE describes plugin management in [Plugins](https://github.com/HewlettPackard/morpheus-docs/blob/dev-9.1/administration/integrations/plugins.rst). In short:

1. In VME, go to **Administration → Integrations → Plugins**.
2. Click **Add** (or **Upload**), choose `vme-insight-plugin-0.1.0.jar`, and upload it.
3. The plugin appears in the list as **vme-insight**, version 0.1.0, status **loaded**, enabled.

## 4. Configure it

1. Click the pencil icon on the **vme-insight** row.
2. Set **vme-insight Grafana URL** to the https address from step 1, for example `https://vme-insight.example.com`. No trailing path is needed unless Grafana is served under one.
3. Save.

Every setting is described in [CONFIGURATION](CONFIGURATION.md). The defaults suit vme-insight 0.1.

### Verify

1. Open **Infrastructure → Clusters** and pick a cluster. A **vme-insight** tab appears beside VME's own tabs, showing the **VME Overview** dashboard for that cluster.
2. Open a host under **Infrastructure → Compute → Hosts**. The tab shows **VME Hosts** with that host selected.
3. Open a VM under **Infrastructure → Compute → Virtual Machines**, or an instance under **Provisioning → Instances**. The tab shows **VME Virtual Machines** for it.
4. **Open in vme-insight ↗** at the top of each tab opens the same dashboard full size.

A tab that's blank, shows a sign-in page, or shows "refused to connect": see [TROUBLESHOOTING](TROUBLESHOOTING.md).

## 5. Optional: the home dashboard

The plugin adds a **vme-insight** dashboard with one widget: a row of tiles from VME Overview (by default clusters, hosts, VMs, VMs running, active alarms, VME manager status, worst VM CPU ready, and VMs over 10% ready).

VME shows the dashboards an administrator selects:

1. Go to **Administration → Settings → Appliance** ([Appliance settings](https://github.com/HewlettPackard/morpheus-docs/blob/dev-9.1/administration/settings/appliance.rst)).
2. In **Dashboards to Display**, add **vme-insight** alongside the standard dashboard, and drag it to where you want it.
3. Save, and open the home page.

The widget follows VME's **Infrastructure: Clusters** permission: users whose role can read clusters see it. Pick other tiles with the **Home dashboard panel IDs** setting ([CONFIGURATION](CONFIGURATION.md#home-dashboard-panel-ids)).

## 6. Optional: limit who sees vme-insight in VME

The plugin defines a role permission, **vme-insight: View dashboards** (None / Read). It applies only when you tick **Only show to roles with the vme-insight permission** in the plugin settings.

1. Tick that setting.
2. In **Administration → Roles**, edit each role that should see vme-insight and set the permission to **Read** ([Role permissions](https://github.com/HewlettPackard/morpheus-docs/blob/dev-9.1/administration/roles/role_permissions.rst)).

This hides the tabs and the corner link in VME. It does **not** protect Grafana itself; anyone on an allowed network can still open the Grafana URL directly. See [AUTH](AUTH.md#recommendation-a-anonymous-viewer-restricted-by-network).

## Upgrading

Build or download the new jar and upload it on **Administration → Integrations → Plugins** the same way. VME replaces the older version and keeps the settings. Check [CHANGELOG](../CHANGELOG.md) for setting changes.

## Removing

On **Administration → Integrations → Plugins**, click the trash icon on the **vme-insight** row. The tabs, the dashboard and the role permission disappear. If you added the vme-insight dashboard to **Dashboards to Display**, check that list afterwards.

To undo the Grafana side, remove the settings you added in [AUTH](AUTH.md#recommended-setup) and restart Grafana.
