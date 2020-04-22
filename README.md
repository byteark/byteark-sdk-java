# ByteArk SDK for java

![Maven Package](https://github.com/byteark/byteark-sdk-java/workflows/Maven%20Package/badge.svg)

## Table of Contents

* [Installation](#installation)
* [Usages](#usages)
* [Usage for HLS](#usage-for-hls)
* [Options](#options)

## Installation

For java 8+, You may install this SDK via [Maven](http://maven.apache.org/)

    <dependency>
      <groupId>com.byteark</groupId>
      <artifactId>byteark-sdk-java</artifactId>
      <version>0.1.0</version>
    </dependency>

## Usages

Now the only feature availabled is creating signed URL with ByteArk Signature Version 2.

First, create a ByteArkV2UrlSigner instance with accessId and accessSecret. (accessId is currently optional for ByteArk Fleet).

Then, call sign method with URL to sign, Unix timestamp that the URL should expired, and sign options.

For sign options argument, you may include method, which determines which HTTP method is allowed (GET is the default is not determined), and may includes custom policies.

### Example usage in Java

The following example will create a signed URL that allows to GET the resource within 1st January 2018:

```java
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ByteArkV2UrlSignerTest {

    @Test
    void sign() {
        ByteArkV2UrlSigner signer = new ByteArkV2UrlSigner.Builder()
                .withAccessId("2Aj6Wkge4hi1ZYLp0DBG")
                .withAccessSecret("31sX5C0lcBiWuGPTzRszYvjxzzI3aCZjJi85ZyB7")
                .build();
        String signedUrl = signer.sign(
                "https://example.cdn.byteark.com/path/to/file.png",
                1514764800
        );
        assertEquals(
                "https://example.cdn.byteark.com/path/to/file.png?x_ark_access_id=2Aj6Wkge4hi1ZYLp0DBG&x_ark_auth_type=ark-v2&x_ark_expires=1514764800&x_ark_signature=OsBgZpn9LTAJowa0UUhlYQ",
                signedUrl);
    }

    @Test
    void verify() throws MalformedURLException,
            ByteArkSignedUrlInvalidConditionException,
            ByteArkSignedUrlExpiredException,
            ByteArkSignedUrlInvalidSignatureException,
            ByteArkSignedUrlMissingParamException {

        ByteArkV2UrlSigner signer = new ByteArkV2UrlSigner.Builder()
                .withAccessId("2Aj6Wkge4hi1ZYLp0DBG")
                .withAccessSecret("31sX5C0lcBiWuGPTzRszYvjxzzI3aCZjJi85ZyB7")
                .build();
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

}
```

For more usage details, please visit [ByteArk Documentation](https://docs.byteark.com)

## Usage for HLS

When signing URL for HLS, you have to choose common path prefix
and assign to `path_prefix` option is required,
since ByteArk will automatically create secured URLs for each segments
using the same options and signature.

For example, if your stream URL is `https://example.cdn.byteark.com/live/playlist.m3u8`,
you may use `/live/` as a path prefix.

```java
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ByteArkV2UrlSignerTest {

    @Test
        void signHLS() {
            ByteArkV2UrlSigner signer = new ByteArkV2UrlSigner.Builder()
                    .withAccessId("2Aj6Wkge4hi1ZYLp0DBG")
                    .withAccessSecret("31sX5C0lcBiWuGPTzRszYvjxzzI3aCZjJi85ZyB7")
                    .build();
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
}
```

## Options

### ByteArkV2UrlSigner

| Option        | Required | Default | Description                                                               |
|---------------|----------|---------|---------------------------------------------------------------------------|
| accessId     | Required | -       | Access key ID for signing                                                 |
| acesssSecret | Required | -       | Access key secret for signing                                             |
| defaultAge   | -        | 900     | Default signed URL age (in seconds), if signing without expired date/time |

### ByteArkV2UrlSigner.sign(url, expires = null, options = [])

| Option      | Required | Default | Description                                                                                                                                                   |
|-------------|----------|---------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| method      | -        | GET     | HTTP Method that allowed to use with the signed URL                                                                                                           |
| path_prefix | -        | -       | Path prefix that allowed to use with the signed URL (the same signing options and signature can be reuse with the
