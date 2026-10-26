# Kali-inspired resources

Выбранный концепт: ../x11-linux-concepts-v1.7.94/Termux-Jobs-X11-linux-kali.png.
Фон wallpaper.png создан встроенным image_gen из него: первый запрос
в prompt.txt, финальный запрос/путь генерации в generated.json.
Размер, SHA256, происхождение — source.json.

Финальный фон971×1619 визуально просмотрен; без панелей, окон и значков.
Кнопки/рамки/значки рабочего стола — собственные ресурсы, генерируемые
../../scripts/prepare-x11-style.py. Это стилизация для XFCE, не дистрибутив Kali.

From private test2, `x11-style.zip` is built into the repository-local
`.tools/x11-components` directory and published as a pinned optional component.
It is downloaded and verified before starting a desktop with the style enabled.
An existing exact archive or verified shared cache is reused without network.
Style application, per-home backup/restore and appearance remain unchanged.
