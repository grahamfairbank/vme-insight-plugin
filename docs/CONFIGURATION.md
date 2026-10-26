# Configuration

All settings are on **Administration → Integrations → Plugins → vme-insight** (pencil icon). They take effect on the next page load; there's nothing to restart. None of them is a secret.

## Settings

| Setting | Default | What it does |
|---|---|---|
| **vme-insight Grafana URL** | *(blank)* | Base URL of vme-insight's Grafana **as users' browsers reach it**, the same address as vme-insight's `GRAFANA_ROOT_URL` (a name is better than an IP), e.g. `https://vme-insight.example.com` or `https://tools.example.com/grafana`. Must start with `https://` (or `http://`, which browsers block inside VME; the tab then shows a warning). Blank, or anything that isn't a plain http(s) URL, hides every tab, the widget content and the corner link. |
| **Grafana organization ID** | *(blank)* | Adds `orgId=<n>` to every URL. Set it when anonymous viewers use a dedicated organization ([AUTH](AUTH.md#recommended-setup)). |
| **Grafana theme** | *(blank)* | `light` or `dark` forces that theme in the frames. Blank uses Grafana's default (or the viewer's preference). |
| **Tab height (pixels)** | `1400` | Height of the frame in each tab, 300 to 10000. The dashboard scrolls inside it. |
| **Home dashboard panel IDs** | `1,2,3,4,7,8,9,12` | Tiles from vme-insight's **VME Summary tiles** dashboard (`vme-widget`) to show in the home dashboard widget, in order. See [below](#home-dashboard-panel-ids). |
| **Treat as hosts** | *(blank)* | Server type codes that always open the **VME Hosts** dashboard. Copy vme-insight's `classification.host_type_codes`. |
| **Treat as VMs** | *(blank)* | Server type codes that always open **VME Virtual Machines**. Copy vme-insight's `classification.vm_type_codes`. |
| **Only show to roles with the vme-insight permission** | off | When on, only users whose role grants **vme-insight: View dashboards → Read** see the tabs and the corner link. See vme-insight's [VME-PLUGIN](https://github.com/grahamfairbank/vme-insight/blob/main/docs/VME-PLUGIN.md#7-optional-extras). |
| **Show a vme-insight link on every page** | off | Adds a small **vme-insight ↗** link in the bottom-left corner of every VME page that opens VME Overview in a new tab. |

## What each tab opens

The plugin passes VME's own ids to vme-insight's dashboard variables. vme-insight stores VME's ids as its `servers.id` and `clusters.id`, so they match one to one.

| VME page | URL in the frame |
|---|---|
| Host | `<url>/d/vme-hosts?var-host=<server id>&kiosk` |
| VM | `<url>/d/vme-vms?var-vm=<server id>&kiosk` |
| Instance | `<url>/d/vme-vms?var-vm=<id>&var-vm=<id>…&kiosk` (one per VM in the instance) |
| Cluster | `<url>/d/vme-overview?var-cluster=<cluster id>&kiosk` |
| Home dashboard widget | `<url>/d-solo/vme-widget?panelId=<n>` for each panel ID |

`orgId` and `theme` are appended when set. **Open in vme-insight ↗** uses the same URL without `kiosk`, so the time picker and variables are available.

**Host or VM?** A server page is a host when its server type is a hypervisor (`vmHypervisor` or `containerHypervisor`), its type code ends in `host` or contains `hypervisor`, or its node type is `morpheus-node` (HVM hosts in VME 9.1). Everything else is a VM. This is the same rule vme-insight's collector uses, including the two override lists, so a server opens the dashboard where vme-insight put it.

**Instances** without a VM (still provisioning, or not backed by a VME VM) get no tab.

## Home dashboard panel IDs

The widget shows single tiles from vme-insight's **VME Summary tiles** dashboard (`vme-widget`, vme-insight 0.2.0+). Its panel ids are fixed, so they stay the same across vme-insight releases:

| ID | Tile | ID | Tile |
|---|---|---|---|
| **1** | Clusters | **7** | Active alarms |
| **2** | Hosts | **8** | VME manager (version) |
| **3** | VMs | **9** | Worst VM CPU ready |
| **4** | VMs running | 10 | Worst single vCPU ready |
| 5 | VMs stopped | 11 | VMs at 5 to 10% ready |
| 6 | Datastores | **12** | VMs over 10% ready |

Bold ids are the default (`1,2,3,4,7,8,9,12`). List the ids you want, in the order to show them.

Plugin 0.1.0 and 0.1.1 used ids from the VME Overview dashboard instead. A saved value equal to their default (`2,3,4,5,8,9,36,39`) is read as the new default, so upgrades need no change. Any other custom list from those versions must be rewritten with the ids above.

## Matching vme-insight settings

| vme-insight | Needed by the plugin |
|---|---|
| `.env`: `GRAFANA_ALLOW_EMBEDDING=true` | Yes. Otherwise Grafana sends `X-Frame-Options: deny`. |
| Anonymous Viewer, https, network restriction | Yes, see vme-insight's [VME-PLUGIN](https://github.com/grahamfairbank/vme-insight/blob/main/docs/VME-PLUGIN.md). |
| `config.yaml`: `morpheus.url` | Must be the same VME manager the plugin is installed on, or the ids won't match. |
| `config.yaml`: `classification` | Copy any overrides to **Treat as hosts** / **Treat as VMs**. |
