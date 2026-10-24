package com.termux.x11;

/** Fixed, reviewed APT package groups; profile text never becomes a package/command. */
public final class DesktopPrograms {
    private DesktopPrograms(){}
    /** Exact aliases only; never redirect a distribution to another rootfs. */
    public static boolean supportsTarget(String target){return target.equals("ubuntu")||supportsMaintenance(target);}
    /** Optional appearance is limited to the eight current non-agent desktops. */
    public static boolean supportsStyle(String target){return "termux".equals(target)||"ubuntu".equals(target)||"debian".equals(target)||"alpine".equals(target)||"fedora".equals(target)||"opensuse".equals(target)||"void".equals(target)||"archlinux".equals(target);}
    public static boolean supportsFirefox(String target){return supportsTarget(target);}
    public static boolean supportsMaintenance(String target){return target.equals("ubuntu")||target.equals("jobs-x11-ubuntu")||target.equals("debian")||target.equals("alpine")||target.equals("termux")||target.equals("fedora")||target.equals("opensuse")||target.equals("void")||target.equals("archlinux");}
    public static final String[] IDS={"files","terminal","editor","archives","images","fonts","browser","office","pdf","media","gpu-tools","gpu-test"};
    public static final String[] RECOMMENDED={"editor","archives","images","browser","office","pdf","media","gpu-tools","gpu-test"};
    public static boolean removable(String pkg){
        return DesktopCatalog.packageName(pkg)&&!pkg.matches("(?:pacman(?:-.*)?|archlinuxarm-keyring|Thunar(?:-.*)?|rpm(?:-.*)?|dnf(?:5)?(?:-.*)?|xbps(?:-.*)?|base-(?:files|filesystem|system)|zypper(?:-.*)?|libzypp(?:-.*)?|glibc(?:-.*)?|systemd(?:-.*)?|bash|coreutils(?:-.*)?|xfce4(?:-.*)?|xfdesktop4(?:-.*)?|xfwm4(?:-.*)?|thunar(?:-.*)?|xfconf|libxfconf-.*|fcitx5(?:-.*)?|dbus(?:-.*)?|libpulse0|libpulse|musl(?:-.*)?|apk-tools(?:-.*)?|gst-plugins-good|xfdesktop|font-.*|ttf-.*|ca-certificates(?:-.*)?|pulseaudio|gstreamer1\\.0-pulseaudio|xauth|x11-.*|fonts-.*|adwaita-icon-theme|hicolor-icon-theme|python3(?:-.*)?|appstream|gir1\\.2-appstream-.*)");
    }
    /** Main application only: keep shared codecs, thumbnailers and documents. */
    public static String mainPackage(String id,String target){switch(id){
        case "gpu-test":return "termux-jobs-gpu-test";
        case "gpu-tools":return target.equals("alpine")?"mesa-utils":target.equals("ubuntu")||target.equals("jobs-x11-ubuntu")||target.equals("debian")?"glmark2-x11":"glmark2";
        case "editor":return "mousepad";case "archives":return target.equals("opensuse")?"engrampa":"xarchiver";case "images":return "ristretto";
        case "browser":return target.equals("opensuse")?"MozillaFirefox":"firefox";case "office":return target.equals("termux")?"libreoffice":target.equals("archlinux")?"libreoffice-fresh":"libreoffice-writer";
        case "pdf":return "evince";case "media":return "parole";default:return "";}}
    static boolean knownRecommendedPackage(String pkg,String target,android.os.Bundle items){
        if(items==null)return false;
        for(String id:RECOMMENDED)if(items.getBoolean(id,false)&&mainPackage(id,target).equals(pkg))return true;
        return false;
    }
    public static boolean valid(String id){for(String known:IDS)if(known.equals(id))return true;return false;}
    static boolean requestId(String id){return valid(id)||id.equals("catalog-tools")||(id.startsWith("pkg:")&&DesktopCatalog.packageName(id.substring(4)));}
    public static String title(String id,boolean ru,String target){return id.equals("archives")&&target.equals("opensuse")?"Engrampa":title(id,ru);}
    public static String title(String id,boolean ru){switch(id){
        case "gpu-test":return "Jobs GPU Test";
        case "gpu-tools":return "GPU & GPU INFO";
        case "catalog-tools":return ru?"Подготовка каталога":"Catalog setup";case "files":return "Thunar";case "terminal":return "XFCE Terminal";case "editor":return "Mousepad";
        case "archives":return "Xarchiver";case "images":return "Ristretto";case "fonts":return ru?"Шрифты и значки":"Fonts and icons";
        case "media":return "Parole";case "browser":return "Firefox";case "office":return "LibreOffice";case "pdf":return "Evince · PDF";default:throw new IllegalArgumentException("Unknown desktop program");}}
    public static String description(String id,boolean ru){switch(id){
        case "gpu-test":return ru?"Наш графический тест: 3D, текстуры, время кадра, VSync и информация о драйвере. Создаёт ярлык GPU TEST. Работает с выбранным рендером; отчёт JSON сохраняется по кнопке. FPS не является баллом glmark2.":"Our graphics diagnostic: 3D, textures, frame timing, VSync and driver information. Creates a GPU TEST shortcut. Uses the selected renderer; save a JSON report with the Report button. FPS is not a glmark2 score.";
        case "gpu-tools":return ru?"Тест рендера и информация о GPU. Установит пакеты выбранной системы и создаст на рабочем столе ярлыки GPU и GPU INFO. Тест — glmark2; в openSUSE — glmark2-es2/EGL. В Alpine — glxgears без балла glmark2. Информация — glxinfo -B. Используется выбранный рендер рабочего стола.":"Renderer test and GPU information. Installs this environment's packages and creates GPU and GPU INFO desktop shortcuts. Uses glmark2; openSUSE uses glmark2-es2/EGL. Alpine uses glxgears without a glmark2 score. Information uses glxinfo -B. Keeps the desktop's selected renderer.";
        case "files":return ru?"Файлы и папки выбранного окружения.":"Browse files and folders in the selected environment.";
        case "terminal":return ru?"Командная строка в окне рабочего стола.":"A command-line window on the desktop.";
        case "editor":return ru?"Редактирование TXT, Markdown, скриптов и конфигураций.":"Edit text, Markdown, scripts and configuration files.";
        case "archives":return ru?"Создание и распаковка ZIP, 7z и tar-архивов.":"Create and extract ZIP, 7z and tar archives.";
        case "images":return ru?"Просмотр изображений и фотографий. Tumbler создаёт миниатюры.":"View images and photographs. Tumbler generates thumbnails.";
        case "fonts":return ru?"Кириллица, латиница и значки рабочего стола.":"Cyrillic and Latin fonts, plus desktop icons.";
        case "browser":return ru?"Современные сайты и JavaScript. Установка без Snap из официального подписанного репозитория Mozilla; он будет добавлен в выбранное окружение. При падении вкладок включите «Совместимость с PRoot» и перезапустите Firefox.":"Modern websites and JavaScript. Installed without Snap from the official signed Mozilla repository, added to the selected environment. If tabs crash, enable PRoot compatibility and restart Firefox.";
        case "office":return ru?"Writer, Calc и Impress: документы, таблицы и презентации. Требуется больше места.":"Writer, Calc and Impress: documents, spreadsheets and presentations. Requires more storage.";
        case "media":return ru?"Видео и аудио: MP4, MKV, WebM, MP3, OGG, WAV. Устанавливаются плеер Parole и декодеры GStreamer. Звук передаётся через компоненты рабочего стола. Поддержка зависит от кодека файла; декодирование программное.":"Video and audio: MP4, MKV, WebM, MP3, OGG, WAV. Installs Parole and GStreamer decoders. Sound uses the desktop audio components. Support depends on the file codec; decoding is software-based.";
        case "pdf":return ru?"Просмотр PDF-документов.":"Read PDF documents.";default:throw new IllegalArgumentException("Unknown desktop program");}}
}
