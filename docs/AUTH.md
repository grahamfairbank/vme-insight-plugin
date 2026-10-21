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

Three pieces: Grafana settings, https in front of Grafana, and a network restriction. vme-insight has all three as `.env` options (from the version after 0.1.0; see its [CONFIGURATION → https front end](https://github.com/grahamfairbank/vme-insight/blob/main/docs/CONFIGURATION.md#https-front-end)).

### 1. Grafana settings

In vme-insight's `.env`:

```bash
GRAFANA_ALLOW_EMBEDDING=true                 # no X-Frame-Options: deny
GRAFANA_ANONYMOUS=true                       # read-only Viewer without signing in
GRAFANA_ANONYMOUS_ORG="Main Org."            # must match an organisation's name exactly
GRAFANA_BIND=127.0.0.1                       # Grafana's own port only on the Docker host
GRAFANA_ROOT_URL=https://vme-insight.example.com:3443/
GRAFANA_COOKIE_SECURE=true
```

`Main Org.` is Grafana's default organisation, where vme-insight provisions its dashboards. Admins still sign in (top right → **Sign in**) for anything beyond viewing.

**Hardening: a dedicated organisation.** Create an organisation such as `VME viewers`, provision vme-insight's data sources and dashboards into it as well, and point `GRAFANA_ANONYMOUS_ORG` at it. Set the plugin's **Grafana organisation ID** to its id. Anonymous viewers then see only what's in that organisation, even if someone later adds other dashboards or data sources to Main Org.

### 2. https in front of Grafana

VME is served over https, and browsers block `http://` frames inside https pages (mixed content). vme-insight's optional front end (Caddy) adds https:

```bash
HTTPS_PORT=3443
HTTPS_HOSTNAME=vme-insight.example.com       # the name users type
HTTPS_TLS=internal                           # or "/certs/<cert>.pem /certs/<key>.pem"
```

The certificate must be **trusted by your users' browsers**. A framed page can't show a certificate warning; it just stays blank.

* **Your own certificate** (internal or public CA): put it in vme-insight's `https/certs/` and set `HTTPS_TLS`.
* **`internal`**: the front end runs its own small CA. Export its root certificate and install it as a trusted root on each admin workstation:
  ```bash
  docker compose --profile https cp https:/data/caddy/pki/authorities/local/root.crt vme-insight-root.crt
  ```
  For a quick lab test, opening the Grafana URL in its own tab and accepting the warning also works, until the browser is restarted.

### 3. Restrict who can reach Grafana

```bash
HTTPS_ALLOW="10.20.0.0/16 192.168.13.0/24"    # networks your VME admins work from
```

Everyone else gets `403 Forbidden`. With `GRAFANA_BIND=127.0.0.1`, the front end is the only way in from other machines. Then:

```bash
docker compose up -d grafana
docker compose --profile https up -d https
```

### 4. Point the plugin at it

In VME: **Administration → Integrations → Plugins → vme-insight** (pencil) → **vme-insight Grafana URL** = `https://vme-insight.example.com`. See [CONFIGURATION](CONFIGURATION.md).

### Check it

From a machine on an allowed network (no Grafana session):

```bash
curl -sI https://vme-insight.example.com:3443/d/vme-overview | grep -i -E '^HTTP|x-frame-options'
```

You should get `HTTP/2 200` and **no** `X-Frame-Options` header. From a machine outside `HTTPS_ALLOW` you should get `403`. A `302` to `/login` means anonymous access is off; `X-Frame-Options: deny` means embedding is off.

## What the plugin itself exposes

* The plugin stores only the settings in [CONFIGURATION](CONFIGURATION.md). None of them is a secret.
* It makes no network calls. VME's manager never connects to Grafana because of it.
* It adds the Grafana origin (for example `https://vme-insight.example.com`) to `frame-src` in VME's Content-Security-Policy, on the pages where it renders. It doesn't loosen any other directive.
* The frames send no referrer, so Grafana doesn't learn VME page addresses.
* Like every VME plugin, it runs inside the manager with the manager's privileges. It is a few hundred lines of Groovy; read it before installing.

## Reporting a vulnerability

Please open a private security advisory on the repository rather than a public issue.
