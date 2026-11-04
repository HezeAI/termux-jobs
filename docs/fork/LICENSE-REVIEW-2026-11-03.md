# License and attribution review — 2026-11-03

This is a dated evidence record for the release and the existing published
Components. It is not an independent legal/security audit or a blanket compliance
certificate. Original Termux/third-party copyright and license texts remain intact.
HezeAI maintains the fork; upstream authors retain their work and rights.

## Confirmed and corrected

| Area | Exact evidence / result |
| --- | --- |
| Termux application | GPL-3.0-only plus LICENSE.md/termux-shared exceptions; COPYING and source copyright headers retained. Fork source, patches and build materials are published in the release tag. |
| Resolved release dependencies | 82 Maven modules, including metadata/BOM/annotation modules, inventoried from the actual release graph. Exact versions and inherited POM licenses: [inventory](third-party/release-dependencies-v2.1.2.json). This is not a claim that every module contributes DEX. |
| Required notices | The prepared APK lacked the checker-qual3.43.0 MIT copyright/permission notice. That license requires retaining the notice in copies/substantial portions. [THIRD_PARTY_NOTICES](../../THIRD_PARTY_NOTICES.md) now retains that exact text and other upstream NOTICE/license texts; the same notice file is included in the APK and About. This is a concrete packaging correction, not a claim of a security exploit. |
| About and README | HezeAI/fork authorship is separated from Termux/upstream authors. Independent unofficial status and lack of endorsement are explicit. Fork contacts precede clearly labeled upstream references. No invented private reporting address. |
| Termux:X11 | GPL-3.0 core at fa3a8b430e2896a19f44c99a9cb056254615ae06; exact Git/submodule sources and build scripts are separately [published](https://github.com/HezeAI/termux-jobs-components/releases/tag/x11-core-fa3a8b430e28), with GPL text. Native core is unchanged and credited upstream. |
| Private proot-distro | Bundled5.9.0 Python source/runtime ZIP preserves its upstream LICENSE and source modules. Launcher changes are included in this repository. This executable source-format runtime is distinct from the optional source archive for Ubuntu packages. |
| Bootstrap | Unmodified aarch64 bootstrap-2025.03.28-r1+apt-android-7, SHA-256 c8d702b6f742935001c37cda81b8ac69504a95d5cf28f2899532dd8cd4b057eb. All75 installed package identities map to build/subpackage recipes at exact upstream commit5a6d1c1eb868795dce83a6c269387b9f82d21805. [Exact recipe source tree](https://github.com/termux/termux-packages/tree/5a6d1c1eb868795dce83a6c269387b9f82d21805). Embedded share/LICENSES and package notices remain. This recipe mapping does not claim a fresh rebuild or checksum verification of every original upstream tarball. |
| Mesa/codec/keyboard payloads | Existing Mesa26.1.6 VirGL build inputs/notices, xkeyboard-config2.48-1 COPYRIGHT, Cisco/Debian OpenH2642.6.0+dfsg-2 and GStreamer per-file notices are retained in the APK/source tree. See [component-specific provenance](THIRD-PARTY.md). |

## Published Ubuntu AI: complete package/source-file coverage

Concrete binary: [ubuntu-ai-arm64.oci.tar](https://github.com/HezeAI/termux-jobs-components/releases/download/ubuntu-ai-base-arm64-20261102/ubuntu-ai-arm64.oci.tar),
41,144,320bytes, SHA-256 `c08828893609894e5ab6d80f146961961148afde007ce904292418d8fcd011e0`.
The separately published ordinary Ubuntu base contains the same upstream blobs.

The earlier Components documentation offered only general distribution source
locations. That was insufficient evidence of exact matching source access. The
supplement now maps **all87 installed packages to58 source-package versions**.
All58 DSC files were downloaded and their Source/Version matched; **all127 files
listed under Checksums-Sha256 were downloaded**, totaling348,117,257bytes.
Each actual byte length and SHA-256 matched its DSC. No referenced file was skipped.
DSC signatures and reproducible package builds were not independently verified.

[Separate corresponding-source archive](https://github.com/HezeAI/termux-jobs-components/releases/download/ubuntu-ai-base-arm64-20261102/ubuntu-ai-corresponding-sources-20261103.tar):
350,515,200bytes, SHA-256 `381789f23e732160e671f064b4e13f4f0ffa99b29b3b47199bed6d9970e718cf`.
It includes all source files, distribution patches/build packaging, DSC files,
all87 package copyright records and common-license texts. The
[inventory](https://github.com/HezeAI/termux-jobs-components/releases/download/ubuntu-ai-base-arm64-20261102/ubuntu-ai-source-inventory-20261103.json)
records individual licenses/conditions rather than assigning one license to the
whole image. Non-DEP5 copyright texts remain verbatim even where machine-normalized
license identifiers are unavailable. GPL source access, LGPL library sources and
permissive attribution must not be treated as interchangeable conditions.

All three new source/manifest/checksum resources were downloaded anonymously and
matched prepared bytes/SHA-256. Source archives are not included in the APK or
automatic application download catalog. All129 previously published Components
assets retained the same IDs, names, URLs, sizes and SHA-256; no existing resource
or release was replaced. The application's real image downloader fetched the
original41,144,320byte image into a fresh isolated cache and verified its expected
hash after the source addition. All10 pinned image catalog entries match public
asset sizes/digests. No environment was installed for that check.

## Remaining component-specific questions

This pass does not claim complete per-package license/source clearance for the
other distribution images or XFCE package sets. Concrete outstanding files include
`debian-arm64.oci.tar`, `alpine-arm64.oci.tar`, `fedora-arm64.oci.tar`,
`opensuse-arm64.oci.tar`, `void-arm64.oci.tar`, `kali-arm64.oci.tar`,
`archlinux-arm64.tar.xz`, `manjaro-arm64.tar.gz` and the published
`xfce-<target>-arm64.tar` archives. Their binary/package manifests and embedded
notices exist. Exact copyleft source/patch/build-package correspondence and archive
checksum access still need individual verification; generic repository links are
not counted as that work. The87/58/127 result applies only to the reviewed Ubuntu
image, not these sets.

`Turnip-a6xx-a7xx-a8xx.zip` is the StevenMXZ-supplied distribution with
GameHub/Mesa26.3.0-devel-b00479181d metadata. The fork does not claim authorship.
Full compiler-input/subcomponent provenance for that imported binary remains
unverified. Existing Mesa notices do not by themselves prove every included
subcomponent's licensing. Other Mesa/libhybris/WSI source/build archives are listed
separately in their exact component manifests.

Qwen model notices and exact source repositories/revisions/Apache-2.0 metadata
remain in the model manifest and published notices. This pass inventories those
records rather than re-downloading all weights or certifying training-data rights.
Fonts/icons/themes inside distribution packages retain their individual licenses;
no blanket “all resources are ours” statement is made. App-drawn agent effects are
not official animations or endorsements.

The [Components asset inventory and dated review](https://github.com/HezeAI/termux-jobs-components/blob/main/LICENSE-REVIEW-20261103.md)
identify exact assets and remaining scope. Any concrete unmet distribution
condition must be addressed for that file before its further publication, with
additional notices/sources rather than silent binary replacement. Unverified
coverage is not a finding that every remaining package is unlawful, and is not a
compliance PASS. The [GPLv3 source-access requirement](https://www.gnu.org/licenses/gpl-3.0.en.html#section6)
and [GPLv2 distribution conditions](https://www.gnu.org/licenses/old-licenses/gpl-2.0.html#section3)
are authoritative; a project landing page alone does not establish source identity.

## Проверка и ограничения — RU

Проверены версии/лицензии82 модулей Maven и сохранение уведомлений в APK. Исправлен
конкретный пропуск MIT-уведомления checker-qual3.43.0. Для опубликованного образа
Ubuntu AI охвачены все87 установленных пакетов,58 исходных пакетов и127 файлов
исходников; размеры и SHA-256 всех скачанных файлов совпали. Сохранены отдельные
copyright и тексты лицензий образа. Подписи DSC и повторная сборка пакетов не проверены.

Прежние129 ресурсов Components сохранены. Новые исходники опубликованы отдельно,
без включения в APK/автозагрузку. Для остальных названных дистрибутивов/XFCE,
всех внутренних компонентов импортированного драйвера и обучения моделей полный
аудит не выполнен. Нельзя переносить результат Ubuntu на них или объявлять полное
соблюдение лицензий. Исторические отчёты не переписаны задним числом.


## Урок: 2026-11-03 — Соответствующие исходники и уведомления проверяются по распространяемым байтам

Перенесено 2026-11-04 без изменения исторических результатов. Это доказательства
на дату проверки; актуальный статус — в [основном плане](SECURITY-AUDIT-STAGE2.md).

### 2026-11-03 — Соответствующие исходники и уведомления проверяются по распространяемым байтам

- Контекст: лицензионная проверка APK и уже опубликованных Components при выпуске v2.1.2.
- Вывод: наличие MIT/NOTICE в Maven JAR или POM не подтверждает его наличие в APK. У checker-qual3.43.0 отсутствовал обязательный текст; теперь точное уведомление входит в THIRD_PARTY_NOTICES и About. Проверять готовый APK, включая сторонние исключения и исходники, а не только зависимости Gradle.
- Применимость: новые/обновлённые зависимости и распространяемые компоненты. Архив соответствующих исходников публиковать отдельно; он отличается от исполняемого Python-кода приложения в ZIP. Не менять URL/байты ранее опубликованного бинарного ресурса ради уведомления.
- Подтверждение: для Ubuntu AI все87 пакетов сопоставлены58 исходным версиям; все127 файлов из DSC скачаны и сверены по размеру/SHA. Отдельный архив опубликован и скачан анонимно с проверкой SHA;129 прежних ресурсов сохранили идентичности. Это не полный аудит остальных компонентов. См. docs/fork/LICENSE-REVIEW-2026-11-03.md в активном checkout.
