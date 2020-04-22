package com.byteark;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ByteArkV2UrlSigner {
    private String accessId;
    private String accessSecret;
    private int defaultAge = 900;
    private boolean skipUrlEncoding = false;

    private ByteArkV2UrlSigner() {
    }

    public ByteArkV2UrlSigner(String accessId,
                              String accessSecret,
                              int defaultAge,
                              boolean skipUrlEncoding) {
        Objects.requireNonNull(accessSecret);
        Objects.requireNonNull(accessId);
        this.accessId = accessId;
        this.accessSecret = accessSecret;
        this.defaultAge = defaultAge;
        this.skipUrlEncoding = skipUrlEncoding;
    }

    public String sign(String url, long expires) {
        return sign(url, expires, null);
    }

    public String sign(String url, long expires, Map<String, String> options) {
        if (expires <= 0) {
            expires = System.currentTimeMillis() / 1000 + defaultAge;
        }
        if (options==null) {
            options = new HashMap<>();
        }
        String queryString = makeQueryParams(url, expires, makeCanonicalOptions(options));
        return url + "?" + queryString;
    }

    public void verify(String url, long now)
            throws MalformedURLException,
            ByteArkSignedUrlExpiredException,
            ByteArkSignedUrlInvalidConditionException,
            ByteArkSignedUrlInvalidSignatureException,
            ByteArkSignedUrlMissingParamException {
        if (now < 0) {
            now = System.currentTimeMillis() / 1000;
        }

        URL parsedUrl = new URL(url);
        String path = parsedUrl.getPath();
        String parsedUrlWithoutQuery = url.substring(0, url.indexOf('?'));
        Map<String, String> parsedQuery = UrlHelper.splitQuery(parsedUrl);


        if (!parsedQuery.containsKey("x_ark_expires")) {
            throw new ByteArkSignedUrlMissingParamException("Missing query parameter 'x_ark_expires'");
        }
        if (!parsedQuery.containsKey("x_ark_signature")) {
            throw new ByteArkSignedUrlMissingParamException("Missing query parameter 'x_ark_signature'");
        }

        long parsedExpires = Long.parseLong(parsedQuery.get("x_ark_expires"));
        if (parsedExpires < now) {
            throw new ByteArkSignedUrlExpiredException();
        }

        if (parsedQuery.containsKey("x_ark_path_prefix")
                && !path.startsWith(parsedQuery.get("x_ark_path_prefix"))) {
            throw new ByteArkSignedUrlInvalidConditionException();
        }

        Map<String, String> options = new HashMap<>();
        parsedQuery.entrySet()
                .stream()
                .filter(this::shouldQueryExistsInOptions)
                .forEach(e -> {
                options.put(
                        e.getKey().replaceAll("x_ark_", "") ,
                        e.getValue());
            });
        String expectedSignature = this.makeSignature(
                parsedUrlWithoutQuery,
                parsedExpires,
                options
        );
        if ( !expectedSignature.equals(parsedQuery.get("x_ark_signature"))) {
            throw new ByteArkSignedUrlInvalidSignatureException();
        }
    }

    private boolean shouldQueryExistsInOptions(Map.Entry<String, String> entry) {
        String key = entry.getKey();
        return key.startsWith("x_ark_")
                && !key.equals("x_ark_access_id")
                && !key.equals("x_ark_auth_type")
                && !key.equals("x_ark_expires")
                && !key.equals("x_ark_signature");
    }

    String makeQueryParams(String url, long expires, Map<String, String> options) {
        SortedMap<String, String> queryParams = new TreeMap<>();
        queryParams.put("x_ark_access_id", accessId);
        queryParams.put("x_ark_auth_type", "ark-v2");
        queryParams.put("x_ark_expires", String.valueOf(expires));
        queryParams.put("x_ark_signature", this.makeSignature(url, expires, options));

        options.entrySet()
                .stream()
                .filter(this::shouldOptionExistsInQuery)
                .forEach(e -> {
                    queryParams.put("x_ark_" + e.getKey(),
                            this.shouldOptionValueExistsInQuery(e) ? e.getValue():"1");
                });

        StringBuilder sb = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                if (sb.length() > 0) {
                    sb.append('&');
                }
                sb.append(k);
                sb.append('=');
                if (skipUrlEncoding) {
                    sb.append(v);
                } else {
                    sb.append(URLEncoder.encode(v, "UTF-8"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    private boolean shouldOptionValueExistsInQuery(Map.Entry<String, String> entry) {
        return entry.getKey().equals("path_prefix");
    }

    private boolean shouldOptionExistsInQuery(Map.Entry<String, String> entry) {
        return !entry.getKey().equals("method");
    }

    String makeSignature(String url, long expires, Map<String, String> options) {
        try {
            String stringToSign = makeStringToSign(url, expires, options);
            MessageDigest hasher = MessageDigest.getInstance("MD5");
            hasher.update(stringToSign.getBytes());
            byte[] digest = hasher.digest();
            return new String(Base64.getEncoder().encode(digest))
                    .replace('+', '-')
                    .replace('/', '_')
                    .replaceAll("=+$", "");
        } catch (NoSuchAlgorithmException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    String makeStringToSign(String url, long expires, Map<String, String> options)
            throws MalformedURLException {
        URL urlComponents = new URL(url);

        StringBuilder linesToSign = new StringBuilder();
        linesToSign.append(options.getOrDefault("method", "GET"));
        linesToSign.append('\n');
        linesToSign.append(urlComponents.getHost());
        linesToSign.append('\n');
        linesToSign.append(options.getOrDefault("path_prefix", urlComponents.getPath()));
        linesToSign.append('\n');
        linesToSign.append(this.makeCustomPolicyLines(options));
        // already has new line
        linesToSign.append(expires);
        linesToSign.append('\n');
        linesToSign.append(this.accessSecret);
        return linesToSign.toString();
    }

    private Map<String, String> makeCanonicalOptions(Map<String, String> options) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            result.put(makeCanonical(k), v);
        }
        return result;
    }

    StringBuilder makeCustomPolicyLines(Map<String, String> options) {
        StringBuilder sb = new StringBuilder();
        options.entrySet()
                .stream()
                .filter(this::shouldOptionExistsInCustomPolicyLine)
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    sb.append(e.getKey())
                            .append(':')
                            .append(e.getValue())
                            .append('\n');
                });
        return sb;
    }

    private boolean shouldOptionExistsInCustomPolicyLine(Map.Entry<String, String> entry) {
        String k = entry.getKey();
        return !k.equals("method")
                && !k.equals("path_prefix");
    }

    private String makeCanonical(String s) {
        return s.toLowerCase().replace('-', '_');
    }


    public static final class Builder {
        private String accessId;
        private String access_secret;
        private int defaultAge = 900;
        private boolean skipUrlEncoding = false;

        public Builder() {
        }

        public Builder withAccessId(String accessId) {
            this.accessId = accessId;
            return this;
        }

        public Builder withAccessSecret(String access_secret) {
            this.access_secret = access_secret;
            return this;
        }

        public Builder withDefaultAge(int defaultAge) {
            this.defaultAge = defaultAge;
            return this;
        }

        public Builder withSkipUrlEncoding(boolean skipUrlEncoding) {
            this.skipUrlEncoding = skipUrlEncoding;
            return this;
        }

        public ByteArkV2UrlSigner build() {
            return new ByteArkV2UrlSigner(accessId, access_secret, defaultAge, skipUrlEncoding);
        }
    }
}
