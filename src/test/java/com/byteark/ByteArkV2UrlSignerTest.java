package com.byteark;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ByteArkV2UrlSignerTest {
    ByteArkV2UrlSigner signer;

    @BeforeEach
    void setup() {
        signer = new ByteArkV2UrlSigner.Builder()
                .withAccessId("2Aj6Wkge4hi1ZYLp0DBG")
                .withAccessSecret("31sX5C0lcBiWuGPTzRszYvjxzzI3aCZjJi85ZyB7")
                .build();
    }

    @Test
    void sign() {
        String signedUrl = signer.sign(
                "https://example.cdn.byteark.com/path/to/file.png",
                1514764800
        );
        assertEquals(
                "https://example.cdn.byteark.com/path/to/file.png?x_ark_access_id=2Aj6Wkge4hi1ZYLp0DBG&x_ark_auth_type=ark-v2&x_ark_expires=1514764800&x_ark_signature=OsBgZpn9LTAJowa0UUhlYQ",
                signedUrl);
    }

    @Test
    void signHLS() {
        Map<String, String> options = new HashMap<>();
        options.put("path_prefix", "/live/");
        String signedUrl = signer.sign(
                "https://example.cdn.byteark.com/live/playlist.m3u8",
                1514764800,
                options
        );
        assertEquals(
                "https://example.cdn.byteark.com/live/playlist.m3u8?x_ark_access_id=2Aj6Wkge4hi1ZYLp0DBG&x_ark_auth_type=ark-v2&x_ark_expires=1514764800&x_ark_path_prefix=%2Flive%2F&x_ark_signature=7JGsff2mBQEOoSYHTjxiVQ",
                signedUrl);
    }

    @Test
    void verify() throws MalformedURLException,
            ByteArkSignedUrlInvalidConditionException,
            ByteArkSignedUrlExpiredException,
            ByteArkSignedUrlInvalidSignatureException,
            ByteArkSignedUrlMissingParamException {

        signer.verify("https://example.cdn.byteark.com/path/to/file.png?x_ark_access_id=2Aj6Wkge4hi1ZYLp0DBG&x_ark_auth_type=ark-v2&x_ark_expires=1514764800&x_ark_signature=OsBgZpn9LTAJowa0UUhlYQ", 1514764700);
        signer.verify("https://example.cdn.byteark.com/live/playlist.m3u8?x_ark_access_id=2Aj6Wkge4hi1ZYLp0DBG&x_ark_auth_type=ark-v2&x_ark_expires=1514764800&x_ark_path_prefix=%2Flive%2F&x_ark_signature=7JGsff2mBQEOoSYHTjxiVQ", 1514764700);
        assertThrows(ByteArkSignedUrlExpiredException.class,
                () -> signer.verify("https://example.cdn.byteark.com/path/to/file.png?x_ark_access_id=2Aj6Wkge4hi1ZYLp0DBG&x_ark_auth_type=ark-v2&x_ark_expires=1514764800&x_ark_signature=OsBgZpn9LTAJowa0UUhlYQ", 1514769900)
        );
        assertThrows(ByteArkSignedUrlMissingParamException.class,
                () -> signer.verify("https://example.cdn.byteark.com/path/to/file.png?x_ark_access_id=2Aj6Wkge4hi1ZYLp0DBG&x_ark_auth_type=ark-v2&x_ark_signature=OsBgZpn9LTAJowa0UUhlYQ", 1514764700)
        );
        assertThrows(ByteArkSignedUrlMissingParamException.class,
                () -> signer.verify("https://example.cdn.byteark.com/path/to/file.png?x_ark_access_id=2Aj6Wkge4hi1ZYLp0DBG&x_ark_auth_type=ark-v2&x_ark_expires=1514764800", 1514764700)
        );
        assertThrows(ByteArkSignedUrlInvalidConditionException.class,
                () -> signer.verify("https://example.cdn.byteark.com/live2/playlist.m3u8?x_ark_access_id=2Aj6Wkge4hi1ZYLp0DBG&x_ark_auth_type=ark-v2&x_ark_expires=1514764800&x_ark_path_prefix=%2Flive%2F&x_ark_signature=7JGsff2mBQEOoSYHTjxiVQ", 1514761000)
        );
        assertThrows(ByteArkSignedUrlInvalidSignatureException.class,
                () -> signer.verify("https://example.cdn.byteark.com/live/playlist.m3u8?x_ark_access_id=2Aj6Wkge4hi1ZYLp0DBG&x_ark_auth_type=ark-v2&x_ark_expires=1514764800&x_ark_path_prefix=%2Flive%2F&x_ark_signature=7JGsff2mBQEOoSYHTjxiV4", 1514761000)
        );
    }

    @Test
    void makeQueryParams() {
        String signedQueryParams = signer.makeSignedQueryParams(
                "example.cdn.byteark.com",
                "/path/to/file.png",
                1514764800,
                null
        );
        assertEquals(
                "x_ark_access_id=2Aj6Wkge4hi1ZYLp0DBG&x_ark_auth_type=ark-v2&x_ark_expires=1514764800&x_ark_signature=OsBgZpn9LTAJowa0UUhlYQ",
                signedQueryParams);
    }
}
