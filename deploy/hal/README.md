# HAL deployment

Run commands from the repository root on `hal.local`. The application uses the
existing, authenticated Hermes OpenAI-compatible API on `127.0.0.1:8642/v1`.
Its API key belongs in the untracked root `.env`, never in this directory or APK.

```bash
docker compose -f compose.yaml -f deploy/hal/compose.override.yaml config --quiet
docker compose -f compose.yaml -f deploy/hal/compose.override.yaml up --build -d
curl -fsS http://127.0.0.1:18000/ping
curl -fsS http://127.0.0.1:18080/api/ping
```

The API listens on loopback port 18000; the web UI listens on port 18080. Host
networking permits access to the existing loopback-only Hermes service. No
optional GPU model containers are started by these commands.

Use `https://rokid.uniclown.com/api` as the APK's LightMem backend URL. The
server's existing Cloudflare Tunnel can route `rokid.uniclown.com` to
`http://127.0.0.1:18080`. A Cloudflare Access login redirect must be resolved
for the native APK before it can use that external API; the APK has no browser
login integration. The web UI can use the existing browser login.

The HAL smoke-test configuration disables ASR until speech credentials are
configured. Visual embedding remains `mock`, as in the upstream Docker example;
this verifies infrastructure, not full visual retrieval or continuous solving.
Actual model processing must be checked separately from `/ping`.

Build the custom APK from Actions → **Build LightMem Rokid APK**. The workflow
defaults to this domain and a `.custom` package. The optional repository secret
`LIGHTMEM_DEBUG_KEYSTORE_B64` keeps its debug signing key stable across builds.

Inspect service and worker logs:

```bash
docker compose -f compose.yaml -f deploy/hal/compose.override.yaml ps
docker compose -f compose.yaml -f deploy/hal/compose.override.yaml logs --tail 100
tail -n 100 docker-data/logs/query.log
```
