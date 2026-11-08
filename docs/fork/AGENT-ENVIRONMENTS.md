# Agent Ubuntu environments

## Unreleased: structured preparation status

The agent launch-environment check retains only fixed stage/component identifiers,
measured image bytes/total/percentage, process exit codes and safe RU/EN errors.
Unknown process output is drained and discarded. There is no expandable raw log,
new preparation-operation log file, raw failure-report fallback or automatic upload.
Closing a preparation window keeps the operation running; reopening joins it.
Retry check becomes available after a failed operation. Cancelling the initial
inventory window suppresses its callback; it does not cancel a package transaction.

Existing `.agent-preparation/operations/*.log` files are left untouched. Their
previous contents are not cleaned by this change and may remain accessible through
the existing setup-directory guest bind. Generic environment installation logs,
chat histories, ordinary terminal output and native CLI logs are separate and
unchanged. This intentionally reduces diagnostic detail: package-manager output,
tracebacks and arbitrary network/error responses are no longer available here.
Read the [validation and scope](VALIDATION-preparation-structured-status.md).

## Unreleased: bounded component transfers

CLI, Node and Cloud CLI archives use one synchronous downloader across all four
agents. Connection limit: 45 seconds; low-speed threshold: 1 byte/s for 180 seconds;
per-attempt deadline: two hours; at most three attempts with 2/4-second pauses.
The sum of transfer deadlines is six hours plus six seconds of backoff. This is
not a timeout for APT, extraction, CLI checks, cleanup or the whole preparation.
Two hours is a conservative allowance for slow mobile transfers, not a device-
validated optimum. A progressing transfer can still reach that hard ceiling.

The shared queue only advances after the owned shell exits and its stream reaches
EOF. Closing the dialog does not cancel installation. Errors remain fixed typed
records, and original URLs/versions/SHA checks and existing installations are
preserved. See [validation and exact policy](VALIDATION-preparation-download-bounds.md).

## v2.1.2: shared Ubuntu AI and retained bindings

New AI Agents sessions use Ubuntu AI. Their form explains that Termux supplies
launch and component installation; it no longer offers ordinary-Ubuntu versus
Ubuntu-AI selection. Existing ordinary-Ubuntu and direct-Termux Codex profiles
keep their actual bindings during edit, save and launch. Managers opened from a
session use that original environment/project and do not silently redirect files.

When Ubuntu AI is missing, the common preparation flow downloads the fixed clean
ARM64 image from GitHub Components and installs the local archive using a bundled,
private proot-distro5.9.0. It never downgrades the system manager. Ready environments
are reused without downloading an image; incomplete/busy roots are preserved and
reported. All four agents share one Ubuntu AI and add only missing components
through existing recipes, without automatic CLI upgrades or model conversations.

In the published v2.1.2 implementation, the explicit launch-environment check
shows stages, measured image progress and a redacted log; reopening joins the existing serialized job. Closing its window
does not cancel preparation. Component readiness does not prove authorization or
model availability. See [installation](ENVIRONMENTS.md) and
[validation](VALIDATION-v2.1.2.md).

The former manual Ubuntu-to-Ubuntu-AI migration action remains removed. Rootfs,
authorization, history and project data are not copied or reset automatically.
Ordinary environment import, backup and explicit update operations remain separate.

## Shared environment deletion

Explicit environment deletion still checks references from saved/attached
sessions, active processes and native proot-distro locks. The bounded scan token
pins the reviewed tree. Deletion uses no-follow descriptors and a private
quarantine name; it does not follow guest symlinks into shared storage or other
systems. Once destructive deletion starts it cannot be cancelled; failures report
a possible quarantined remainder. No deletion runs merely by opening a menu.

Python and proot-distro prerequisites for this shared operation can still be
prepared after confirmation through the fixed `agent-operation-tools` branch of
`environment-runtime.sh`. This does not restore the removed rootfs migration
feature. Permission failures are reported as permission failures, not missing
packages. No installed user environment is used as a clean-install test fixture.

## Совместимость

Пользовательская миграция Ubuntu → Ubuntu AI удалена. Выбор среды, обычный запуск,
продолжение диалогов, импорт, резервное копирование и безопасное удаление окружений
сохранены. Уже существующие файлы и авторизация не переносятся и не удаляются.
Внутренняя совместимость данных при обновлении APK продолжает действовать.

See [usage](USAGE.md) and the [helper separation report](HELPER-EXPERIMENT-PAUSED.md).
The optional helper is paused and excluded from the next public release. Earlier
release notes and validation reports describe their historical version, not the
current migration UI or the composition of a future APK.


## Урок: 2026-11-02 — Unauthorized OCI не доказывает необходимость входа в Docker Hub

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-02 — Unauthorized OCI не доказывает необходимость входа в Docker Hub

- Контекст: диагностика отказа Ubuntu AI на этапе ARM64-манифеста, установленный proot-distro5.9.0.
- Вывод: pull.py сводит401/403 разных этапов к общей подсказке о приватном образе. При401 дочернего манифеста повторного получения токена нет. Перед советом войти проверять анонимный token endpoint, индекс и child manifest отдельно. EnvironmentSetup создаёт чистое окружение процесса: прокси чата не переносится в установщик.
- Применимость: указанная версия CLI и проверенный код запуска приложения. Не считать механизм обработчика доказательством причины конкретного сетевого отказа; Android VPN не исключается обходом HTTP-прокси.
- Подтверждение: самостоятельно выполнены четыре успешных metadata-пути (прямой клиент и штатная функция CLI, сHTTP-прокси и без), плюс фикстура401. Исходная ошибка установки не воспроизведена; слои не скачивались. Отчёт `/root/termux-fork/.verification/ubuntu-ai-registry-diagnosis/REPORT.md`, идентичность исходников и ответы рядом. Токены не сохранялись.

- Уточнение чистой установки: proot-distro5.9.0 CLI запрещает вложенный proot по TracerPid. Успех `_resolve_single_manifest` не подтверждает возможность вызвать полный install из текущей Ubuntu. Самостоятельный запуск штатных environment-install.py/runtime.sh в пустой фикстуре остановился до HTTP с exit1. Защиту не обходить и ручную распаковку не засчитывать за установку приложения. Для сквозного прогона требуется нативный Termux-контекст. Подтверждение: `/root/termux-fork/.verification/ubuntu-ai-first-launch/REPORT.md` и direct/install.log; полный install пока не подтверждён.

- Дополнение для5.10.3: на другом телефоне подтверждена эта версия. Хеши pull.py/transport.py/download.py совпали с официальным тегом. У общего `_Opener.open` есть NETWORK_TIMEOUT=120секунд, поэтому отсутствие timeout= в вызове manifest не означает бесконечное ожидание. Это socket timeout, не общий deadline; максимум5попыток, для429/503 учитывается Retry-After. Вывод об отсутствии тайм-аута в5.9.0 не переносить на5.10.3. Подтверждение: самостоятельно сверены исходники и присланный снимок; `/root/termux-fork/.verification/ubuntu-ai-registry-diagnosis/phone-5.10.3/REPORT.md`. HTTP-причина отказа на другом устройстве пока неизвестна.

- Решение пользователя/test32: новые Ubuntu AI используют отдельный GitHub Components OCI-образ Ubuntu26.04.1 LTS и частный proot-distro5.9.0 для локального архива; установленный системный менеджер не понижать. Старые среды/профили не переносить автоматически. Выбор версии не считать доказанным сетевым исправлением. Общая операция подготовки не запускает модель или вход; Vertex должен разделять установку Cloud CLI и авторизацию. Подтверждение: реальная загрузка с нового источника, upstream5.9.0 install API с запрещённой сетью,84JVM/52Python проверки; нативная сквозная проверка телефона ещё не подтверждена. Отчёт `docs/fork/VALIDATION-test32-ubuntu-ai.md` в актуальном checkout.

- Уточнение2026-11-02: пользователь подтвердил работу Claude на другом телефоне после перехода установки Ubuntu AI на образ с GitHub. Это пересказ проверки пользователя на устройстве, не самостоятельный прогон; точная версия APK и подробности шагов не сообщены. Причина прежнего отказа Docker Hub остаётся неустановленной. Результат не распространяется на остальных агентов и все сценарии установки/восстановления. Дополнение внесено в тот же отчёт test32.


## Урок: 2026-11-03 — Подготовка proot-distro остаётся под надписью проверки Ubuntu

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Подготовка proot-distro остаётся под надписью проверки Ubuntu

- Контекст: после переустановки на втором телефоне окно показывало отсутствующие proot-distro/Ubuntu AI и «Проверка Ubuntu AI…» более пяти минут. Пользователь прислал список процессов: apt и HTTPS method работали5:41; command -v proot-distro ничего не вывел. Эти данные получены от пользователя, не с текущего устройства.
- Подтверждение исходниками: AgentPreparationOperation сохраняет inventory и переходит к worker; agent-host-setup.sh выставляет checking-ubuntu перед pkg install -y proot-distro, а installing-ubuntu — только после него. Поэтому надпись не различает проверку и подготовку пакета. Локальный pkg сначала выбирает зеркало и при необходимости выполняет apt update; версия pkg другого телефона не сверена.
- Вывод: по этой надписи нельзя утверждать зависание инвентаризации или загрузку образа. Сначала различать apt update/install и рост частичных файлов. Наличие HTTPS-процесса само по себе не доказывает сетевой отказ. Срок25секунд относится к inventory, не ко всей подготовке. Исправление интерфейса этой записью не выполнено.


## Урок: 2026-11-03 — Этап подготовки должен предшествовать блокирующей команде

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Этап подготовки должен предшествовать блокирующей команде

- Контекст: в 2.1.3-test1 при отсутствующем proot-distro оставалось «Проверка Ubuntu AI…» во время pkg. Это отсутствие структурированного события, а не доказанное зависание сети.
- Вывод: перед pkg нужен отдельный этап; фиксированный протокол должен быть согласован между shell allowlist, AgentPreparationOutput и EnvironmentSetupStatus. Смена этапа не должна теряться из-за ограничения частоты уведомлений. Общий failure-marker должен сохранять предшествующий точный этап. Готовность отдельного CLI не означает готовность всей операции, особенно Gemini + Vertex.
- Условия: не возвращать stdout/stderr в состояние, ошибки или журнал; измеренные bytes и неопределённые остальные этапы сохраняются. Операции Gemini API и Vertex имеют разные ключи, но общий последовательный executor: нельзя использовать результат активного API-job как готовность Vertex.
- Подтверждение: локальный c92df2a2, test2 / 1478; 115 целевых JVM, 35 искусственных real-child случаев пяти вариантов, 6 shared-host фикстур и 9 local TLS/queue сценариев. Нет реальной установки или новой приёмки телефона. См. docs/fork/VALIDATION-2.1.3-test2.md активного checkout.

## Stable 2.1.6: selected environment and sealed deletion path

Deletion plans validate the selected alias and `containers/<alias>` identity.
Error messages name that environment. The deletion-only scan reports the exact
mode-000 `rootfs/run/systemd/dissect-root` directory as unscanned; other access
errors remain fatal. Removal uses kernel `rmdir` to verify emptiness without chmod.
A nonempty directory restores the original container before recursive deletion.
See [native Termux evidence and v2.1.5 reproduction](VALIDATION-2.1.6-environments.md).
Pinned image downloads may use the public Components asset API after gateway
errors; archive identity, size and SHA-256 remain mandatory on both routes.
