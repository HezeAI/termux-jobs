package com.termux.app.workspace;

import android.net.Uri;
import java.util.*;

/** Reviewed documentation sources; neither URLs nor page content become commands. */
final class WorkspaceDocumentation {
    static final class Source {
        final String id,name,en,ru;final String[] domains;
        Source(String id,String name,String en,String ru,String... domains){this.id=id;this.name=name;this.en=en;this.ru=ru;this.domains=domains;}
        String home(String language){return "ru".equals(language)&&ru!=null?ru:en;}
        boolean fallback(String language){return "ru".equals(language)&&ru==null;}
        boolean owns(String url){Uri u=https(url);if(u==null)return false;String host=u.getHost().toLowerCase(Locale.ROOT);if(id.equals("archwiki")&&!Arrays.asList(domains).contains(host))return false;for(String domain:domains)if(host.equals(domain)||host.endsWith("."+domain)){if(domain.equals("github.com"))return host.equals(domain)&&u.getPath()!=null&&u.getPath().startsWith("/termux/");return true;}return false;}
    }
    static final String FORUM="https://bbs.archlinux.org/";
    static final Source WIKI=new Source("archwiki","ArchWiki","https://wiki.archlinux.org/","https://wiki.archlinux.org/title/Main_page_(%D0%A0%D1%83%D1%81%D1%81%D0%BA%D0%B8%D0%B9)","wiki.archlinux.org","bbs.archlinux.org");
    private static final Map<String,Source> SOURCES;
    static {
        Map<String,Source> s=new LinkedHashMap<>();
        s.put("termux",new Source("termux","Termux","https://github.com/termux/termux-packages/wiki",null,"github.com","termux.dev","wiki.termux.com"));
        s.put("ubuntu",new Source("ubuntu","Ubuntu","https://documentation.ubuntu.com/",null,"ubuntu.com"));
        s.put("debian",new Source("debian","Debian","https://www.debian.org/doc/index.en.html","https://www.debian.org/doc/index.ru.html","debian.org"));
        s.put("alpine",new Source("alpine","Alpine Linux","https://docs.alpinelinux.org/",null,"alpinelinux.org"));
        s.put("fedora",new Source("fedora","Fedora","https://docs.fedoraproject.org/en-US/docs/","https://docs.fedoraproject.org/ru/docs/","fedoraproject.org"));
        s.put("opensuse",new Source("opensuse","openSUSE","https://doc.opensuse.org/",null,"opensuse.org"));
        s.put("void",new Source("void","Void Linux","https://docs.voidlinux.org/",null,"voidlinux.org"));
        s.put("archlinux",new Source("archlinux","Arch Linux ARM","https://archlinuxarm.org/",null,"archlinuxarm.org"));
        s.put("kali",new Source("kali","Kali Linux","https://www.kali.org/docs/",null,"kali.org"));
        s.put("manjaro",new Source("manjaro","Manjaro ARM","https://wiki.manjaro.org/index.php?title=Main_Page","https://wiki.manjaro.org/index.php?title=Main_Page/ru","manjaro.org"));
        SOURCES=Collections.unmodifiableMap(s);
    }
    static Source environment(String target){if("jobs-x11-ubuntu".equals(target)||"ubuntu-ai".equals(target))target="ubuntu";Source source=SOURCES.get(target);if(source==null)throw new IllegalArgumentException("Unsupported documentation target");return source;}
    static Set<String> targets(){return SOURCES.keySet();}
    static Uri https(String value){
        if(value==null||value.isEmpty()||value.length()>8192)return null;
        for(int i=0;i<value.length();i++)if(Character.isWhitespace(value.charAt(i))||Character.isISOControl(value.charAt(i)))return null;
        try{Uri u=Uri.parse(value);return "https".equalsIgnoreCase(u.getScheme())&&u.getHost()!=null&&!u.getHost().isEmpty()&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)?u:null;}catch(IllegalArgumentException bad){return null;}
    }
    static String search(String query,String language){String q=query==null?"":query.trim();if(q.isEmpty())return WIKI.home(language);if(q.length()>200)q=q.substring(0,200);return Uri.parse("https://wiki.archlinux.org/index.php").buildUpon().appendQueryParameter("title","Special:Search").appendQueryParameter("search",q).appendQueryParameter("uselang","ru".equals(language)?"ru":"en").build().toString();}
}
