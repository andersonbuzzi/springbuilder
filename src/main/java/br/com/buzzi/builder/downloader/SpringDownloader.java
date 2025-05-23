package br.com.buzzi.builder.downloader;

import br.com.buzzi.builder.config.SpringProject;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SpringDownloader {

    public static Path download(SpringProject config) throws Exception {
        try {
            String url = String.format(
                    "https://start.spring.io/starter.zip?type=maven-project&language=java&bootVersion=%s" +
                            "&baseDir=%s&groupId=%s&artifactId=%s&name=%s&packageName=%s&dependencies=%s&javaVersion=%s",
                    config.getBootVersion(),
                    config.getBaseDir(),
                    config.getGroupId(),
                    config.getArtifactId(),
                    config.getName(),
                    config.getPackageName(),
                    config.getDependencies(),
                    config.getJavaVersion()
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            Path zipPath = Paths.get(config.getBaseDir() + ".zip");

            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(zipPath));
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                String errorMessage = String.format(
                        "Error downloading project. Status: %d - URL: %s",
                        response.statusCode(),
                        url
                );
                throw new IOException(errorMessage);
            }
            return response.body();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return null;
    }

    public static void extract(Path zipFile, Path path) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newFile = path.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(newFile);
                } else {
                    Files.createDirectories(newFile.getParent());
                    try (OutputStream os = Files.newOutputStream(newFile)) {
                        zis.transferTo(os);
                    }
                }
            }
        }
    }

}
