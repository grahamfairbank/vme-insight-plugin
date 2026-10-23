# Changelog

## 0.1.3 (2026-10-23)

Documentation release; the plugin's behaviour is unchanged.

* Docs follow vme-insight 0.4.x's certificate guidance: prefer a certificate your browsers already trust, else a self-signed certificate for that host only; the built-in CA is a lab shortcut. Check the fingerprint before importing.
* Troubleshooting for browsing by IP (`ERR_SSL_PROTOCOL_ERROR`, `HTTPS_DEFAULT_SNI`) and for a plugin URL that differs from `GRAFANA_ROOT_URL`.
* [DEVELOPMENT](docs/DEVELOPMENT.md#repositories-versions-and-releases) documents the two repositories, versioning, changelog, tags and GitHub releases shared with vme-insight.
* Tested with vme-insight 0.4.4.

## 0.1.2 (2026-10-21)

* The home dashboard widget uses vme-insight's **VME Summary tiles** dashboard (`vme-widget`, vme-insight 0.2.0+), whose panel ids never change. Default tiles are the same as before, now `1,2,3,4,7,8,9,12`; the old default list is read as the new one.
* Docs: the setup guide moved to vme-insight ([VME-PLUGIN](https://github.com/grahamfairbank/vme-insight/blob/main/docs/VME-PLUGIN.md)), so both projects share one walkthrough. INSTALL here covers building, upgrading and removing. README adds a compatibility table.

## 0.1.1 (2026-10-21)

* Fixed: on VME 9.1.0.1 the tabs showed "This content is blocked" because the page's Content-Security-Policy had no `frame-src` for Grafana. The plugin now falls back to the Grafana origin from its last successful settings read when VME asks for the policy.
* First lab install on VME 9.1.0.1: host, VM, instance and cluster tabs checked.

## 0.1.0 (2026-10-21)

First version, built against Plugin API 1.5.0 (VME 9.1).

* **vme-insight tab** on host and VM pages (VME Hosts / VME Virtual Machines for that server), instance pages (VME Virtual Machines for the instance's VMs) and cluster pages (VME Overview for that cluster), with an **Open in vme-insight** link.
* Host or VM is decided with the same rule as vme-insight's collector, with the same override lists.
* Adds the Grafana origin to VME's Content-Security-Policy `frame-src`. Tabs are hidden while no URL is set; plain-http URLs show a mixed-content warning.
* Optional **vme-insight home dashboard** with a widget of VME Overview tiles (estate counts, alarms, manager status, CPU contention); panel IDs configurable.
* Optional **vme-insight link** on every page (off by default).
* Optional role permission **vme-insight: View dashboards**.
* Docs: install, configuration, authentication options and recommendation, troubleshooting, development.
