# HAL deployment

Run commands from the repository root on `hal.local`. The application uses the
existing, authenticated Hermes OpenAI-compatible API on `127.0.0.1:8642/v1`.
Its API key belongs in the untracked root `.env`, never in this directory or APK.

For this same-host Hermes deployment, also configure:

```dotenv
EM2MEM_AGENT_SESSIONS_DIR=/root/projects/LightMem-Ego/docker-data/sessions
EM2MEM_MCUR_MAX_QUERY_IMAGES=1
EM2MEM_MCUR_MAX_EVIDENCE_FRAMES=1
EM2MEM_MCUR_ANSWER_MODEL_RETRIES=0
EM2MEM_CHAT_COMPLETIONS_FALLBACK_ATTEMPTS=1
EM2MEM_OPENAI_TIMEOUT_SECONDS=90
```

The sessions directory is the path visible to Hermes on the host. LightMem adds
the selected image's exact host path to the current-frame prompt so Hermes can
use its vision tool without searching for a container-relative filename. Image
attachments are still sent normally. These optional settings do not change the
Hermes gateway's listener, credentials or global model configuration.

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

On 2026-10-21 the tunnel route was validated, but the public URL still returned
an Access login redirect. The owner is handling the domain's Access exception;
other protected hostnames must keep their existing policies. Verify that
`https://rokid.uniclown.com/api/ping` returns JSON without login before using the
APK remotely. This demo does not add application-level authentication.

The HAL smoke-test configuration disables ASR until speech credentials are
configured. Visual embedding remains `mock`, as in the upstream Docker example;
this verifies infrastructure, not full visual retrieval or continuous solving.
Actual model processing must be checked separately from `/ping`.

The current-frame API was tested on 2026-10-21 with a synthetic 1280x720 image
containing a random verification code, an arithmetic question, a red rectangle
and an underline. Hermes (owner-selected `deepseek-v4-flash-vision-exp`) read the
code correctly and answered all three questions correctly in about 44 seconds
end to end. The request used one image and skipped interaction cache and
long-term retrieval. This is a smoke test, not a real-glasses accuracy or
latency benchmark. Current frames expire after 60 seconds by default: upload a
fresh frame immediately before a manual test. The APK has not been installed
or tested on physical glasses during this deployment.

Build the custom APK from Actions → **Build LightMem Rokid APK**. The workflow
defaults to this domain and a `.custom` package. The optional repository secret
`LIGHTMEM_DEBUG_KEYSTORE_B64` keeps its debug signing key stable across builds.

Inspect service and worker logs:

```bash
docker compose -f compose.yaml -f deploy/hal/compose.override.yaml ps
docker compose -f compose.yaml -f deploy/hal/compose.override.yaml logs --tail 100
tail -n 100 docker-data/logs/query.log
```
