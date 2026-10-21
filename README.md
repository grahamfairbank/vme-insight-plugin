# vme-insight-plugin

A small **HPE Morpheus VM Essentials (VME)** UI plugin that shows [vme-insight](https://github.com/grahamfairbank/vme-insight)'s Grafana dashboards inside VME: a **vme-insight** tab on every host, VM, instance and cluster page, and an optional vme-insight home dashboard.

vme-insight stays a standalone web site. This plugin is a thin companion: it holds no credentials, stores no data and makes no API calls. It builds the right dashboard URL for the page you're on and shows it in a frame.

**Supported:** VME 9.1 and later (Plugin API 1.5.0), with vme-insight 0.1 or later.

> **Community project.** vme-insight-plugin is independent and community maintained. It is not affiliated with, endorsed, sponsored or supported by Hewlett Packard Enterprise, Morpheus Data, or any related organisation. See the [disclaimer](#disclaimer).

## What you get

| Where in VME | Tab or item | Shows |
|---|---|---|
| **Infrastructure → Compute → Hosts → *host*** | vme-insight tab | **VME Hosts** dashboard for this host: CPU, CPU ready, memory, swap, storage, network, running VMs |
| **Infrastructure → Compute → Virtual Machines → *VM*** | vme-insight tab | **VME Virtual Machines** dashboard for this VM: CPU, CPU ready, memory, disk, IOPS, network, power history |
| **Provisioning → Instances → *instance*** | vme-insight tab | **VME Virtual Machines** for the instance's VMs (all nodes of a multi-node instance) |
| **Infrastructure → Clusters → *cluster*** | vme-insight tab | **VME Overview** for this cluster: capacity, N+1 headroom, hosts, datastores, CPU contention, top VMs |
| **Home dashboard** *(optional)* | vme-insight widget | A row of tiles from VME Overview: estate counts, alarms, manager status, CPU contention |
| **Every page** *(optional, off by default)* | vme-insight link | A small link in the corner that opens vme-insight in a new tab |

Each tab also has an **Open in vme-insight ↗** link that opens the same dashboard full size, with the time picker and drill-downs.

## How it works

```
 Browser ── VME page (https://vme-manager) ──────────────────────────────┐
   │          └─ vme-insight tab: <iframe src="https://grafana/d/vme-hosts?var-host=42&kiosk">
   │                                                                     │
   └── loads the frame directly from Grafana ──▶ vme-insight Grafana ◀───┘
                                                 (anonymous Viewer, https,
                                                  reachable only from your networks)
```

* VME calls the plugin when it renders a host, VM, instance or cluster page. The plugin returns a frame pointing at the matching vme-insight dashboard, using VME's own object id. vme-insight stores VME's ids, so `var-host=42` is VME server 42.
* The plugin adds the Grafana address to VME's **Content-Security-Policy** `frame-src`, which VME otherwise restricts to itself.
* The **browser** loads the dashboard from Grafana. VME's manager never talks to Grafana, and Grafana never talks to VME through the plugin.
* Grafana has to allow embedding and let the browser in without a separate sign-in. [AUTH](docs/AUTH.md) compares the ways to do that and recommends one.

## Quick start

1. In vme-insight, make Grafana embeddable and reachable over **https** with anonymous Viewer access, restricted to your admin networks. Details and exact settings: [AUTH](docs/AUTH.md#recommended-setup).
2. Build the plugin (needs only Docker) or download a release jar:
   ```bash
   git clone https://github.com/grahamfairbank/vme-insight-plugin && cd vme-insight-plugin
   tools/build.sh                      # → build/libs/vme-insight-plugin-0.1.0.jar
   ```
3. In VME, go to **Administration → Integrations → Plugins**, click **Add**, and upload the jar.
4. Edit the plugin (pencil icon) and set **vme-insight Grafana URL**, for example `https://vme-insight.example.com`.
5. Open any host, VM or cluster. The **vme-insight** tab appears next to VME's own tabs.

Full walkthrough: **[docs/INSTALL.md](docs/INSTALL.md)**.

## Documentation

| | |
|---|---|
| [INSTALL](docs/INSTALL.md) | Prerequisites, building, uploading, first configuration, the home dashboard, upgrades, removal |
| [CONFIGURATION](docs/CONFIGURATION.md) | Every plugin setting, the URLs it builds, matching vme-insight settings |
| [AUTH](docs/AUTH.md) | How the browser gets into Grafana: options compared, recommended setup, security notes |
| [TROUBLESHOOTING](docs/TROUBLESHOOTING.md) | Blank tabs, sign-in loops, wrong dashboards, missing tabs |
| [DEVELOPMENT](docs/DEVELOPMENT.md) | Project layout, building and testing, the VME plugin APIs used |

## Requirements

* **VME 9.1 or later.** 9.1 ships Plugin API 1.5.0, which this plugin is built against ([9.1 compatibility](https://github.com/HewlettPackard/morpheus-docs/blob/dev-9.1/release_notes/compatibility.rst)).
* **vme-insight 0.1 or later**, with its Grafana reachable from your users' browsers over **https**. Browsers block `http://` frames inside VME's `https://` pages.
* A VME role that can manage plugins (Administration → Integrations) to install it.
* To build: Docker, or JDK 11+ with the included Gradle wrapper.

## Tested versions

| Component | Version | Status |
|---|---|---|
| VME manager | 9.1.0.1 | Pending: built against its Plugin API 1.5.0; install and lab test not yet done |
| Plugin API (`morpheus-plugin-api`) | 1.5.0 | Builds; unit tests pass |
| vme-insight | 0.1 | Dashboard uids and variables (`vme-hosts`/`var-host`, `vme-vms`/`var-vm`, `vme-overview`/`var-cluster`) checked against its dashboards |
| Grafana | 11.5.2 (as shipped by vme-insight) | Kiosk and single-panel URLs as documented by Grafana |
| Browsers | | Pending lab test |

This table will be updated after the first lab install.

## VME reference documentation

HPE's documentation for Morpheus and VM Essentials is published at [HewlettPackard/morpheus-docs](https://github.com/HewlettPackard/morpheus-docs). This plugin relies on these pages:

| Topic | HPE page |
|---|---|
| Installing, enabling and removing plugins | [Plugins](https://github.com/HewlettPackard/morpheus-docs/blob/dev-9.1/administration/integrations/plugins.rst) |
| Plugin API version per release (1.5.0 in 9.1) | [Compatibility](https://github.com/HewlettPackard/morpheus-docs/blob/dev-9.1/release_notes/compatibility.rst), [9.1.0 release notes](https://github.com/HewlettPackard/morpheus-docs/blob/dev-9.1/release_notes/9_1_0.rst) |
| Custom tabs, `show()` and the Content-Security-Policy | [Getting started with custom instance tabs](https://github.com/HewlettPackard/morpheus-docs/blob/dev-9.1/getting_started/guides/instance_tabs.rst) |
| Choosing home dashboards | [Appliance settings → Dashboards to Display](https://github.com/HewlettPackard/morpheus-docs/blob/dev-9.1/administration/settings/appliance.rst) |
| Role permissions | [Role permissions](https://github.com/HewlettPackard/morpheus-docs/blob/dev-9.1/administration/roles/role_permissions.rst) |

The plugin APIs themselves are documented in [morpheus-plugin-core](https://github.com/HewlettPackard/morpheus-plugin-core) and the [Morpheus Developer Portal](https://developer.morpheusdata.com). Links point at the `dev-9.1` branch, matching the supported release. Where these pages and this plugin disagree, HPE's documentation describes VME's behaviour.

## Disclaimer

vme-insight-plugin is an independent, community-maintained open-source project.

* It is **not affiliated with, endorsed, sponsored or supported by** Hewlett Packard Enterprise (HPE), Morpheus Data, or any of their subsidiaries or affiliates, nor by Grafana Labs or any other organisation whose products it works with.
* HPE, Morpheus, VM Essentials, HVM and other product names are trademarks of their respective owners. They are used here only to describe what the software works with.
* It is **not an HPE product and not covered by HPE support**. Please don't raise HPE support cases about this plugin; use this repository's issues instead.
* It runs inside your VME manager. Custom plugins run with the manager's privileges, so review the source (it is short) and check your HPE support terms before installing third-party plugins on a production manager.
* It is provided "as is", without warranty of any kind. See [LICENSE](LICENSE).

## Credits

The home dashboard approach follows HPE's open-source [Morpheus home dashboard](https://github.com/HewlettPackard/morpheus-dashboards) and [VME Prometheus dashboard](https://github.com/HewlettPackard/morpheus-vme-prometheus-plugin) plugins (Apache 2.0 and MIT). The code here is an independent implementation.

## License

MIT. See [LICENSE](LICENSE).
