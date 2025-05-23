package br.com.buzzi.builder.config;

import br.com.buzzi.builder.generator.LoadFile;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Getter
@Setter
@ToString
public class Properties {

    private String packageName;
    private String path;
    private String entityPath;
    private String controllerPath;
    private String servicePath;
    private String dtoPath;
    private String repositoryPath;
    private String migrationPath;
    private Boolean generatePermissions = false;
    private String modelPath;
    private String mapperPath;
    private String[] appArgs;
    private String pathProperties;

    public Properties(String[] args) {
        this.appArgs = args;

        java.util.Properties config = loadConfig();
        final String BUILDER_PREFIX = "builder.";

        packageName = getRequired(config, BUILDER_PREFIX + "package");
        path = getRequired(config, BUILDER_PREFIX + "path");

        String initialDir = "/src/main/java/" + packageName.replace(".", "/");

        entityPath = initialDir + getPath(config, BUILDER_PREFIX + "entitypath");
        controllerPath = initialDir + getPath(config, BUILDER_PREFIX + "controllerpath");
        servicePath = initialDir + getPath(config, BUILDER_PREFIX + "servicepath");
        dtoPath = initialDir + getPath(config, BUILDER_PREFIX + "dtopath");
        repositoryPath = initialDir + getPath(config, BUILDER_PREFIX + "repositorypath");
        mapperPath = initialDir + getPath(config, BUILDER_PREFIX + "mapperpath");

        migrationPath = getPath(config, BUILDER_PREFIX + "migrationpath");

        modelPath = getPath(config, BUILDER_PREFIX + "modelpath");

        String usePermissions = config.getProperty(BUILDER_PREFIX + "generatepermission");
        if ("1".equalsIgnoreCase(usePermissions)) {
            generatePermissions = true;
        } else if (usePermissions != null) {
            generatePermissions = Boolean.parseBoolean(usePermissions);
        }
    }

    private String getRequired(java.util.Properties config, String key) {
        String value = config.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new RuntimeException("Missing required property: " + key);
        }
        return value;
    }

    private String getPath(java.util.Properties config, String key) {
        String value = config.getProperty(key);
        return (value != null) ? value.trim() : "";
    }

    public java.util.Properties loadConfig() {
        java.util.Properties props = new java.util.Properties();
        try {
            File file = LoadFile.getFile("builder.properties");
            try (InputStream input = new FileInputStream(file)) {
                props.load(input);
                pathProperties = file.getAbsolutePath();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading builder.properties: " + e.getMessage(), e);
        }
        return props;
    }
}
