# Third-party notices — Termux Job’s

Review date: 2026-11-03. The fork is maintained by HezeAI; original Termux and
third-party code remain the work of their respective authors. No endorsement is
claimed. Licenses below apply to their components, not automatically to every
file in this aggregate. Original copyright headers and license files are retained.

RU: Termux Job’s — неофициальный независимый форк. HezeAI поддерживает изменения
форка; это не авторство Termux, библиотек, дистрибутивов, шрифтов и моделей.
Команда Termux и владельцы названий/значков не заявляются одобрившими проект.

## Application and native sources

- Termux: GPL-3.0-only, with the exceptions in LICENSE.md and termux-shared/LICENSE.md.
  Exact fork sources and changes: https://github.com/HezeAI/termux-jobs/tree/v2.1.2
  Build instructions: FORK.md and scripts/build-local-arm64.sh. Original Termux authors
  retain their copyright. Terminal-emulator/view include Jack Palevich and Android
  Terminal Emulator contributors' Apache-2.0 code and retained source headers.
- Termux:X11 native core: commit fa3a8b430e2896a19f44c99a9cb056254615ae06, GPL-3.0,
  upstream Termux:X11 contributors. Exact source/submodule archive and build scripts:
  https://github.com/HezeAI/termux-jobs-components/releases/download/x11-core-fa3a8b430e28/termux-x11-fa3a8b430e28-sources.tar.gz
  Fork Android integration is in x11/src; pinned native input is unchanged.
- proot-distro5.9.0: Termux/proot-distro contributors, upstream LICENSE in the bundled
  ubuntu-ai-proot-distro-5.9.0.zip, with complete shipped Python module sources.
  Exact upstream source: https://github.com/termux/proot-distro/tree/v5.9.0
  The fork's restricted local-archive launcher is app/src/main/assets/ubuntu-ai-installer.py.
- Embedded Termux bootstrap: bootstrap-2025.03.28-r1+apt-android-7/aarch64, unchanged
  upstream archive. Its packages retain individual licenses; GPL/LGPL source-package
  and exact-recipe coverage is tracked separately in the license review, not inferred
  from a generic upstream homepage.
- X11 keyboard data: xkeyboard-config2.48-1, HPND/MIT; COPYRIGHT is preserved in the
  keyboard archive. Mesa VirGL26.1.6, BSD/MIT component terms: existing in-APK
  x11-virgl-alpine-LICENSE.txt and third-party/virgl-client build/source metadata.
- OpenH2642.6.0+dfsg-2 (Cisco/Debian, BSD-2-Clause) and GStreamer OpenH264 bridge:
  existing assets/licenses/OpenH264-copyright.txt and GStreamer-OpenH264-copyright.txt
  retain package/per-file notices. See third-party/openh264 and gstreamer-openh264.
- Agent CLI/model/graphics archives downloaded later are separate components, not
  authorship of this fork. Their exact upstream revisions, notices and pending source
  checks are described in docs/fork/THIRD-PARTY.md and the Components license review.
  System/selected user fonts are not relicensed by this application. Agent icon
  treatments are app-drawn identifiers/effects, not official endorsements or mascots.

## Resolved release Maven modules

This is the resolved release graph, including metadata/BOM and annotation modules;
it is not a claim that every module contributes executable DEX. Exact versions come
from the APK build graph. License metadata includes inherited POM licenses; retained
upstream notices follow the table. No third-party license is changed by this table.

| Module | Version | License metadata | Exact artifact metadata |
| --- | --- | --- | --- |
| androidx.annotation:annotation | 1.8.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/annotation/annotation/1.8.1/annotation-1.8.1.pom) |
| androidx.annotation:annotation-jvm | 1.8.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/annotation/annotation-jvm/1.8.1/annotation-jvm-1.8.1.pom) |
| org.jetbrains.kotlin:kotlin-stdlib | 1.8.0 | The Apache License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/1.8.0/kotlin-stdlib-1.8.0.pom) |
| org.jetbrains.kotlin:kotlin-stdlib-common | 1.8.0 | The Apache License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-common/1.8.0/kotlin-stdlib-common-1.8.0.pom) |
| org.jetbrains:annotations | 13.0 | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/jetbrains/annotations/13.0/annotations-13.0.pom) |
| androidx.exifinterface:exifinterface | 1.4.2 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/exifinterface/exifinterface/1.4.2/exifinterface-1.4.2.pom) |
| org.jspecify:jspecify | 1.0.0 | The Apache License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.pom) |
| androidx.media3:media3-exoplayer | 1.5.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/media3/media3-exoplayer/1.5.1/media3-exoplayer-1.5.1.pom) |
| androidx.collection:collection | 1.2.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/collection/collection/1.2.0/collection-1.2.0.pom) |
| androidx.core:core | 1.9.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/core/core/1.9.0/core-1.9.0.pom) |
| androidx.annotation:annotation-experimental | 1.3.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/annotation/annotation-experimental/1.3.1/annotation-experimental-1.3.1.pom) |
| androidx.concurrent:concurrent-futures | 1.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/concurrent/concurrent-futures/1.0.0/concurrent-futures-1.0.0.pom) |
| com.google.guava:listenablefuture | 9999.0-empty-to-avoid-conflict-with-guava | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/com/google/guava/listenablefuture/9999.0-empty-to-avoid-conflict-with-guava/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.pom) |
| androidx.lifecycle:lifecycle-runtime | 2.3.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime/2.3.1/lifecycle-runtime-2.3.1.pom) |
| androidx.arch.core:core-runtime | 2.1.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/arch/core/core-runtime/2.1.0/core-runtime-2.1.0.pom) |
| androidx.arch.core:core-common | 2.1.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/arch/core/core-common/2.1.0/core-common-2.1.0.pom) |
| androidx.lifecycle:lifecycle-common | 2.3.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-common/2.3.1/lifecycle-common-2.3.1.pom) |
| androidx.versionedparcelable:versionedparcelable | 1.1.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/versionedparcelable/versionedparcelable/1.1.1/versionedparcelable-1.1.1.pom) |
| androidx.core:core-ktx | 1.9.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/core/core-ktx/1.9.0/core-ktx-1.9.0.pom) |
| androidx.media3:media3-common | 1.5.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/media3/media3-common/1.5.1/media3-common-1.5.1.pom) |
| org.jetbrains.kotlin:kotlin-bom | 1.8.0 | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-bom/1.8.0/kotlin-bom-1.8.0.pom) |
| org.jetbrains.kotlin:kotlin-stdlib-jdk8 | 1.8.0 | The Apache License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-jdk8/1.8.0/kotlin-stdlib-jdk8-1.8.0.pom) |
| org.jetbrains.kotlin:kotlin-stdlib-jdk7 | 1.8.0 | The Apache License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib-jdk7/1.8.0/kotlin-stdlib-jdk7-1.8.0.pom) |
| com.google.guava:guava | 33.3.1-jre | Apache License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/com/google/guava/guava/33.3.1-jre/guava-33.3.1-jre.pom) |
| com.google.guava:failureaccess | 1.0.2 | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.pom) |
| com.google.code.findbugs:jsr305 | 3.0.2 | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.pom) |
| org.checkerframework:checker-qual | 3.43.0 | The MIT License | [POM](https://repo.maven.apache.org/maven2/org/checkerframework/checker-qual/3.43.0/checker-qual-3.43.0.pom) |
| com.google.errorprone:error_prone_annotations | 2.28.0 | Apache 2.0 | [POM](https://repo.maven.apache.org/maven2/com/google/errorprone/error_prone_annotations/2.28.0/error_prone_annotations-2.28.0.pom) |
| com.google.j2objc:j2objc-annotations | 3.0.0 | Apache License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/com/google/j2objc/j2objc-annotations/3.0.0/j2objc-annotations-3.0.0.pom) |
| androidx.media3:media3-container | 1.5.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/media3/media3-container/1.5.1/media3-container-1.5.1.pom) |
| androidx.media3:media3-database | 1.5.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/media3/media3-database/1.5.1/media3-database-1.5.1.pom) |
| androidx.media3:media3-datasource | 1.5.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/media3/media3-datasource/1.5.1/media3-datasource-1.5.1.pom) |
| androidx.media3:media3-decoder | 1.5.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/media3/media3-decoder/1.5.1/media3-decoder-1.5.1.pom) |
| androidx.media3:media3-extractor | 1.5.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/media3/media3-extractor/1.5.1/media3-extractor-1.5.1.pom) |
| androidx.media3:media3-ui | 1.5.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/media3/media3-ui/1.5.1/media3-ui-1.5.1.pom) |
| androidx.media:media | 1.7.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/media/media/1.7.0/media-1.7.0.pom) |
| androidx.recyclerview:recyclerview | 1.3.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/recyclerview/recyclerview/1.3.0/recyclerview-1.3.0.pom) |
| androidx.customview:customview | 1.1.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/customview/customview/1.1.0/customview-1.1.0.pom) |
| androidx.customview:customview-poolingcontainer | 1.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/customview/customview-poolingcontainer/1.0.0/customview-poolingcontainer-1.0.0.pom) |
| androidx.drawerlayout:drawerlayout | 1.1.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/drawerlayout/drawerlayout/1.1.1/drawerlayout-1.1.1.pom) |
| androidx.preference:preference | 1.1.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/preference/preference/1.1.1/preference-1.1.1.pom) |
| androidx.appcompat:appcompat | 1.3.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/appcompat/appcompat/1.3.1/appcompat-1.3.1.pom) |
| androidx.cursoradapter:cursoradapter | 1.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/cursoradapter/cursoradapter/1.0.0/cursoradapter-1.0.0.pom) |
| androidx.activity:activity | 1.2.4 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/activity/activity/1.2.4/activity-1.2.4.pom) |
| androidx.lifecycle:lifecycle-viewmodel | 2.3.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel/2.3.1/lifecycle-viewmodel-2.3.1.pom) |
| androidx.savedstate:savedstate | 1.1.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/savedstate/savedstate/1.1.0/savedstate-1.1.0.pom) |
| androidx.lifecycle:lifecycle-viewmodel-savedstate | 2.3.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-viewmodel-savedstate/2.3.1/lifecycle-viewmodel-savedstate-2.3.1.pom) |
| androidx.lifecycle:lifecycle-livedata-core | 2.3.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-livedata-core/2.3.1/lifecycle-livedata-core-2.3.1.pom) |
| androidx.tracing:tracing | 1.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/tracing/tracing/1.0.0/tracing-1.0.0.pom) |
| androidx.fragment:fragment | 1.3.6 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/fragment/fragment/1.3.6/fragment-1.3.6.pom) |
| androidx.viewpager:viewpager | 1.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/viewpager/viewpager/1.0.0/viewpager-1.0.0.pom) |
| androidx.loader:loader | 1.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/loader/loader/1.0.0/loader-1.0.0.pom) |
| androidx.lifecycle:lifecycle-livedata | 2.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-livedata/2.0.0/lifecycle-livedata-2.0.0.pom) |
| androidx.appcompat:appcompat-resources | 1.3.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/appcompat/appcompat-resources/1.3.1/appcompat-resources-1.3.1.pom) |
| androidx.vectordrawable:vectordrawable | 1.1.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/vectordrawable/vectordrawable/1.1.0/vectordrawable-1.1.0.pom) |
| androidx.vectordrawable:vectordrawable-animated | 1.1.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/vectordrawable/vectordrawable-animated/1.1.0/vectordrawable-animated-1.1.0.pom) |
| androidx.interpolator:interpolator | 1.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/interpolator/interpolator/1.0.0/interpolator-1.0.0.pom) |
| io.noties.markwon:core | 4.6.2 | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/io/noties/markwon/core/4.6.2/core-4.6.2.pom) |
| com.atlassian.commonmark:commonmark | 0.13.0 | BSD 2-Clause License | [POM](https://repo.maven.apache.org/maven2/com/atlassian/commonmark/commonmark/0.13.0/commonmark-0.13.0.pom) |
| io.noties.markwon:ext-strikethrough | 4.6.2 | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/io/noties/markwon/ext-strikethrough/4.6.2/ext-strikethrough-4.6.2.pom) |
| com.atlassian.commonmark:commonmark-ext-gfm-strikethrough | 0.13.0 | BSD 2-Clause License | [POM](https://repo.maven.apache.org/maven2/com/atlassian/commonmark/commonmark-ext-gfm-strikethrough/0.13.0/commonmark-ext-gfm-strikethrough-0.13.0.pom) |
| io.noties.markwon:linkify | 4.6.2 | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/io/noties/markwon/linkify/4.6.2/linkify-4.6.2.pom) |
| io.noties.markwon:recycler | 4.6.2 | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/io/noties/markwon/recycler/4.6.2/recycler-4.6.2.pom) |
| com.atlassian.commonmark:commonmark-ext-gfm-tables | 0.13.0 | BSD 2-Clause License | [POM](https://repo.maven.apache.org/maven2/com/atlassian/commonmark/commonmark-ext-gfm-tables/0.13.0/commonmark-ext-gfm-tables-0.13.0.pom) |
| com.google.android.material:material | 1.4.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/com/google/android/material/material/1.4.0/material-1.4.0.pom) |
| androidx.cardview:cardview | 1.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/cardview/cardview/1.0.0/cardview-1.0.0.pom) |
| androidx.coordinatorlayout:coordinatorlayout | 1.1.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/coordinatorlayout/coordinatorlayout/1.1.0/coordinatorlayout-1.1.0.pom) |
| androidx.constraintlayout:constraintlayout | 2.0.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/constraintlayout/constraintlayout/2.0.1/constraintlayout-2.0.1.pom) |
| androidx.constraintlayout:constraintlayout-solver | 2.0.1 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/constraintlayout/constraintlayout-solver/2.0.1/constraintlayout-solver-2.0.1.pom) |
| androidx.dynamicanimation:dynamicanimation | 1.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/dynamicanimation/dynamicanimation/1.0.0/dynamicanimation-1.0.0.pom) |
| androidx.legacy:legacy-support-core-utils | 1.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/legacy/legacy-support-core-utils/1.0.0/legacy-support-core-utils-1.0.0.pom) |
| androidx.documentfile:documentfile | 1.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/documentfile/documentfile/1.0.0/documentfile-1.0.0.pom) |
| androidx.localbroadcastmanager:localbroadcastmanager | 1.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/localbroadcastmanager/localbroadcastmanager/1.0.0/localbroadcastmanager-1.0.0.pom) |
| androidx.print:print | 1.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/print/print/1.0.0/print-1.0.0.pom) |
| androidx.transition:transition | 1.2.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/transition/transition/1.2.0/transition-1.2.0.pom) |
| androidx.viewpager2:viewpager2 | 1.0.0 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/viewpager2/viewpager2/1.0.0/viewpager2-1.0.0.pom) |
| org.lsposed.hiddenapibypass:hiddenapibypass | 6.1 | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/lsposed/hiddenapibypass/hiddenapibypass/6.1/hiddenapibypass-6.1.pom) |
| androidx.window:window | 1.0.0-alpha09 | The Apache Software License, Version 2.0 | [POM](https://dl.google.com/dl/android/maven2/androidx/window/window/1.0.0-alpha09/window-1.0.0-alpha09.pom) |
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.5.0 | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-android/1.5.0/kotlinx-coroutines-android-1.5.0.pom) |
| org.jetbrains.kotlinx:kotlinx-coroutines-core | 1.5.0 | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/1.5.0/kotlinx-coroutines-core-1.5.0.pom) |
| org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm | 1.5.0 | The Apache Software License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.5.0/kotlinx-coroutines-core-jvm-1.5.0.pom) |
| commons-io:commons-io | 2.5 | Apache License, Version 2.0 | [POM](https://repo.maven.apache.org/maven2/commons-io/commons-io/2.5/commons-io-2.5.pom) |

## Retained upstream license and NOTICE texts

### Notice 1

- androidx.exifinterface:exifinterface:1.4.2 / META-INF/androidx/exifinterface/exifinterface/LICENSE.txt

```text

                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS
```

### Notice 2

- androidx.media3:media3-exoplayer:1.5.1 / classes.jar / androidx/media3/exoplayer/drm/OfflineLicenseHelper$1.class

```text
����   4 <	      4androidx/media3/exoplayer/drm/OfflineLicenseHelper$1 this$0 4Landroidx/media3/exoplayer/drm/OfflineLicenseHelper;
  	 
   java/lang/Object <init> ()V
      2androidx/media3/exoplayer/drm/OfflineLicenseHelper 
access$000 T(Landroidx/media3/exoplayer/drm/OfflineLicenseHelper;)Landroid/os/ConditionVariable;
      android/os/ConditionVariable open  5androidx/media3/exoplayer/drm/DrmSessionEventListener 7(Landroidx/media3/exoplayer/drm/OfflineLicenseHelper;)V Code LineNumberTable LocalVariableTable this 6Landroidx/media3/exoplayer/drm/OfflineLicenseHelper$1; onDrmKeysLoaded @(ILandroidx/media3/exoplayer/source/MediaSource$MediaPeriodId;)V windowIndex I 
mediaPeriodId <Landroidx/media3/exoplayer/source/MediaSource$MediaPeriodId; $RuntimeInvisibleParameterAnnotations Landroidx/annotation/Nullable; onDrmSessionManagerError U(ILandroidx/media3/exoplayer/source/MediaSource$MediaPeriodId;Ljava/lang/Exception;)V e Ljava/lang/Exception; onDrmKeysRestored onDrmKeysRemoved 
SourceFile OfflineLicenseHelper.java EnclosingMethod  2 �(Landroidx/media3/exoplayer/drm/DefaultDrmSessionManager;Landroidx/media3/exoplayer/drm/DrmSessionEventListener$EventDispatcher;)V InnerClasses 5 :androidx/media3/exoplayer/source/MediaSource$MediaPeriodId 7 ,androidx/media3/exoplayer/source/MediaSource 
MediaPeriodId : Eandroidx/media3/exoplayer/drm/DrmSessionEventListener$EventDispatcher EventDispatcher                     >     
*+� *� �           �        
       
       !     M     *� � 
� �       
    � 
 �                 " #     $ %  &   	    '    ( )     W     *� � 
� �       
    � 
 �    *            " #     $ %     * +  &       '      , !     M     *� � 
� �       
    � 
 �                 " #     $ %  &   	    '    - !     M     *� � 
� �       
    � 
 �                 " #     $ %  &   	    '    .    / 0     1 3            4 6 8  9  ; 
```

### Notice 3

- androidx.media3:media3-exoplayer:1.5.1 / classes.jar / androidx/media3/exoplayer/drm/OfflineLicenseHelper.class

```text
����   4�	      2androidx/media3/exoplayer/drm/OfflineLicenseHelper drmListenerConditionVariable Landroid/os/ConditionVariable;
   	 
 newWidevineInstance �(Ljava/lang/String;ZLandroidx/media3/datasource/DataSource$Factory;Landroidx/media3/exoplayer/drm/DrmSessionEventListener$EventDispatcher;)Landroidx/media3/exoplayer/drm/OfflineLicenseHelper;
   	 
 �(Ljava/lang/String;ZLandroidx/media3/datasource/DataSource$Factory;Ljava/util/Map;Landroidx/media3/exoplayer/drm/DrmSessionEventListener$EventDispatcher;)Landroidx/media3/exoplayer/drm/OfflineLicenseHelper;  >androidx/media3/exoplayer/drm/DefaultDrmSessionManager$Builder
     <init> ()V
     setKeyRequestParameters Q(Ljava/util/Map;)Landroidx/media3/exoplayer/drm/DefaultDrmSessionManager$Builder;  2androidx/media3/exoplayer/drm/HttpMediaDrmCallback
     E(Ljava/lang/String;ZLandroidx/media3/datasource/DataSource$Factory;)V
      build j(Landroidx/media3/exoplayer/drm/MediaDrmCallback;)Landroidx/media3/exoplayer/drm/DefaultDrmSessionManager;
  "  # �(Landroidx/media3/exoplayer/drm/DefaultDrmSessionManager;Landroidx/media3/exoplayer/drm/DrmSessionEventListener$EventDispatcher;)V
 %  & java/lang/Object	  ( ) * drmSessionManager 8Landroidx/media3/exoplayer/drm/DefaultDrmSessionManager;	  , - . eventDispatcher GLandroidx/media3/exoplayer/drm/DrmSessionEventListener$EventDispatcher; 0 android/os/HandlerThread 2 ExoPlayer:OfflineLicenseHelper
 / 4  5 (Ljava/lang/String;)V	  7 8 9 
handlerThread Landroid/os/HandlerThread;
 / ; <  start > android/os/Handler
 / @ A B 	getLooper ()Landroid/os/Looper;
 = D  E (Landroid/os/Looper;)V	  G H I handler Landroid/os/Handler; K android/os/ConditionVariable
 J  N 4androidx/media3/exoplayer/drm/OfflineLicenseHelper$1
 M P  Q 7(Landroidx/media3/exoplayer/drm/OfflineLicenseHelper;)V
 S T U V W Eandroidx/media3/exoplayer/drm/DrmSessionEventListener$EventDispatcher addEventListener N(Landroid/os/Handler;Landroidx/media3/exoplayer/drm/DrmSessionEventListener;)V	 Y Z [ \ ] androidx/media3/common/Format drmInitData $Landroidx/media3/common/DrmInitData;
 _ ` a b c &androidx/media3/common/util/Assertions 
checkArgument (Z)V e 6androidx/media3/exoplayer/drm/DefaultDrmSessionManager
  g h i 9acquireSessionAndGetOfflineLicenseKeySetIdOnHandlerThread &(I[BLandroidx/media3/common/Format;)[B
 _ k l m checkNotNull &(Ljava/lang/Object;)Ljava/lang/Object;	  o p q FORMAT_WITH_EMPTY_DRM_INIT_DATA Landroidx/media3/common/Format;
  s t u "acquireFirstSessionOnHandlerThread N(I[BLandroidx/media3/common/Format;)Landroidx/media3/exoplayer/drm/DrmSession; w <androidx/media3/exoplayer/drm/DrmSession$DrmSessionException
 v y z { getCause ()Ljava/lang/Throwable; } 2androidx/media3/exoplayer/drm/KeysExpiredException
  � � � � java/lang/Long valueOf (J)Ljava/lang/Long;
 � � � � � android/util/Pair create 9(Ljava/lang/Object;Ljava/lang/Object;)Landroid/util/Pair;
 � � � � � 0com/google/common/util/concurrent/SettableFuture 4()Lcom/google/common/util/concurrent/SettableFuture;   � � � run �(Landroidx/media3/exoplayer/drm/OfflineLicenseHelper;Lcom/google/common/util/concurrent/SettableFuture;Landroidx/media3/exoplayer/drm/DrmSession;)Ljava/lang/Runnable;
 = � � � post (Ljava/lang/Runnable;)Z
 � � � � get ()Ljava/lang/Object;
  � �  releaseManagerOnHandlerThread � 'java/util/concurrent/ExecutionException � java/lang/InterruptedException � java/lang/IllegalStateException
 � �  � (Ljava/lang/Throwable;)V
 / � � � quit ()Z  � � [B
 J � �  close  � � � �(Landroidx/media3/exoplayer/drm/OfflineLicenseHelper;I[BLcom/google/common/util/concurrent/SettableFuture;Landroidx/media3/common/Format;)Ljava/lang/Runnable; � (androidx/media3/exoplayer/drm/DrmSession
 J � �  block  � � � �(Landroidx/media3/exoplayer/drm/OfflineLicenseHelper;Landroidx/media3/exoplayer/drm/DrmSession;Lcom/google/common/util/concurrent/SettableFuture;)Ljava/lang/Runnable;  � � � |(Landroidx/media3/exoplayer/drm/OfflineLicenseHelper;Lcom/google/common/util/concurrent/SettableFuture;)Ljava/lang/Runnable;
 d � �  release
 � � � � set (Ljava/lang/Object;)Z � java/lang/Throwable
 � � � � setException (Ljava/lang/Throwable;)Z � � � � getError @()Landroidx/media3/exoplayer/drm/DrmSession$DrmSessionException; � � � � getState ()I � � � � J(Landroidx/media3/exoplayer/drm/DrmSessionEventListener$EventDispatcher;)V
 � � � � B android/os/Looper myLooper	 � � � � � ,androidx/media3/exoplayer/analytics/PlayerId UNSET .Landroidx/media3/exoplayer/analytics/PlayerId;
 d � � � 	setPlayer D(Landroid/os/Looper;Landroidx/media3/exoplayer/analytics/PlayerId;)V
 d � �  prepare
 d � � � setMode (I[B)V
 d � � � acquireSession �(Landroidx/media3/exoplayer/drm/DrmSessionEventListener$EventDispatcher;Landroidx/media3/common/Format;)Landroidx/media3/exoplayer/drm/DrmSession; � � � � getOfflineLicenseKeySetId ()[B
 � � � � � *androidx/media3/exoplayer/drm/WidevineUtil getLicenseDurationRemainingSec ?(Landroidx/media3/exoplayer/drm/DrmSession;)Landroid/util/Pair; � %androidx/media3/common/Format$Builder
 �  � "androidx/media3/common/DrmInitData -androidx/media3/common/DrmInitData$SchemeData
 �  3([Landroidx/media3/common/DrmInitData$SchemeData;)V
 � setDrmInitData M(Landroidx/media3/common/DrmInitData;)Landroidx/media3/common/Format$Builder;
 �
  !()Landroidx/media3/common/Format; �(Ljava/lang/String;Landroidx/media3/datasource/DataSource$Factory;Landroidx/media3/exoplayer/drm/DrmSessionEventListener$EventDispatcher;)Landroidx/media3/exoplayer/drm/OfflineLicenseHelper; Code LineNumberTable LocalVariableTable defaultLicenseUrl Ljava/lang/String; dataSourceFactory /Landroidx/media3/datasource/DataSource$Factory; forceDefaultLicenseUrl Z optionalKeyRequestParameters Ljava/util/Map; LocalVariableTypeTable 5Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>; 	Signature �(Ljava/lang/String;ZLandroidx/media3/datasource/DataSource$Factory;Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;Landroidx/media3/exoplayer/drm/DrmSessionEventListener$EventDispatcher;)Landroidx/media3/exoplayer/drm/OfflineLicenseHelper; $RuntimeInvisibleParameterAnnotations Landroidx/annotation/Nullable; this 4Landroidx/media3/exoplayer/drm/OfflineLicenseHelper; defaultDrmSessionManager 
eventListener 7Landroidx/media3/exoplayer/drm/DrmSessionEventListener; downloadLicense #(Landroidx/media3/common/Format;)[B format 
StackMapTable 
Exceptions renewLicense ([B)[B offlineLicenseKeySetId releaseLicense ([B)V ([B)Landroid/util/Pair; 
drmSession *Landroidx/media3/exoplayer/drm/DrmSession; e >Landroidx/media3/exoplayer/drm/DrmSession$DrmSessionException; Ljava/lang/Exception; licenseDurationRemainingSec 2Lcom/google/common/util/concurrent/SettableFuture; iLcom/google/common/util/concurrent/SettableFuture<Landroid/util/Pair<Ljava/lang/Long;Ljava/lang/Long;>;>;7 java/lang/Exception 9([B)Landroid/util/Pair<Ljava/lang/Long;Ljava/lang/Long;>; licenseMode I keySetId 6Lcom/google/common/util/concurrent/SettableFuture<[B>; RuntimeInvisibleTypeAnnotations *Landroidx/media3/common/util/NullableType; drmSessionFuture drmSessionError ^Lcom/google/common/util/concurrent/SettableFuture<Landroidx/media3/exoplayer/drm/DrmSession;>; rLcom/google/common/util/concurrent/SettableFuture<Landroidx/media3/exoplayer/drm/DrmSession$DrmSessionException;>; result DLcom/google/common/util/concurrent/SettableFuture<Ljava/lang/Void;>; &lambda$releaseManagerOnHandlerThread$4 5(Lcom/google/common/util/concurrent/SettableFuture;)V Ljava/lang/Throwable; +lambda$acquireFirstSessionOnHandlerThread$3 _(Landroidx/media3/exoplayer/drm/DrmSession;Lcom/google/common/util/concurrent/SettableFuture;)V error +lambda$acquireFirstSessionOnHandlerThread$2 W(I[BLcom/google/common/util/concurrent/SettableFuture;Landroidx/media3/common/Format;)V Blambda$acquireSessionAndGetOfflineLicenseKeySetIdOnHandlerThread$1 _(Lcom/google/common/util/concurrent/SettableFuture;Landroidx/media3/exoplayer/drm/DrmSession;)V 'lambda$getLicenseDurationRemainingSec$0 
access$000 T(Landroidx/media3/exoplayer/drm/OfflineLicenseHelper;)Landroid/os/ConditionVariable; x0 <clinit> 
SourceFile OfflineLicenseHelper.java RuntimeInvisibleAnnotations )Landroidx/media3/common/util/UnstableApi; BootstrapMethodsZ
[\]^_ "java/lang/invoke/LambdaMetafactory metafactory �(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; b
 cONe
 fMNh
 iKLk
 lHIn
 oEF InnerClassesr -androidx/media3/datasource/DataSource$Factoryt %androidx/media3/datasource/DataSource Factoryw 5androidx/media3/exoplayer/drm/DrmSessionEventListener EventDispatcher Builder DrmSessionException 
SchemeData} %java/lang/invoke/MethodHandles$Lookup java/lang/invoke/MethodHandles Lookup 1  %     p q         ) *    8 9    H I    - .    	 	 
   F     *+,� �          C                  - .  	 	 
 
   Q     	*,-� �          Y   *    	     	    	    	 - .  	 	 
 
   �     "� Y� Y� -� � Y*,� � � !�          w  y  z ! w   4    "     "    "    "    " - .        "                     # 
   �     \*� $*+� '*,� +*� /Y1� 3� 6*� 6� :*� =Y*� 6� ?� C� F*� JY� L� � MY*� ON,� =Y*� 6� ?� C-� R�      * 
   �  � 	 �  �  � " � 4 � ? � H � [ �   *    \     \  *    \ - .  H !"  !#$ 
   Z     +� X� � � ^*+� f�      
    �  �            % q &    @'     v !() 
   G     +� jW*+� n� f�      
    �  �            * � '     v !+, 
   L     +� jW*+� n� fW�          �  �  �            * � '     v ! �- 
  �     d+� jW*+� n� rM� N-� x� |� 	� ~	� ~� ��-�� �N*� F*-,� �  � �W-� �� �:*� ��:� �Y� ��:*� ��     v ? H O � ? H O � ? H [   O ] [      F    �  �  �  �  �  �  � ) � + � / � ? � H  L � O � Q � [  a   H   ./   01  Q 
02    d     d* �  + 9./  / 534      / 535 &   7 R v�   v�    � �  � #   � � � 6K �'     v   8  �  
   7     	*� 6� �W�      
           	    h i 
  J     L*,-� r:� �:*� F*� �  � �W� �� �� j� �:*� ��:� �Y� ��:*� ��    0 7 �   0 7 �   0 C   7 E C      .     	   # 0' 4# 7$ 9% C' I(   H  9 
02    L     L9:    L* �    L% q  	 C./   >;4       >;< &     � 7   � Y � � 6K �=    @   >  >  '     v            t u 
  �     ~-� X� jW� �:*� � �*� F*,-� �  � �W� �� �:� :� �Y� ��*� � �� �:*� F*� �  � �W� �� � �� v��:� �Y� ��  ' 1 4 � ' 1 4 � ^ q r � ^ q r �    B   C D 
E F '[ 1^ 4\ 6] @c Ge Lf ^v fw oy r{ t|   f 
 1 ./  6 
02  t 
02    ~     ~9:    ~* �    ~% q  
 q?4  @ >./  L 2@4      
 q?A  L 2@B &   ) � 4   � Y � 6�  �� . �B6=    @  L 2  >  '     v            �  
   �     &� �L*� F*+� �  � �W+� �W� 
M� �Y,� ���     �    �       � � � � � � %�       	02    &    "C4       "CD &    �    � 6	EF 
        *� '� �+� �W� 
M+,� �W�    
  �       � � 
� � � �       0G         C4 &    P �HI 
   �     D+� � N+� � � +*� +� � *� '� �,-� �W� N,-� �W+*� +� � *� '� ��    ( + �    .   i j k l "n (s +o ,p 2q <r Ct   4   !J1  , 0G    D     D./    D@4 &    � " v�    � �  �KL 
       W*� '� ׸ j� ز ܶ �*� '� �*� ',� �-*� '*� +� �� j� �� �W� :*� '� ��� :-� �W�   ; > �   J M �    >   I J L #M .O 1N 7M ;S >P @Q GR JV MT OU VW   H  @ 
0G  O 0G    W     W9:    W* �    W?4    W% q &    ~ �B �MN 
   �     <+,� � � �W,*� +� � � &N+-� �W,*� +� � � :,*� +� � ��      �    ,     ,   , . ,      * 
        ) , 8 ;    *   0G    <     <;4    <./ &    X �S �ON 
   �     @+,� �� j� �� �W,*� +� � � &N+-� �W,*� +� � � :,*� +� � ��      �    0    # 0   0 2 0      2    �  �  �  �  �  �  � # � - � 0 � < � ? �   *   0G    @     @34    @./ &    \ �S �PQ 
   /     *� �          )       R   S  
   <      � �Y� �� �Y� ���	� n�          +  ,  + T   UV    W  X   4 Y `a`Y `d`Y `g`Y `j`Y `m`p   B qsu	 Svx 	  dy  M       v �z 	 � Yy   �{ |~� 
```

### Notice 4

- com.google.guava:guava:33.3.1-jre / META-INF/LICENSE

```text

                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

### Notice 5

- org.checkerframework:checker-qual:3.43.0 / META-INF/LICENSE.txt

```text
Checker Framework qualifiers
Copyright 2004-present by the Checker Framework developers

MIT License:

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```

### Notice 6

- com.atlassian.commonmark:commonmark:0.13.0 / META-INF/LICENSE.txt
- com.atlassian.commonmark:commonmark-ext-gfm-strikethrough:0.13.0 / META-INF/LICENSE.txt
- com.atlassian.commonmark:commonmark-ext-gfm-tables:0.13.0 / META-INF/LICENSE.txt

```text
Copyright (c) 2015-2016, Atlassian Pty Ltd
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

### Notice 7

- commons-io:commons-io:2.5 / META-INF/LICENSE.txt

```text

                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

### Notice 8

- commons-io:commons-io:2.5 / META-INF/NOTICE.txt

```text
Apache Commons IO
Copyright 2002-2016 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).
```

## Existing license collection

Full GPL-3.0 text: COPYING and assets/licenses/Termux-X11-GPL-3.0.txt.
Apache-2.0 and CommonMark BSD notices: assets/markdown-NOTICE.txt.
Media3 Apache-2.0: assets/media3-LICENSE.txt. Other native per-file notices remain
in the existing assets/licenses and third-party directories; those texts are not
replaced or relicensed here. See docs/fork/THIRD-PARTY.md for source/build details.

This notice inventory does not claim a complete independent legal/security audit.
Unresolved exact-source coverage for older mirrored distro/XFCE/bootstrap packages
must remain visible in the dated license review rather than being declared compliant.


# Offline field dictation (private test12 / code1507)

Vosk Android 0.3.75 — Alpha Cephei Inc. and contributors — Apache-2.0.
Artifact: https://repo.maven.apache.org/maven2/com/alphacephei/vosk-android/0.3.75/vosk-android-0.3.75.aar
Exact published Java sources: https://repo.maven.apache.org/maven2/com/alphacephei/vosk-android/0.3.75/vosk-android-0.3.75-sources.jar
Native upstream: https://github.com/alphacep/vosk-api (no matching 0.3.75 Git tag found;
this build uses the published AAR binary, not a reproduced native build).
Native algorithms also include upstream Kaldi/OpenFst dependencies. The AAR is
used unchanged; this project does not claim upstream authorship or endorsement.

JNA 5.18.1 — Timothy Wall and contributors. Distribution under the offered
Apache-2.0 option; original dual-license notice retained in jna-LICENSE.txt.
Source: https://github.com/java-native-access/jna/tree/5.18.1
Exact Java source artifact: https://repo.maven.apache.org/maven2/net/java/dev/jna/jna/5.18.1/jna-5.18.1-sources.jar

Russian small model 0.22 — Alpha Cephei, Apache-2.0 as listed at
https://alphacephei.com/vosk/models . Model archive:
https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip
SHA-256: 961d5ff98a17f4aa6de69864d0aa71fa5bac682301d2b5d17a3f24c5c99a46d4
The model is included compressed and extracted into this app's private cache.
No other language is bundled. No audio recording is written to a file or sent
by this dictation backend. This does not describe unrelated network agent features.

Full Apache-2.0 license texts and original JNA notice accompany this file.
Native/AAR/model identities and verification limits are in the test12 validation.
