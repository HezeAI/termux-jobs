# Security, data and trust / Безопасность, данные и границы доверия

Current release: **2.1.8 /1540**. The implementation/security review below was
performed on **2026-11-04 for v2.1.3**; that date and scope are not extended by a
new release. This documentation reconciliation compared the named source paths
with v2.1.7 and its reports; it did not repeat a full security audit or phone tests.
[Original audit scope](docs/fork/SECURITY-AUDIT-STAGE2.md) ·
[2.1.7 verification](docs/fork/VALIDATION-v2.1.7.md) ·
[Separate release APK/source comparison](docs/fork/REPRODUCIBILITY-v2.1.7.md).

The 2.1.8 release changes action presentation, adapter display metadata, session
labels and notification lifecycle. It does not repeat the original security audit.
[2.1.8 validation](docs/fork/VALIDATION-v2.1.8.md) scopes the checks and phone acceptance.

## Changes and evidence since the original review

- Updater/package checks, Gemini/SSH vaults, backup engine/helper, diagnostic
  collector and the named server/skill/preparation classes retain identical
  source blobs between v2.1.3 and v2.1.7. This is source identity, not a new
  device check of credentials, crash recovery or backup restore.
- Chat adapters/UI, attachments, file viewer, environment image helper and Android
  manifest changed. Their newer checks are scoped in the version reports; the
  original review does not attest these changes. See [2.1.6](docs/fork/VALIDATION-v2.1.6.md)
  and [2.1.7](docs/fork/VALIDATION-v2.1.7.md).
- RTK is now optional per chat. Native rewriting may affect Codex command-prefix
  bans and Claude Bash deny/approval matching. Stored rules remain; the enable
  dialog warns about the limitation. Full-output recovery files may retain command
  data locally. Saving a recovery file does not itself send it to a model;
  explicit recall sends additional text. [RTK scope](docs/fork/RTK-MODE.md).
- Live Codex RTK evidence and Claude synthetic-provider CLI checks are distinct.
  The latter do not establish real Anthropic model behavior or all permission
  modes. Claude Auto availability depends on CLI/account support; root/Ubuntu
  Bypass restrictions remain. These are not blanket disablement of all controls.
- The image-history Codex WebSocket failure remains open. It was reproduced
  without this UI, but the closing remote component was not established.
  [Diagnostic report](https://github.com/openai/codex/issues/55730#issuecomment-6645086819).

«Не проверено» означает границу доказательств, а не подтверждённый дефект.
Android key handoff/crash recovery and backup restore phone scenarios remain
unverified in the documented scope. No previous phone acceptance is expanded
by this documentation update.

## Русский

### Проект и подпись

Termux Job’s — независимый неофициальный форк, который поддерживает HezeAI.
Проект не связан с командой Termux и не одобрен ею.
[Исходники](https://github.com/HezeAI/termux-jobs/tree/fork/base) и
[релизы](https://github.com/HezeAI/termux-jobs/releases) опубликованы отдельно.

Совместимость обновления проверяется с предыдущими версиями **Termux Job’s**:
должны совпасть package ID и сертификат, а versionCode должен увеличиться.
Одинакового имени `com.termux` недостаточно. Совместимость установки поверх
официального Termux из GitHub, F-Droid или Google Play здесь не подтверждена.
Удаление приложения не является обычным способом обновления: оно может удалить
его приватные данные. Сначала сохраните важную работу и отдельную резервную копию.

SHA-256 сертификата релизного APK Termux Job’s, отдельно от хеша файла:

```text
dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644
```

Хеш самого APK находится в `SHA256SUMS` соответствующего релиза и `update.json`.
Проверка подписи устанавливает подписанта и целостность, а не безопасность кода.

### Локальное хранение

Настройки, команды, заметки, пресеты и сведения сессий сохраняются в приватных
файлах приложения, включая `workspace-v1.json`. История CLI, проекты, инструкции,
LEARNING.md и навыки находятся в выбранном Termux/Ubuntu или на выбранном сервере.
Кеш предпросмотра и подготовленные вложения также могут сохраняться локально.
Некоторые черновики/позиции чата существуют только в памяти процесса; это не
обещание восстановления любого несохранённого текста после завершения приложения.
Экспорт/скачивание переносит выбранные данные в указанную папку или приложение.
Локальное расположение не препятствует доступу агента с соответствующими правами.

### Агенты и провайдеры

При отправке через Codex, Claude Code, Gemini или OpenCode выбранный CLI/провайдер
может получить текст, контекст диалога, инструкции, описания инструментов и навыков,
а также выбранные вложения и результаты инструментов. Конкретный формат зависит
от агента: изображения могут передаваться как обработанные изображения; видео —
как выбранные кадры с метками времени без звука; текстовые файлы — как выборки и
пути к оригиналу. Агент может затем прочитать доступный оригинал целиком. Просмотр
списка навыков сам по себе не отправляет запрос модели.

Агенты могут читать/менять доступные файлы, выполнять команды и обращаться к сети.
Следовательно, результаты чтения, команд и содержимое проекта могут стать частью
запроса провайдеру. Не помещайте секреты в доступный агенту проект без понимания
выбранных прав и политики провайдера. Обычная рабочая папка и инструкции не являются
изоляцией файловой системы. Ubuntu/proot использует права Android-приложения;
root внутри Ubuntu не даёт root Android и не создаёт отдельную виртуальную машину.

Выбранные права, native approvals и подтверждения ограничивают доступные действия
в пределах возможностей CLI и среды. Для Claude/Gemini принудительное ограничение
папкой в этой интеграции отмечено недоступным. Codex sandbox зависит от реальной
поддержки Android/proot. Широкие права или отключённые подтверждения увеличивают
доступ агента. Сохранение настроек не перенастраивает работающий процесс автоматически.
Внешние навыки — исполняемые агентом инструкции; импорт не запускает их скрипты
и не устанавливает зависимости, но последующее использование может выполнять их.

### Авторизация

Учётные данные CLI хранятся в его фактической среде: например, CODEX_HOME,
CLAUDE_CONFIG_DIR/`.claude`, `.gemini`, конфигурация OpenCode или Google Cloud.
Точный путь зависит от CLI и настроек; приложение не объявляет все эти файлы
зашифрованными. Редакторы инструкций/навыков не читают файлы авторизации для
диагностики и не копируют авторизацию между средами.

Введённый в приложение API-ключ Gemini сохраняется как шифротекст, привязанный к
профилю, с ключом Android Keystore (AES-GCM). Для запуска используется одноразовый broker-канал с проверкой принадлежности
запуску. Новый plaintext-файл передачи не создаётся; старые файлы автоматически
не удаляются. Функциональный путь с искусственным ключом принят на телефоне,
но низкоуровневые Android handoff/Keystore/crash проверки не выполнены. Это не защищает от всех действий процесса того же
UID, повреждённой ОС, инструментов агента или утечки через пользовательские команды.
Пароли SSH имеют отдельный механизм Keystore/передачи. Перенос архива не переносит
автоматически ключи Android Keystore на другой телефон.

### Подтверждённые сетевые пути

- Обновлятор обращается к GitHub Releases API за стабильным релизом и `update.json`.
  Автопроверка при открытии приложения включена по умолчанию и отключается в меню.
  APK скачивается после отдельного действия; перед установкой проверяются размер,
  SHA-256, пакет и сертификат. Проверка обновлений не загружает переписку.
- Подготовка окружений/компонентов скачивает закреплённые образы из GitHub Components,
  CLI/Node из источников рецептов и пакеты из репозиториев дистрибутива/Termux.
  Установка и обновление пакетов исполняют сторонний код. Для новой Ubuntu AI
  локальный закреплённый архив устанавливается без Docker Hub; другие механизмы
  окружений могут использовать свои registry/repository endpoints.
- Импорт публичного GitHub-навыка обращается к GitHub за выбранной ревизией.
  Провайдеры, OAuth/вход и дополнительные функции CLI имеют собственные сетевые пути.
- Справочник/WebView/браузер открывают выбранные сайты; поиск документации передаёт
  запрос сайту поиска. HTTP(S)-ссылки и внешние страницы могут загружать ресурсы
  сторонних сайтов. SSH/серверные инструменты подключаются к указанному серверу,
  проверяют соединение и передают выбранные команды/файлы. Загрузчики медиа
  и графических компонентов обращаются к выбранным источникам.
- Локальный HTML/GIF-предпросмотр чата использует отдельный WebView: нет app bridge,
  доступа к произвольным файлам, file chooser или разрешений страницы; запросы
  ограничены одним виртуальным документом. Это не описание обычного веб-браузера.

Серверы видят обычные сетевые метаданные, например IP и параметры запроса.
Прокси/VPN и настройки провайдера меняют маршрут. Проверены названные пути кода,
но полного анализа трафика всех нативных CLI, WebView, пакетов и зависимостей
не выполнялось. Заявлений «ничего не отправляется» или «нет телеметрии» проект
по этой проверке не делает. Политики и сроки хранения данных провайдера определяются
им самим и выбранным аккаунтом, а не этим документом.

### Диагностика и резервные копии

Структурированная диагностика включает версии, среду/идентификаторы сессий,
события/этапы, коды ошибок, время и сведения запуска. Подготовка окружений сохраняет только ограниченные этапы/компоненты/измеренный
прогресс/коды и безопасные описания ошибок; полный stdout/stderr не накапливается
и не экспортируется. Старые журналы не удалены. Другие отчёты, например X11, могут
содержать очищенные сообщения загрузчика, пути и адреса. Очистка эвристическая:
проверяйте текст перед передачей. Это не полный сетевой trace и не доказательство
отсутствия секретов во всех native логах. Копирование/экспорт — отдельное действие;
в рассмотренных сборщиках нет автоматической отправки диагностического отчёта.

Базовый экспорт настроек покрывает только заявленные данные. Полный/выборочный
backup HOME или окружения может включать CLI-авторизацию, SSH-ключи, проекты,
историю, инструкции и секреты. Он не считается автоматически зашифрованным или
безопасным для публикации. Данные подключения OpenCode выбираются отдельно и могут содержать авторизацию,
историю, журналы и внутренние снимки проектов. Исключение проектной папки
не исключает её копии из native истории. Android `allowBackup=false` не отменяет ручной экспорт.
См. [объём и ограничения копирования](docs/fork/BACKUP-RESTORE.md).

### Проверки и сообщение об уязвимости

Автотесты, Lint, ревью кода и проверка APK описаны в отчёте релиза. Проверки HezeAI
на телефоне отмечены отдельно и распространяются на названные сценарии.
Источники и границы проверок указаны выше. Открытый код, число тестов
и совпадение сертификата не заменяют проверку конкретного поведения.

Закрытые сообщения об уязвимостях: [GitHub private vulnerability reporting](https://github.com/HezeAI/termux-jobs/security/advisories/new).
Функция включена 2026-11-08; повторный запрос GitHub API подтвердил `enabled: true`.
Доставка пробного сообщения не проверялась. Для обычных ошибок используйте
[GitHub Issues](https://github.com/HezeAI/termux-jobs/issues).
Не публикуйте ключи, приватные журналы, архивы окружений, чужие данные или
чувствительные детали эксплуатации в открытых Issues.

## English

### Project, updates and signer

Termux Job’s is an independent unofficial fork maintained by HezeAI. It is not
affiliated with or endorsed by the Termux team. Its
[source](https://github.com/HezeAI/termux-jobs/tree/fork/base) and
[releases](https://github.com/HezeAI/termux-jobs/releases) are published separately.

Update compatibility is checked against previous **Termux Job’s** versions with
matching package ID and signing certificate and an increasing versionCode.
`com.termux` alone is insufficient. Installing over official GitHub, F-Droid or
Google Play Termux builds is not verified here. Uninstalling is not a normal
update step: it can delete private app data. Save work and keep a separate backup.

Release APK signing-certificate SHA-256, distinct from the APK file SHA-256:

```text
dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644
```

The APK file digest is in that release's `SHA256SUMS` and `update.json`. Signature
verification identifies the signer and integrity; it does not establish code safety.

### Storage, agents and credentials

Settings, saved commands/notes/presets and session metadata are stored in private
app files, including `workspace-v1.json`. Native CLI histories, projects,
instructions, memory and skills reside in the selected Termux/Ubuntu or remote
server. Preview caches and staged attachments may remain locally. Some drafts and
scroll positions are process-memory only. Explicit export/download copies selected
content to its destination; local storage does not prevent a permitted agent read.

The selected CLI/provider can receive messages, conversation context, instructions,
skill/tool descriptions, attachments and tool results. Images may be processed;
video attachments use sampled frames/timestamps without audio; text attachments
can include excerpts and original paths. An agent may read the complete accessible
original later. Listing skills alone does not request a model response.

Agents can read/write accessible files, execute commands and use networks. Project
content and command output can consequently enter provider requests. A working
directory or instruction text is not filesystem isolation. Proot shares the Android
app's permissions; guest root is not Android root or a separate virtual machine.
Native permissions/approvals depend on the CLI and environment. Enforced project
folder limits are unavailable for Claude/Gemini in this integration; Codex sandbox
support depends on Android/proot. Broad rights/disabled approvals increase access.
Saved settings do not automatically reconfigure an active process. Importing a skill
does not run its scripts/install dependencies; later agent use can execute them.

CLI authorization stays in its actual environment, such as CODEX_HOME,
CLAUDE_CONFIG_DIR/`.claude`, `.gemini`, OpenCode or Google Cloud configuration.
Paths depend on CLI settings. These files are not all claimed to be encrypted.
Instruction/skill diagnostics do not read authorization files or migrate accounts.
App-entered Gemini keys use profile-bound Android Keystore AES-GCM ciphertext,
with a run-owned one-shot broker handoff. No new plaintext handoff file is created;
old files are not deleted automatically. The artificial-key/API-refusal/normal-retry
path was accepted on the phone; low-level Android handoff/Keystore/crash remains
unverified. SSH passwords use a separate
Keystore/handoff mechanism. This does not cover compromised OS/same-UID processes,
agent tools or user command leaks; copying a backup does not transfer Keystore keys.

### Networks, diagnostics and backups

The updater contacts GitHub Releases API and metadata. Foreground automatic checks
are enabled by default and can be disabled. APK download/installation is separate;
size/SHA/package/certificate checks apply. The updater does not upload chat history.
Environment setup downloads pinned Components images and recipe-specified CLI/Node
archives and distribution/Termux packages. Package installation executes third-party
code. New Ubuntu AI uses a local archive without Docker Hub; other environment
mechanisms can have their own registry endpoints. Public skill import contacts
GitHub. Agent provider/OAuth/CLI features have additional network behavior.

Documentation search, websites and ordinary browser WebViews contact their sites
and may load third-party resources. SSH/server tools connect to the chosen server
and transfer selected commands/files. Media/graphics downloads contact their
selected sources. Local chat HTML/GIF preview is a separate restricted WebView,
serving one virtual document with no app bridge, arbitrary-file access, file chooser
or page permissions; that is not a claim about the general web browser.

Servers see ordinary network metadata such as IP/request parameters. Proxies/VPNs
and provider settings change routing. These named code paths were reviewed; a full
traffic/dependency/native-CLI audit was not performed. No blanket “nothing is sent”,
“no telemetry” or universal secret-protection claim follows. Provider retention and
account policies are outside this app's control.

Structured diagnostics include versions, environment/session identifiers, stages,
events, times and error codes. Environment preparation retains only bounded stages/components/measured progress,
exit codes and safe error descriptions; raw stdout/stderr is not accumulated or
exported. Old logs are not deleted. Other reports, including X11, can contain
redacted downloader messages, paths and addresses. Redaction is heuristic; review before sharing. Reports
are not complete HTTP traces or guarantees that native logs contain no secrets.
The reviewed report collectors do not automatically upload diagnostic reports.
Copy/export is explicit. Basic settings export has limited coverage; full/custom
HOME/environment backups can contain CLI credentials, SSH keys, history, projects,
instructions and secrets. Do not assume encryption or publish them. Android
`allowBackup=false` does not disable manual export. Optional OpenCode connection
data can include credentials, history/logs and internal project snapshots; excluding
a project folder does not remove its copies from native history. See
[backup scope](docs/fork/BACKUP-RESTORE.md).

### Evidence and reporting

Release validation distinguishes automated tests/Lint/code and artifact review
from HezeAI's named phone checks. Source availability, test counts and a matching
signer do not replace verification of specific behavior. Historical reports retain their original scope; consult the
current release report for active limits.

Report sensitive vulnerabilities through [GitHub private vulnerability reporting](https://github.com/HezeAI/termux-jobs/security/advisories/new).
Enabled on 2026-11-08 and confirmed by a subsequent GitHub API query (`enabled: true`).
Delivery of a test report was not exercised. Use [GitHub Issues](https://github.com/HezeAI/termux-jobs/issues)
for ordinary bugs. Do not post keys, private logs, environment archives, other
people's data or sensitive exploit details in public issues.

## Implementation paths from the dated review / Пути исходного обзора

- [WorkspaceUpdates](app/src/main/java/com/termux/app/workspace/WorkspaceUpdates.java),
  [UpdateConnection](app/src/main/java/com/termux/app/workspace/UpdateConnection.java): update queries/download and package/signer checks.
- [AgentChatUi](app/src/main/java/com/termux/app/workspace/AgentChatUi.java),
  [agent-attachments](app/src/main/assets/agent-attachments.cjs),
  [Codex](app/src/main/assets/codex-chat.cjs), [Claude](app/src/main/assets/claude-chat.cjs),
  [Gemini](app/src/main/assets/gemini-chat.cjs), [OpenCode](app/src/main/assets/opencode-chat.cjs): context, files and native operations.
- [GeminiApiKeyVault](app/src/main/java/com/termux/app/workspace/GeminiApiKeyVault.java),
  [SshPasswordVault](app/src/main/java/com/termux/app/workspace/SshPasswordVault.java): app credential storage/handoff.
- [ChatFileViewer](app/src/main/java/com/termux/app/workspace/ChatFileViewer.java),
  [WorkspaceDocumentation](app/src/main/java/com/termux/app/workspace/WorkspaceDocumentation.java),
  [ServerConnection](app/src/main/java/com/termux/app/workspace/ServerConnection.java): local/remote views and connections.
- [AgentPreparation](app/src/main/java/com/termux/app/workspace/AgentPreparation.java),
  [environment-image](app/src/main/assets/environment-image.py),
  [agent-skills](app/src/main/assets/agent-skills.py): downloads and installation boundaries.
- [WorkspaceDiagnostics](app/src/main/java/com/termux/app/workspace/WorkspaceDiagnostics.java),
  [WorkspaceBackupEngine](app/src/main/java/com/termux/app/workspace/WorkspaceBackupEngine.java),
  [workspace-backup](app/src/main/assets/workspace-backup.py),
  [manifest](app/src/main/AndroidManifest.xml): diagnostics, backup and Android declarations.
