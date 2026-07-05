package com.docsynth.infrastructure.ingestion;

import com.docsynth.application.ingestion.SpecDownloadException;
import com.docsynth.domain.ingestion.SpecSource;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * UrlSpecDownloader — fetches a spec from a public URL.
 * Includes timeout (10s) and size cap (10 MB).
 */
@Component
public class UrlSpecDownloader {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(TIMEOUT)
        .build();

    public String download(SpecSource source) {
        if (!SpecSource.URL.equals(source.kind())) {
            throw new IllegalArgumentException("UrlSpecDownloader only supports url sources");
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(source.ref()))
                .timeout(TIMEOUT)
                .GET()
                .build();
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() / 100 != 2) {
                throw new SpecDownloadException("HTTP " + resp.statusCode() + " from " + source.ref());
            }
            byte[] body = resp.body();
            if (body.length > MAX_BYTES) {
                throw new SpecDownloadException("Spec exceeds 10 MB cap (" + body.length + " bytes)");
            }
            return new String(body, java.nio.charset.StandardCharsets.UTF_8);
        } catch (SpecDownloadException e) {
            throw e;
        } catch (Exception e) {
            throw new SpecDownloadException("Download failed: " + e.getMessage());
        }
    }

    public String downloadGitHubRaw(String ownerRepo, String ref, String specFile, String tokenRef) {
        String url = String.format(
            "https://raw.githubusercontent.com/%s/%s/%s",
            ownerRepo, ref, specFile
        );
        return download(SpecSource.url(url));
    }
}
