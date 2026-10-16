# Опыт разработки: карточки ошибок v1.2.11

Добавлены36 карточек,4 прежние расширены без дублирования. Итого114.

Это собственные краткие изложения локальных отчётов и наблюдений разработчика.
Статус resolved относится к описанному случаю; verified-code — к проверке кода,
guidance — к пояснению; open — к нерешённому случаю. Это не гарантия решения
на всех устройствах. Данные аккаунтов, адреса серверов, ключи и токены не включены.

OpenCode OC001 остаётся открытым: мерцание наблюдалось до буферов v1.0.10
и в новом чате. Причина не сводится доказанно к истории; откат экспериментов
не означает исправления. Исторические ограничения OAuth/бесплатных моделей
нельзя переносить на все будущие версии провайдеров.

Новые записи не содержат исполняемой команды; исправления описаны текстом.
Поиск включает свидетельства и ID. База не читает терминалы и не обращается
к сети для диагностики. Тесты приложения не подтверждают каждый совет на ОС.

| ID | Статус |
| --- | --- |
| termux-storage | verified-code |
| proot-version | verified-code |
| ssh-publickey | verified-code |
| linux-exec-format | verified-code |
| jobs-keyboard-window | verified-code |
| jobs-draft-per-tab | guidance |
| jobs-nested-scroll | guidance |
| jobs-filter-highlight | guidance |
| jobs-startup-sequence | guidance |
| jobs-session-vs-process | guidance |
| jobs-codex-status-bridge | guidance |
| jobs-chat-directory | guidance |
| jobs-access-instructions | guidance |
| jobs-opencode-free-adapter | verified-code |
| jobs-opencode-model-revert | resolved |
| jobs-opencode-flicker-oc001 | open |
| jobs-gemini-auth-mode | verified-code |
| jobs-gemini-policy-permissions | verified-code |
| jobs-gemini-oauth-unavailable | open |
| jobs-provider-capacity | guidance |
| jobs-backup-source-unreadable | verified-code |
| jobs-backup-size | guidance |
| jobs-backup-runtime-dependencies | guidance |
| jobs-backup-dialog-cancel | guidance |
| jobs-backup-progress | guidance |
| jobs-keystore-transfer | guidance |
| jobs-apk-update-identity | guidance |
| jobs-update-partial-download | guidance |
| jobs-agent-probe | guidance |
| jobs-server-media | verified-code |
| jobs-server-ui-refresh | resolved |
| jobs-diagnostic-boundaries | guidance |
| jobs-sftp-performance | resolved |
| jobs-editor-conflict | guidance |
| jobs-package-active-shell | guidance |
| jobs-gradle-lint-toolchain | guidance |
| jobs-assistant-import-api | verified-code |
| jobs-assistant-ui-loop | verified-code |
| jobs-assistant-apk-padding | verified-code |
| jobs-assistant-stale-stage | guidance |
