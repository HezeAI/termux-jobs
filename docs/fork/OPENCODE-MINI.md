# OpenCode frontend jobs1, локальная v1.0.12

**Исторический эксперимент.** В v1.0.13 отдельный frontend и его установка убраны
по просьбе пользователя. Установленные ранее файлы не удаляются агентом; текущий
адаптер их не использует. Инструкции ниже относятся только к v1.0.12.

У штатного mini --replay объединяет показ истории при входе и reset/hydrate при
изменении размера. Пользователь подтвердил частичное улучшение v1.0.11, но при IME,
возвращении из другой сессии и Home/Commands история повторно выводилась целиком.

Источник: anomalyco/opencode v2.0.28, commit
026b106bc5d41b679ffc413264229c340871e4d3, лицензия MIT. Использован отдельный
frontend, собранный из исходников; официальный бинарник/сервер не патчился.
Изменение видно в patches/opencode-v2.0.28-resize-replay.patch:

- MiniHost.terminal получает optional replayOnResize (отсутствие означает прежнее поведение).
- CLI host читает TERMUX_JOBS_DISABLE_RESIZE_REPLAY=1.
- Runtime пропускает только replayOnResize; initial replay, theme и footer resize остаются.
- Адаптер задаёт флаг только процессу mini, официальный serve использует прежний exe.
- При смене модели новый mini использует тот же private server и native chat ID.

## Подготовка на устройстве

APK содержит opencode-mini-v2.0.28-jobs1.bundle: gzip tar только с бинарником и MIT
LICENSE. Расширение .bundle нужно потому, что Android merge-assets распаковывает
файлы .gz и удаляет суффикс. AiAgents.prepare копирует неизменяемый bundle один раз
в private .ai-control. Обновляемые небольшие JS/SH файлы по-прежнему заменяются атомарно.

После обычного bootstrap запускается mini-v1.sh. SHA-256 bundle и бинарника
проверяются, установка защищена flock. Временный каталог создаётся только под
~/.local/share/termux-jobs/agents; rename атомарный, staging очищается при ошибке.
Существующая повреждённая/другая папка не перезаписывается. Повторный вход проверяет
имеющийся бинарник и не заменяет его. Не используются npm/system package upgrades.

Frontend: ~/.local/share/termux-jobs/agents/opencode-mini-v2.0.28-jobs1/opencode-mini.
Официальный bin/opencode, connection XDG data/config/state/cache, авторизация, история
и Codex остаются в прежних местах. Во время этой работы агент готовил runtime только
в репозитории, не в пользовательской установленной среде.

Bundle71048636 bytes, runtime177588520 bytes. На первой подготовке потребуется
место для bundle, staging и runtime; повторная подготовка использует cache. APK
становится больше примерно на71MB. Для обновления поведения нужно заново запустить mini.

## Повторная сборка

Проверочный source/cache: .tools/ai-agents/resize-source. Source tar получен с
официального тега; receipt содержит SHA и ref. Bun1.4.2 ARM64 получен из официального
@oven/bun-linux-aarch64 и проверен npm integrity. Dependencies из bun.lock ставились
внутри source, с --frozen-lockfile --ignore-scripts, filters @opencode/cli и opencode.
Системные инструменты и SDK не устанавливались/не менялись.

Применить patch к чистому указанному commit; из packages/cli выполнить:

```sh
OPENCODE_VERSION=2.0.28 OPENCODE_CHANNEL=latest bun run script/build.ts \
  --single --skip-install --skip-web-ui --outdir=/absolute/path/inside/repository/dist
```

Web UI не включён в этот frontend; приложение запускает его исключительно как mini,
web/server продолжает предоставлять официальный runtime. Upstream build проверяет
architecture и отсутствие screenshot payload. Создать gzip tar с opencode-mini и
LICENSE, проверить SHA, обновить manifest и mini shell hashes. Если бинарник изменился,
использовать новый immutable revision/filename (jobs2 и т.д.), чтобы не переиспользовать
кэш прежнего APK. Generated bundle исключён из Git; публикация/CI должны отдельно
предусмотреть его сборку или проверенную загрузку. Локальная сборка не опубликована.

## Проверки и ограничения

31 upstream runtime/queue/boot tests, CLI typecheck, native resume/model/resize и
install/reuse/corruption paths. Android tests и final APK — VALIDATION-v1.0.12.md.
Не выдавать native/parser tests за проверку реальных Android кадров/клавиатуры.
Возможна отложенная перерисовка отдельных footer элементов без history replay.
Выделение текста и правило auto-scroll отдельно не менялись. Стабильный1s возврат
на телефоне до тестирования нового APK не считается устранённым.
