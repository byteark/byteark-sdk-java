package com.byteark;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

class UrlHelper {
    public static Map<String, String> splitQuery(URL url) {
        if (url.getQuery()==null || url.getQuery().isEmpty()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(url.getQuery().split("&"))
                .map(UrlHelper::splitQueryParameter)
                .collect(
                        Collectors.toMap(SimpleImmutableEntry::getKey,
                                SimpleImmutableEntry::getValue)
                );
    }
    public static Map<String, List<String>> splitQueryMultiMap(URL url) {
        if (url.getQuery()==null || url.getQuery().isEmpty()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(url.getQuery().split("&"))
                .map(UrlHelper::splitQueryParameter)
                .collect(
                        Collectors.groupingBy(SimpleImmutableEntry::getKey,
                                LinkedHashMap::new,
                                mapping(Map.Entry::getValue, toList())
                        )
                );
    }

    public static SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        final int idx = it.indexOf("=");
        String key = idx > 0 ? it.substring(0, idx):it;
        String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1):null;
        try {
            key = URLDecoder.decode(key, "UTF-8");
            value = URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return new SimpleImmutableEntry<>(key, value);
    }
}
