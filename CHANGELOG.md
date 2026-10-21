# Changelog

## 0.1.1

* Fixed: on VME 9.1.0.1 the tabs showed "This content is blocked" because the page's Content-Security-Policy had no `frame-src` for Grafana. The plugin now falls back to the Grafana origin from its last successful settings read when VME asks for the policy.
* First lab install on VME 9.1.0.1: host, VM, instance and cluster tabs checked.

## 0.1.0

First version, built against Plugin API 1.5.0 (VME 9.1).

* **vme-insight tab** on host and VM pages (VME Hosts / VME Virtual Machines for that server), instance pages (VME Virtual Machines for the instance's VMs) and cluster pages (VME Overview for that cluster), with an **Open in vme-insight** link.
* Host or VM is decided with the same rule as vme-insight's collector, with the same override lists.
* Adds the Grafana origin to VME's Content-Security-Policy `frame-src`. Tabs are hidden while no URL is set; plain-http URLs show a mixed-content warning.
* Optional **vme-insight home dashboard** with a widget of VME Overview tiles (estate counts, alarms, manager status, CPU contention); panel IDs configurable.
* Optional **vme-insight link** on every page (off by default).
* Optional role permission **vme-insight: View dashboards**.
* Docs: install, configuration, authentication options and recommendation, troubleshooting, development.
