# Проверки текущего релиза v1.0.18

[Полный отчёт](VALIDATION-v1.0.18.md):288 JUnit/Robolectric tests,
release/lintVital,581 RU/EN строк, Gemini controller suites, ZIP/package/version/
ABI/сертификат/SHA256. com.termux,1.0.18/code1055,Android7+/ARM64,библиотекаv8.

APK36637078 bytes. SHA256:
`30b11320d082f8a8f462daea8c635e53a3588351d9cac53e14bec185bdf1f54b`.
Certificate SHA256:
`dd37d983cd2496ebe49c7e88ddbc0088f311d38b8a8503e4392c81b48a11a644`.
Пользователь запросил публикацию. Агент APK не устанавливал. Нет подробного
протокола проверки всех OAuth/моделей/истории, Android IME/DPI/жестов и экспорта.
AI Agents экспериментальны; OC-001 не исправлен. См. [known issues](KNOWN-ISSUES.md).

Default versionName исходников и helper сборки актуализированы до1.0.18.
Финальный APK уже собран с явным override1.0.18; это не изменение effective версии
или исполняемого кода. Документационные/metadata правки не требовали повторной
сборки неизменённого APK. Контрольные суммы assets и исходников приложений
сверены с подготовленной сборкой (с поправкой на обновлённый default metadata).

[Релиз](RELEASE-v1.0.18.md) · [Инструкция](USAGE.md) · [AI Agents](AI-AGENTS.md)

## Исторические проверки

[v1.0.4](VALIDATION-v1.0.4.md), [v1.0](VALIDATION-v1.0.md),
[v0.2.17](VALIDATION-v0.2.17.md). Локальные этапы v1.0.5–17 сохранены отдельными
VALIDATION-v*.md; это результаты прежних APK, не доказательства новых сценариев.
