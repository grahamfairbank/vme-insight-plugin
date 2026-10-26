# Troubleshooting

Most problems are between the browser and Grafana, not in VME. Start with the browser's developer tools (F12): the **Console** shows Content-Security-Policy and mixed-content errors, and the **Network** tab shows what Grafana answered for the frame.

## No vme-insight tab

| Cause | Fix |
|---|---|
| **vme-insight Grafana URL** is blank or not a plain http(s) URL | Set it ([CONFIGURATION](CONFIGURATION.md)). URLs with a query string (`?`), fragment (`#`) or user name are rejected. |
| **Only show to roles with the vme-insight permission** is on and your role lacks it | Grant **vme-insight: View dashboards → Read** to the role, or untick the setting. |
| It's there but out of sight | Pages with many tabs (hosts) put extra tabs in the **▼** menu at the end of the tab row. |
| Instance page with no VM yet, or not a VME VM | Expected. Open the VM under Infrastructure → Compute instead. |
| Plugin not loaded | **Administration → Integrations → Plugins**: status should be **loaded** and enabled. Otherwise see [Plugin won't load](#plugin-wont-load). |

## Tab is blank, or says it refused to connect

| Console or Network shows | Cause | Fix |
|---|---|---|
| `Mixed Content: ... requested an insecure frame 'http://...'` | Grafana URL is http, VME is https | Serve Grafana over https and update the URL ([VME-PLUGIN](https://github.com/grahamfairbank/vme-insight/blob/main/docs/VME-PLUGIN.md#2-turn-on-https-and-anonymous-viewing)). The tab also shows a warning. |
| `Refused to display ... in a frame because it set 'X-Frame-Options' to 'deny'` | Grafana embedding is off | vme-insight `.env`: `GRAFANA_ALLOW_EMBEDDING=true`, then `docker compose up -d grafana`. |
| `Refused to frame ... because it violates ... default-src` or `frame-src` (Chrome and Edge show "This content is blocked. Contact the site owner") | VME's page policy doesn't list the Grafana origin | Use plugin 0.1.1 or later: 0.1.0 could return no policy where VME asks for it. Check the URL setting is saved, then reload the page. If it persists after changing the URL, disable and re-enable the plugin. |
| `net::ERR_CERT_AUTHORITY_INVALID`, or Edge's "might be having issues" page in the tab; vme-insight's https log (with `HTTPS_LOG_LEVEL=DEBUG`) shows `tls: unknown certificate` | Browser doesn't trust Grafana's certificate; frames can't show the warning. On Windows the usual cause is a certificate installed by double-clicking it, which files it under *Intermediate Certification Authorities*. | Make this browser trust vme-insight's certificate: [VME-PLUGIN, step 3](https://github.com/grahamfairbank/vme-insight/blob/main/docs/VME-PLUGIN.md#3-make-browsers-trust-the-certificate) (check the fingerprint, then import it with PowerShell into a *Root* store), and restart the browser. In a lab, opening the Grafana URL in its own tab and accepting the warning works until the browser restarts. |
| `ERR_SSL_PROTOCOL_ERROR`, with the Grafana URL set to an IP address | vme-insight's front end doesn't know which certificate to send for a bare IP | Prefer a name. Or add the IP to vme-insight's `HTTPS_HOSTNAME`, set `HTTPS_DEFAULT_SNI` to it, and include it in the certificate ([VME-PLUGIN, step 2](https://github.com/grahamfairbank/vme-insight/blob/main/docs/VME-PLUGIN.md#2-turn-on-https-and-anonymous-viewing)). |
| Tab loads, but Grafana's links or sign-in jump to a different address | The plugin's URL and vme-insight's `GRAFANA_ROOT_URL` differ (e.g. name vs IP) | Use the same address in both. |
| `403 Forbidden` from the frame | Your workstation's network isn't in vme-insight's `HTTPS_ALLOW` | Add it to `HTTPS_ALLOW` and `docker compose --profile https up -d https`. |
| Request times out | Browser can't reach Grafana (firewall, VPN, DNS) | Open the Grafana URL directly from the same machine. Check vme-insight's `HTTPS_ALLOW`. |

## Tab shows Grafana's sign-in page, or signs in and comes back

Anonymous access is off, or set to an organization that doesn't exist. A sign-in inside the frame won't stick ([why](AUTH.md#why-signing-in-inside-the-frame-doesnt-work)).

* vme-insight `.env`: `GRAFANA_ANONYMOUS=true`, then `docker compose up -d grafana`.
* `GRAFANA_ANONYMOUS_ORG` must match an organization name exactly, including the full stop in `Main Org.`.
* Check: `curl -sI https://<grafana>/d/vme-overview` returns `200`, not `302`.

## Tab shows "Dashboard not found"

| Cause | Fix |
|---|---|
| **Grafana organization ID** points at an organization without vme-insight's dashboards | Clear it, or provision the dashboards into that organization. |
| The URL points at a different Grafana | Check the URL. vme-insight's dashboards have fixed uids (`vme-hosts`, `vme-vms`, `vme-overview`). |

## Dashboard loads but shows the wrong thing or "No data"

| Symptom | Cause | Fix |
|---|---|---|
| Every panel says No data, host/VM picker shows an id instead of a name | vme-insight collects from a different VME manager, so the ids don't match | Point vme-insight's `morpheus.url` at this manager. |
| A new host or VM shows No data | vme-insight hasn't collected it yet | Wait a couple of minutes: vme-insight picks up hosts and VMs every minute by default (`intervals.metrics`). |
| A host opens the VMs dashboard, or the reverse | Unusual server type code | Add it to **Treat as hosts** / **Treat as VMs**, matching vme-insight's `classification`. |
| VM memory or OS empty | No QEMU guest agent in the VM | See vme-insight's [TROUBLESHOOTING](https://github.com/grahamfairbank/vme-insight/blob/main/docs/TROUBLESHOOTING.md#dashboards). |

## Home dashboard

| Symptom | Cause | Fix |
|---|---|---|
| No vme-insight dashboard to choose in **Dashboards to Display** | Plugin not loaded, or VME hasn't synced dashboards since the upload | Check the plugin status; reload the settings page. Re-enable the plugin if needed. |
| Dashboard listed but empty | The widget type registered after the dashboard | Disable and re-enable the plugin, or restart the VME UI service. |
| Some tiles say "Panel not found" or "Dashboard not found" | vme-insight older than 0.2.0 (no `vme-widget` dashboard), or a custom panel list from plugin 0.1.x | Upgrade vme-insight; check **Home dashboard panel IDs** ([CONFIGURATION](CONFIGURATION.md#home-dashboard-panel-ids)). |
| Some users don't see the widget | Their role can't read clusters | The widget follows **Infrastructure: Clusters → Read**. |

## Plugin won't load

| Status message | Cause | Fix |
|---|---|---|
| Version or compatibility error | VME older than 9.1 | Upgrade VME. The plugin needs Plugin API 1.5.0. |
| `ClassNotFoundException` / `NoSuchMethodError` mentioning `com.morpheusdata` | Plugin API changed in a newer VME release | Open an issue with the VME version and the message. Rebuilding against that release's `morpheusPluginApiVersion` may be enough ([DEVELOPMENT](DEVELOPMENT.md)). |

The VME UI log has the details. It's on the manager at `/var/log/morpheus/morpheus-ui/current`; search for `vme-insight`.
