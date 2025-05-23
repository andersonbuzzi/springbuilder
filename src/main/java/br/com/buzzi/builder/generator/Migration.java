package br.com.buzzi.builder.generator;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.OptionalInt;

@Getter
@Setter
public class Migration {

    private String path;

    public Migration(String path, String migrationPath) {
        Path fullPath = Paths.get(path, migrationPath);
        this.path = fullPath.toString();
        try {
            Files.createDirectories(fullPath); // garante que o diretório exista
        } catch (Exception e) {
            throw new RuntimeException("Failed to create migration directory: " + fullPath, e);
        }
    }

    public void saveMigration(String content, String entity) {
        String file = getNextMigrationFile() + entity.toLowerCase() + ".sql";
        try {
            Path entityPath = new File(this.path, file).toPath();
            Files.write(entityPath, Collections.singletonList(content), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }

    }

    private String getNextMigrationFile() {
        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }

        File[] files = folder.listFiles((dir, name) -> name.matches("V\\d+__.*\\.sql"));

        if (files == null || files.length == 0) {
            return "V1__create_";
        }

        OptionalInt maxVersion = Arrays.stream(files)
                .map(File::getName)
                .map(name -> name.substring(1, name.indexOf("__")))
                .mapToInt(Integer::parseInt)
                .max();

        int nextVersion = maxVersion.orElse(0) + 1;
        return "V" + nextVersion + "__create_";
    }

}
