# Authentication and security

The plugin only builds a URL. The **browser** loads the dashboard from vme-insight's Grafana inside a frame on a VME page. So two things must be true for a tab to show anything:

1. **Grafana must allow being framed.** By default Grafana sends `X-Frame-Options: deny`. vme-insight turns this off with `GRAFANA_ALLOW_EMBEDDING=true` in its `.env`.
2. **The browser must be let into Grafana without a sign-in page.** A sign-in inside the frame rarely works (see [why](#why-signing-in-inside-the-frame-doesnt-work)), so Grafana has to recognise the viewer some other way.

This page compares the ways to do the second, recommends one, and lists what each exposes.

## Why signing in inside the frame doesn't work

VME (say `https://vme.example.com`) and Grafana (say `https://vme-insight.example.com`) are different sites. To a browser, Grafana inside a VME page is a **third-party frame**:

* Grafana's session cookie is `SameSite=Lax` by default, so the browser doesn't send it to a framed Grafana. You sign in, and the next request is anonymous again.
* Setting `cookie_samesite = none` (with `cookie_secure = true`) helps in some browsers only. Safari and Firefox block third-party cookies by default, and Chrome users can too.
* Single sign-on pages (SAML, OIDC) usually refuse to be framed at all.

So the frame needs an approach that doesn't depend on a Grafana session cookie.

## The options

| | **A. Anonymous Viewer, restricted by network** *(recommended)* | **B. Auth proxy** | **C. Service-account token** |
|---|---|---|---|
| How | Grafana treats anyone who can reach it as a read-only Viewer in one organisation. A firewall or reverse proxy limits who can reach it. | A reverse proxy authenticates the user (for example oauth2-proxy against your IdP) and passes the user name to Grafana in a trusted header (`[auth.proxy]`). | A Grafana service-account token authenticates requests. |
| Works in the frame? | **Yes.** No cookie or sign-in needed. | Only after the user has signed in to the proxy in a normal tab, and only where the browser sends the proxy's third-party cookie (`SameSite=None; Secure`). Breaks in Safari and Firefox defaults. | **No, not on its own.** Tokens go in an `Authorization` header, which a browser can't add to a frame. Grafana doesn't accept them in the URL. It needs a proxy to inject the header, which makes it option A with a secret to manage. |
| Who is the viewer in Grafana | Everyone is the same anonymous Viewer | The real user (from your IdP) | Whoever the token belongs to |
| Changes to vme-insight | Grafana settings (`.env` / compose), an https front end, a network rule | An https proxy with an IdP client, Grafana `auth.proxy` settings | A proxy that injects the header |
| Changes to VME | None | None (VME can't hand its session to the proxy) | None |
| Exposure | Anyone on the allowed networks can read vme-insight's data | Only signed-in users | Anyone who gets the token can call Grafana's API as that account, from anywhere it reaches |
| Effort | Low | High | Medium, and no gain over A |

**Not an option: Grafana shared ("public") dashboards.** They need no sign-in, but they ignore template variables, so a host tab couldn't filter to its host.

**Later, for per-user identity: signed JWT.** Grafana can accept a short-lived signed JWT in the URL (`[auth.jwt]` with `url_login = true`). The plugin could sign one per page view with the VME user's name, and Grafana would check it with the matching public key. That gives per-user identity without a proxy. It needs a key pair held by the plugin, and its cookie behaviour in third-party frames must be tested before it can be relied on. It is a candidate for a later version, not part of v0.1.

## Recommendation: A, anonymous Viewer restricted by network

For vme-insight this is the right trade-off:

* vme-insight's data is **read-only monitoring data**: metrics, inventory, lifecycle status. It holds **no VME credentials**; the collector removes secrets before storing anything (see vme-insight's [SECURITY](https://github.com/grahamfairbank/vme-insight/blob/main/docs/SECURITY.md#whats-stored)).
* People who use VME's host, VM and cluster pages can already see most of it in VME itself.
* It works in every browser, needs no IdP and no secret in the plugin, and keeps the plugin a thin frame.

What you give up, and how to limit it:

| Trade-off | Mitigation |
|---|---|
| No per-user identity in Grafana: everyone on the allowed networks sees every vme-insight dashboard, for every cluster, regardless of their VME role or group access | Restrict Grafana to admin networks (VPN, management VLAN). In VME, tick **Only show to roles with the vme-insight permission** so only chosen roles see the tabs (this hides the tabs; it doesn't secure Grafana). |
| A Viewer can list data sources (names and types, no passwords) and send its own read queries to them through Grafana's API, not only view panels | vme-insight's Grafana database user can only `SELECT`. The data is the same inventory the dashboards show (host names, IPs, VM names, user names in activity). Use a dedicated Grafana organisation (below) so anonymous viewers never see anything else added to Grafana. |
| Anyone who can reach the URL can read it without a password | The network restriction is the control. Don't publish this Grafana to the internet. |
| Grafana's version is visible | `hide_version = true` for anonymous users. |

Choose **B** instead if your security policy requires named users for monitoring data, and your users can sign in to the proxy in a normal tab first.

## Recommended setup

vme-insight 0.2.0 and later have everything option A needs as optional settings. The step-by-step guide is vme-insight's [VME-PLUGIN](https://github.com/grahamfairbank/vme-insight/blob/main/docs/VME-PLUGIN.md). In outline:

1. **Grafana:** embedding on, anonymous read-only Viewer on, Grafana's own port kept on the Docker host (`GRAFANA_ALLOW_EMBEDDING`, `GRAFANA_ANONYMOUS`, `GRAFANA_BIND=127.0.0.1`).
2. **https:** vme-insight's https front end (`--profile https`), with a certificate from a CA your browsers already trust if you have one; otherwise a self-signed certificate for that host only (`tools/make-cert.sh host`). The built-in CA (`HTTPS_TLS=internal`) is a lab shortcut: it can vouch for any website once trusted.
3. **Network restriction:** `HTTPS_ALLOW` lists the networks your VME admins use; everyone else gets `403`.
4. **Certificate trust:** unless the certificate comes from a CA your browsers already trust, each admin workstation imports it, after checking its fingerprint. On Windows, use PowerShell into a *Root* store, for your own account unless the machine is shared; Windows' import wizard files it as an intermediate, where browsers still reject it.
5. **Plugin:** set **vme-insight Grafana URL** to the https address users browse to, the same as `GRAFANA_ROOT_URL`.

**Hardening: a dedicated organisation.** Create a Grafana organisation such as `VME viewers`, provision vme-insight's data sources and dashboards into it as well, and point `GRAFANA_ANONYMOUS_ORG` at it. Set the plugin's **Grafana organisation ID** to its id. Anonymous viewers then see only what's in that organisation, even if someone later adds other dashboards or data sources to Main Org.

## What the plugin itself exposes

* The plugin stores only the settings in [CONFIGURATION](CONFIGURATION.md). None of them is a secret.
* It makes no network calls. VME's manager never connects to Grafana because of it.
* It adds the Grafana origin (for example `https://vme-insight.example.com`) to `frame-src` in VME's Content-Security-Policy, on the pages where it renders. It doesn't loosen any other directive.
* The frames send no referrer, so Grafana doesn't learn VME page addresses.
* Like every VME plugin, it runs inside the manager with the manager's privileges. It is a few hundred lines of Groovy; read it before installing.

## Reporting a vulnerability

Please open a private security advisory on the repository rather than a public issue.
