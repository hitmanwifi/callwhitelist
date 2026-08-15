# Call Whitelist — правила и статус проекта

## Назначение

`org.alexrust.callwhitelist` — локальное Android-приложение для фильтрации
входящих вызовов через `CallScreeningService`. Правила, настройки и журнал
хранятся на устройстве; сетевого обмена и аналитических SDK нет.

## Архитектура

```text
app/                  Compose UI, navigation, system integration
callfilter/           CallScreeningService и горячий путь решения
core/model/           Kotlin/JVM модели предметной области
core/database/        Room entities, DAO и database provider
core/preferences/     DataStore настроек
core/designsystem/    Material 3 theme
data/                 локальные хранилища и snapshot stores
domain/               чистые правила фильтрации и unit-тесты
docs/                 продуктовые и будущие privacy/open-source документы
```

Отдельный Gradle-модуль для каждого экрана не создаём. Каждый экран и крупный
компонент — отдельный Kotlin-файл; навигация находится в `navigation`.

## Обязательные правила разработки

- Новый код только Kotlin; `.java`-исходники и пустые Java source roots не добавлять.
- Исходники находятся только в `src/main/kotlin`, `src/test/kotlin` и `src/androidTest/kotlin`.
- Для времени использовать `kotlin.time` и `kotlinx-datetime`, не `java.time`.
- Все версии Gradle, AGP, Kotlin и библиотек задавать только в `gradle/libs.versions.toml`.
- Compose и Material 3 выравнивать через BOM.
- Все пользовательские тексты хранить в `values` и `values-en`; UI-тексты не хардкодить.
- UI-иконки брать из стандартной Material Icons; launcher icon хранить как нативный Android vector/adaptive resource.
- Горячий путь `CallScreeningService` использует локальный snapshot без UI, сети и ожидания DataStore.
- `respondToCall` выполняется до журнала, уведомлений и другой фоновой работы.
- Не добавлять `READ_CALL_LOG`/`WRITE_CALL_LOG`.
- Доступ к контактам запрашивать только после понятного disclosure и использовать локально.
- Не добавлять OEM workaround без воспроизводимого пользовательского симптома.

## Аудит после каждого этапа

После каждого этапа нужно:

1. Обновить этот файл: отметить этап и текущий статус.
2. Проверить каждый модуль из `settings.gradle.kts`.
3. Проверить `src/main`, `src/test`, `src/androidTest`: `.java = 0`, пустых source/package-каталогов нет.
4. Проверить отсутствие `com.example`, старых package-путей, `java.time` и удалённых модулей.
5. Запустить применимые compile/test-задачи каждого модуля.
6. После структурных изменений выполнить clean-сборку и повторный аудит.
7. После успешной проверки сделать отдельный понятный Conventional Commit.

Для `core:model` и `domain` Android Studio может показывать логическую группу
`java`: источником истины являются физические пути `src/main/kotlin` и Gradle
source sets.

## Завершённые этапы

- V0 baseline: `50c6e55`.
- V1 идемпотентный журнал: `1e50bb2`.
- V2 глобальный тумблер фильтрации: `f041905`.
- V3 единый статус и статистика: `eca13d8`.
- V4 временные разрешённые номера и migration 2→3: `747433f`.
- V6 разбиение моделей и database source files: `5234054`.
- V7 clean build, тесты и полный модульный аудит: `cc5c5b9`.
- Двуязычный README: `bbbf7b0`.
- BSD-2-Clause и юридическая информация README: `3e9f805`.
- [x] V8 нативная launcher-иконка и блок лицензии/copyright в настройках.

## Лицензия и авторство

Исходный код проекта распространяется по BSD-2-Clause. Полный текст находится
в корневом `LICENSE`; copyright holder — `AlexRust`, 2026. Лицензии сторонних
зависимостей не переоформляются и должны быть перечислены отдельно перед
публичным релизом.

## Отложенный этап

Google Play/privacy readiness выполняется позже по решению пользователя:
target API, disclosure перед `READ_CONTACTS`, privacy policy, Data Safety,
публичный URL и финальная проверка публикации пока не входят в текущий этап.

## Текущий статус

V0–V4, V6, V7, лицензирование и V8 завершены. Последняя проверка: clean build,
доменные unit-тесты и `app:assembleDebug` прошли успешно; APK находится в
`app/build/outputs/apk/debug/app-debug.apk`.
