# Продолжение — stable v2.1.3 / 1485

Checkout: `/root/termux-fork/.verification/rollback-live-actions/source`.
Релизная ветка `fork/base`, тег `v2.1.3`; разработка сохранена из `work/opencode-declined-status`.
Единственная память: `/root/termux-fork/LEARNING.md`; читать при начале работы.

Test8 принят HezeAI на телефоне 2026-11-04: всё работает, прозрачность устраивает.
Разрешены подготовка/публикация stable, commit/push/tag и загрузка APK.
Установка APK и перезапуск рабочих сессий не выполняются. Старые APK/теги/Components
не заменяются. Основа сравнения — последний опубликованный stable v2.1.2.

Контрольная точка выпуска: подписанный APK, RU/EN update.json, SHA256SUMS,
коммит/тег v2.1.3. Точный публичный статус и результаты скачивания — на
[странице релиза](https://github.com/HezeAI/termux-jobs/releases/tag/v2.1.3).
Порядок: [RELEASING](docs/fork/RELEASING.md); артефакт/проверки:
[VALIDATION-v2.1.3](docs/fork/VALIDATION-v2.1.3.md).

Принятые phone permissions всех четырёх агентов и fake-key/API/retry Gemini
не повторять без изменения значимых условий. [Текущий аудит](docs/fork/SECURITY-AUDIT-STAGE2.md)
отделяет стенд и сообщения разработчика. Android handoff/Keystore/crash и
export/restore копий на устройстве остаются непроверенными по согласованному охвату.
Настоящий Gemini API/Vertex вход и все legacy/manual/TUI пути не закрыты целиком.

Следующий цикл после релиза — накопление вложений/контекста: [исследование](docs/fork/MEDIA-CONTEXT-INVESTIGATION.md).
Проблема не исправлена, не удалять историю/оригиналы. MCP/плагины исключены.
Перед тематической работой читать только относящиеся разделы и навигатор AGENTS.md.
