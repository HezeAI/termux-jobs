> Historical experimental feature: removed from the current v1.5.22-test source/APK. This document is retained as research evidence, not current functionality. See [removal notes](RELEASE-v1.5.22-test.md).

# Historical private local-AI previews

These development notes are not the public feature list. Public v1.5.0 keeps local models disabled while testing continues. See LOCAL-AI.md for experimental implementation details.

## v1.4.30-test

Local AI Engine settings: CPU / experimental OpenCL GPU for verified Qwen3.5 2B on Snapdragon 8 Elite. GPU hides CPU thread controls; CPU remains default. Tool error cards preserve original calls and precise parameter errors; clearer empty-write vs deletion results. [Details](docs/fork/RELEASE-v1.4.30-test.md).

## v1.4.29-test

Experimental opt-in native Qwen tool format for catalog Qwen3.5 2B / llama.cpp. Engine settings retains current JSON by default. Existing authorization and native inference libraries are unchanged; device testing is needed to assess instruction compliance. [Details](docs/fork/RELEASE-v1.4.29-test.md).

## v1.4.28-test

Local AI: editable behavior/rights prompts, dedicated Ubuntu default for new sessions, explicit optional Termux command access, and stronger multi-step guidance. Unsaved-session closing separates history from presets. New Chat histories browser supports local chats and native Codex viewing/deletion where supported. No automatic data reset or environment switch for existing sessions.

## v1.4.27-test

Bundled local-assistant operating instructions are loaded automatically for every turn: act within current permissions, verify filesystem access and results, handle missing dependencies, and verify deletion targets. Existing rights and project boundaries remain enforced. Model compliance requires device testing.

## v1.4.26-test

Local AI Environment settings separate tools from engine location: Embedded Android, native Termux, or the same dedicated Local-AI Ubuntu. Experimental CPU runners share model files/history and keep existing permissions. Includes compact file-list context and clearer tool-call draft status.

## v1.4.25-test

All local models: compact file-list metadata for inference, full raw results retained; instructions recommend filters and requested count for newest-file tasks. Larger requested lists remain available.

## v1.4.24-test

Local AI: shorter system/tool instructions and smaller old-result excerpts for lower input cost; full saved history retained. Saved preset menu order and model-picker palette aligned with other settings.

## v1.4.23-test

Local AI diagnostics now reports input token composition per inference pass, using the model tokenizer for llama totals and labelled estimates for components. Session/preset button shortened to Engine settings.

## v1.4.22-test

Local AI: Engine settings now groups processor mode and CPU thread selection. Auto preserves the previous four-thread cap; manual options are 2/4/6/8 where supported. Existing chats and presets are retained.

Тестовая **v1.4.21-test**: отдельный тест обработки входа llama.cpp — длина,
2/4/6/8 потоков и размеры порций, с CPU-временем и отчётом по каждому прогону.
[Как проверить](docs/fork/RELEASE-v1.4.21-test.md). Без публикации.

Тестовая **v1.4.20-test**: исправление моделью неверных параметров инструментов,
пометки кэша и защита от повторного чтения; понятное название ARM-ускорения.
[Изменения и проверка](docs/fork/RELEASE-v1.4.20-test.md). Без публикации.

Тестовая **v1.4.19-test**: выбор CPU-движка llama.cpp — автоматическая ARM-оптимизация
или совместимая сборка; выбранный вариант виден в диагностике.
[Условия и проверка](docs/fork/RELEASE-v1.4.19-test.md). Без публикации.

Тестовая **v1.4.18-test**: уточнение папки для коротких запросов о последних файлах,
поиск скриншотов без выдуманного просмотра и компактные прошлые результаты чтения.
[Изменения и ограничения](docs/fork/RELEASE-v1.4.18-test.md). Без публикации.

Тестовая **v1.4.17-test**: диагностика каждого обращения локального ИИ к модели,
включая промежуточные проходы между инструментами.
[Изменения и ограничения](docs/fork/RELEASE-v1.4.17-test.md). Без публикации.

Тестовая **v1.4.16-test**: отдельные замеры обработки входных токенов и первого
шага генерации, повторное использование совпадающего префикса llama.cpp в RAM.
[Условия, проверка и ограничения](docs/fork/RELEASE-v1.4.16-test.md). Без публикации.

Тестовая **v1.4.15-test**: контекст и максимальный ответ в настройках локальных ИИ,
статусы этапов и диагностика времени, ускоренное «найди … и прочитай».
[Изменения и ограничения](docs/fork/RELEASE-v1.4.15-test.md). Без публикации.

Локальная тестовая **v1.4.14-test**: сокращение контекста локальных ИИ с уведомлением,
поиск в истории текущего чата и быстрый показ прочитанного текста без пересказа.
[Изменения и ограничения](docs/fork/RELEASE-v1.4.14-test.md). Без публикации.

Локальная тестовая **v1.4.13-test**: короткие запросы «последний TXT в загрузках»
проверяют нужную папку, сортируют по дате и читают найденный текст по запросу.
[Изменения и ограничения](docs/fork/RELEASE-v1.4.13-test.md). Без публикации.

Локальная проверка **v1.3.4**: стабильные панели при поиске команд/заметок
и освобождение закрытых окон пакетов/файлов. Диагностика не менялась.
[Изменения](docs/fork/RELEASE-v1.3.4.md). Публичный выпуск пока v1.3.3.

**v1.3.3 — Обязательное обновление:** исправлены вылеты при очистке временных
файлов и обработке crash-log из-за ошибки зависимостей Guava.
**Android8.0+**. Обновляйте поверх, без очистки данных.
[Изменения](docs/fork/RELEASE-v1.3.3.md).


### Local AI preview (not published)

The local `v1.4.12-test` build adds optional on-device chat, per-session project tools, a pinned model
catalogue and custom GGUF/LiteRT-LM imports. See [setup, compatibility and current
limitations](docs/fork/LOCAL-AI.md). GPU requires device checks; Qualcomm NPU is
not operational in this preview. Existing public stable release remains unchanged.
