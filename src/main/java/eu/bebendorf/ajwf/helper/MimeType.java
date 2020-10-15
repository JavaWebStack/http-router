package eu.bebendorf.ajwf.helper;

import java.util.Arrays;
import java.util.List;

public enum MimeType {
    HTML("text/html", "html","htm"),
    XHTML("application/xhtml+xml", "xhtml"),
    CSS("text/css", "css"),
    JS("application/javascript", "js"),
    JSON("application/json", "json"),
    PDF("application/pdf", "pdf"),
    XML("application/xml", "xml"),
    X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded"),
    OCTET_STREAM("application/octet-stream", ".bin", ".file", ".com", ".class", ".ini"),
    ZIP("application/zip", "zip"),
    PNG("image/png", "png"),
    JPG("image/jpeg", "jpg", "jpeg"),
    SVG("image/svg+xml", "svg"),
    ICO("image/x-icon", "ico"),
    PLAIN("text/plain", "txt"),
    MP4("video/mp4", "mp4"),
    MP3("audio/mpeg", "mp3"),
    WAV("audio/wav", "wav"),
    OGG("audio/ogg", "ogg");

    MimeType(String mime, String... extensions){
        this.mime = mime;
        this.extensions = Arrays.asList(extensions);
    }


    final String mime;
    final List<String> extensions;
    static MimeType byExtension(String extension){
        if(extension.startsWith("."))
            extension = extension.substring(1);
        for(MimeType type : values()){
            if(type.extensions.contains(extension)){
                return type;
            }
        }
        return null;
    }
    static MimeType byFileName(String fileName){
        if(fileName.contains("/")) {
            String[] spl = fileName.split("/");
            fileName = spl[spl.length - 1];
        }
        if(!fileName.contains("."))
            return PLAIN;
        String[] spl = fileName.split("\\.");
        MimeType type = byExtension(spl[spl.length - 1]);
        return type != null ? type : PLAIN;
    }
}