#!/usr/bin/env bash
# Sourced after Cloud auth, before Gemini. Read data, never source/eval user input.
jobs_vertex_project() {
    [[ -z ${GOOGLE_API_KEY:-} ]] || return 0
    local project=${JOBS_GEMINI_VERTEX_PROJECT:-${GOOGLE_CLOUD_PROJECT:-}} location=${JOBS_GEMINI_VERTEX_LOCATION:-${GOOGLE_CLOUD_LOCATION:-global}} request=${JOBS_VERTEX_REQUEST:-} response=${JOBS_VERTEX_RESPONSE:-} count=0
    if [[ -z $project ]]; then
        [[ -n $request && -n $response ]] || return 1
        [[ ! -e $response ]] || { printf '%s\n' 'Stale Vertex response; restart the session.' >&2;return 1; }
        ( umask 077; printf 'project\n' > "$request" ) || return $?
        jobs_agent_say 'Учётные данные Google Cloud найдены. Укажите ID облачного проекта и регион в окне приложения. Это не путь к папке.' 'Google Cloud credentials were found. Enter a cloud project ID and location in the app. This is not a folder path.'
        while [[ ! -f $response ]]; do
            ((count+=1));if ((count>900));then rm -f -- "$request";printf '%s\n' 'Vertex project setup timed out. Restart the session to retry.' >&2;return 1;fi
            sleep 1 || return $?
        done
        { IFS= read -r project;IFS= read -r location; } < "$response" || return 1
        if [[ ! $project =~ ^([a-z][a-z0-9-]{4,28}[a-z0-9]|[0-9]{6,20})$ || ! $location =~ ^(global|[a-z]+(-[a-z0-9]+){1,4})$ || ${#location} -gt 63 ]];then rm -f -- "$request" "$response";printf '%s\n' 'Invalid Vertex project/location.' >&2;return 1;fi
        rm -f -- "$request" "$response" || return $?
    fi
    export GOOGLE_CLOUD_PROJECT="$project" GOOGLE_CLOUD_LOCATION="$location" JOBS_GEMINI_VERTEX_PROJECT="$project" JOBS_GEMINI_VERTEX_LOCATION="$location"
}
jobs_vertex_project || return $?
unset -f jobs_vertex_project
