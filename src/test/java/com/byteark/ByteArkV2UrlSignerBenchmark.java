package com.byteark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
public class ByteArkV2UrlSignerBenchmark {

    private ByteArkV2UrlSigner signer;

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ByteArkV2UrlSignerBenchmark.class.getName() + ".*")
                .forks(1)
                .jvmArgs("-Xmx64M", "-Xmx64M")
                .mode(Mode.Throughput)
                .timeUnit(TimeUnit.SECONDS)
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        signer = new ByteArkV2UrlSigner.Builder()
                .withAccessId("2Aj6Wkge4hi1ZYLp0DBG")
                .withAccessSecret("31sX5C0lcBiWuGPTzRszYvjxzzI3aCZjJi85ZyB7")
                .build();
    }

    @Benchmark
    public void benchmarkSign() {
        String signedUrl = signer.sign(
                "https://example.cdn.byteark.com/path/to/file.png",
                1514764800
        );
    }

    @Benchmark
    public void benchmarkVerify() throws MalformedURLException, ByteArkSignedUrlMissingParamException, ByteArkSignedUrlInvalidConditionException, ByteArkSignedUrlExpiredException, ByteArkSignedUrlInvalidSignatureException {
        signer.verify("https://example.cdn.byteark.com/path/to/file.png?x_ark_access_id=2Aj6Wkge4hi1ZYLp0DBG&x_ark_auth_type=ark-v2&x_ark_expires=1514764800&x_ark_signature=OsBgZpn9LTAJowa0UUhlYQ", 1514764700);
    }
}
