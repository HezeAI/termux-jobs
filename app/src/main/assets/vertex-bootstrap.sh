#!/usr/bin/env bash
# User-launched Vertex preparation. Native Cloud login stays visible in its terminal.
_jobs_vertex_prepare() (
    set -euo pipefail
    jobs_agent_cloud_credentials && exit 0
    local root="$HOME/.local/share/termux-jobs/gemini-cli" cloud stage
    cloud=$(command -v gcloud || true)
    [[ -n $cloud ]] || cloud=$(jobs_agent_cloud_path)
    if [[ ! -x $cloud ]]; then
        [[ ${JOBS_AGENT_SETUP_ALLOWED:-0} == 1 ]] || { jobs_agent_say 'Для входа требуется Google Cloud CLI. Подтвердите установку при запуске.' 'Google Cloud CLI is needed to sign in. Confirm installation on launch.' >&2;exit 1; }
        jobs_agent_say 'Установка отдельного Google Cloud CLI…' 'Installing private Google Cloud CLI…'
        mkdir -p "$root" || exit $?;chmod 700 "$root" || exit $?;exec 8>"$root/gcloud-install.lock" || exit $?;flock -w 300 8 || exit $?
        if [[ ! -x $cloud ]]; then
            [[ ! -e $root/google-cloud-sdk-589.0.0 ]] || { printf '%s\n' 'Existing unusable Cloud CLI preserved.' >&2;exit 1; }
            stage=$(mktemp -d "$root/.gcloud-XXXXXXXX") || exit $?;trap 'rm -rf -- "$stage"' EXIT || exit $?
            curl --fail --location --proto '=https' --tlsv1.2 --connect-timeout 20 --max-time 1200 --retry 2 -o "$stage/gcloud.tgz" 'https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-cli-589.0.0-linux-arm.tar.gz' || exit $?
            printf '%s  %s\n' 3febb61a57ea76612973e40a2ff7bd79939161451d11130659d5c981201dca97 "$stage/gcloud.tgz" | sha256sum -c - || exit $?
            tar -xzf "$stage/gcloud.tgz" --no-same-owner -C "$stage" || exit $?
            [[ $(cat "$stage/google-cloud-sdk/VERSION") == 589.0.0 ]] || exit 1
            [[ ! -e $root/google-cloud-sdk-589.0.0 ]] || { printf '%s\n' 'Existing unusable Cloud CLI preserved.' >&2;exit 1; }
            mv "$stage/google-cloud-sdk" "$root/google-cloud-sdk-589.0.0" || exit $?
        fi
        flock -u 8;exec 8>&-
    fi
    mkdir -p "$root" || exit $?;chmod 700 "$root" || exit $?;exec 9>"$root/gcloud-auth.lock" || exit $?;flock -w 300 9 || exit $?
    jobs_agent_cloud_credentials && exit 0
    jobs_agent_say 'Вход Google Cloud: откройте ссылку из терминала в браузере и подтвердите доступ. Затем вернитесь и выполните подсказки Google.' 'Google Cloud sign-in: open the terminal link in your browser and approve access. Then return and follow Google instructions.'
    export CLOUDSDK_COMPONENT_MANAGER_DISABLE_UPDATE_CHECK=true
    if [[ -n ${JOBS_GEMINI_VERTEX_PROJECT:-} ]]; then export CLOUDSDK_CORE_PROJECT="$JOBS_GEMINI_VERTEX_PROJECT";fi
    "$cloud" auth application-default login --no-launch-browser || exit $?
    jobs_agent_cloud_credentials || { jobs_agent_say 'Авторизация Google Cloud не завершена.' 'Google Cloud authorization was not completed.' >&2;exit 1; }
)
_jobs_vertex_prepare || return
unset -f _jobs_vertex_prepare
