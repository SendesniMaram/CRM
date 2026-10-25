package com.crm.gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriUtils;

/** Reject internal namespaces conservatively; never rewrite an accepted path. */
final class InternalPathPolicy {
    private InternalPathPolicy() {}

    static HttpStatus rejection(String rawPath) {
        if (rawPath == null) return HttpStatus.BAD_REQUEST;
        String path = rawPath;
        for (int round = 0; round <= 6; round++) {
            if (path.chars().anyMatch(c -> c < 32 || c == 127 || c == 0xfffd)
                    || path.indexOf('?') >= 0 || path.indexOf('#') >= 0) {
                return HttpStatus.BAD_REQUEST;
            }
            if (containsInternalNamespace(path)) return HttpStatus.NOT_FOUND;
            if (!path.contains("%")) return null;
            if (round == 6) return HttpStatus.BAD_REQUEST;
            try {
                path = UriUtils.decode(path, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ex) {
                return HttpStatus.BAD_REQUEST;
            }
        }
        return HttpStatus.BAD_REQUEST;
    }

    private static boolean containsInternalNamespace(String path) {
        var canonical = new ArrayList<String>();
        String previous = "";
        for (String raw : path.replace('\\', '/').split("/")) {
            String segment = raw.split(";", 2)[0].toLowerCase(Locale.ROOT);
            // Check before dot normalization too: do not hide an internal path behind '..'.
            if (previous.equals("api") && segment.equals("internal")) return true;
            if (!segment.isEmpty()) previous = segment;
            if (segment.isEmpty() || segment.equals(".")) continue;
            if (segment.equals("..")) {
                if (!canonical.isEmpty()) canonical.remove(canonical.size() - 1);
            } else {
                canonical.add(segment);
            }
        }
        for (int i = 1; i < canonical.size(); i++) {
            if (canonical.get(i - 1).equals("api") && canonical.get(i).equals("internal")) return true;
        }
        return false;
    }
}
