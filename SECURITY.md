# Security, data and trust / Безопасность, данные и границы доверия

Applies to Termux Job’s v2.1.3. Updated on 2026-11-04 against the release source
and APK checks; current first-party audit scope is [recorded separately](docs/fork/SECURITY-AUDIT-STAGE2.md). This describes implementation and known limits, not a security
certification. Earlier dated validation reports describe their named versions.

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
  проверяют соединение и передают выбранные команды/файлы. Загрузчики медиа,
  моделей и графических компонентов обращаются к выбранным источникам.
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
Подтверждённого независимого аудита безопасности этой версии нет. Открытый код,
число тестов и совпадение сертификата не гарантируют безопасность.

Подтверждённый канал связи — [GitHub Issues проекта](https://github.com/HezeAI/termux-jobs/issues).
На 2026-11-03 private vulnerability reporting в GitHub выключен; отдельный закрытый
адрес не опубликован. Для чувствительной проблемы оставьте только несекретное
уведомление и запросите у HezeAI закрытый способ передачи подробностей. Не публикуйте
ключи, приватные журналы, архивы окружений, чужие данные или чувствительные детали
эксплуатации в Issues. Этот документ не выдумывает приватный канал.

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
and transfer selected commands/files. Media/model/graphics downloads contact their
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
from HezeAI's named phone checks. There is no confirmed independent security audit
of this version. Source availability, test counts and a matching signer are not
security guarantees. Historical reports retain their original scope; consult the
current release report for active limits.

The existing contact is [project GitHub Issues](https://github.com/HezeAI/termux-jobs/issues).
GitHub private vulnerability reporting was disabled when checked on2026-11-03;
no separate confidential address is published. For sensitive findings, post only a
non-sensitive notification and ask HezeAI for a private handoff before sharing
technical details. Do not publish keys, private logs, environment archives, other
people's data or sensitive exploit details. No private address is invented here.

## Reviewed implementation / Проверенные пути кода

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
