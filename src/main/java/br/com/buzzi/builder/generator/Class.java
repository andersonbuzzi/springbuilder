package br.com.buzzi.builder.generator;

import br.com.buzzi.builder.Main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Class {

    public static final String ENTITY = "entity";
    public static final String IMPORT = "import";
    public static final String MAPPER = "mapper";
    private String entityName = "";

    private final StringBuilder entityFields = new StringBuilder();
    private final StringBuilder dtoFields = new StringBuilder();
    private final Set<String> importsEntity = new TreeSet<>();
    private final Set<String> importsDTO = new TreeSet<>();
    private final Set<String> importsMapper = new TreeSet<>();
    private final Set<String> usedMappers = new TreeSet<>();

    public Class(String entityName) throws IOException {
        this.entityName = entityName;
        createBaseDirectories();
    }

    private void createBaseDirectories() throws IOException {
        List<String> paths = List.of(
                Main.config.getRepositoryPath(),
                Main.config.getControllerPath(),
                Main.config.getServicePath(),
                Main.config.getEntityPath(),
                Main.config.getMapperPath(),
                Main.config.getDtoPath()
        );

        for (String path : paths) {
            Files.createDirectories(Paths.get(Main.config.getPath(), path));
        }
    }

    public void generateFiles(String fields) throws Exception {
        parseFields(fields);
        for (String type : List.of("repository", "service", "controller", ENTITY, MAPPER)) {
            writeFileFromTemplate(type, type + ".model", getOutputFilename(type));
        }
        writeDtoFiles();
    }

    private String getOutputFilename(String type) {
        String suffix = switch (type.toLowerCase()) {
            case ENTITY -> "";
            default -> capitalize(type); // Repository, Service, Controller, Mapper
        };
        return capitalize(entityName) + suffix;
    }

    private void writeDtoFiles() throws Exception {
        String capitalizedEntity = capitalize(entityName);

        // DTO principal (ex: EmpresaDTO.java)
        writeFileFromTemplate("dto", "dto.model", capitalizedEntity + "DTO");

        // DTO de criação (ex: NewEmpresaDTO.java)
        writeFileFromTemplate("dto", "newdto.model", "New" + capitalizedEntity + "DTO");
    }

    private void writeFileFromTemplate(String type, String modelName, String filenameWithoutExtension) throws IOException {
        Map<String, String> vars = buildTemplateVars(type);
        String templatePath = "models/" + modelName;
        String content = TemplateProcessor.fillTemplate(templatePath, vars);

        Path destination = Paths.get(Main.config.getPath(), getTargetDirectory(type), filenameWithoutExtension + ".java");
        Files.write(destination, List.of(content), StandardCharsets.UTF_8);
    }
    private Map<String, String> buildTemplateVars(String type) {
        String basePackage = Main.config.getPackageName();

        return Map.of(
                "Entity", capitalize(entityName),
                ENTITY, entityName.toLowerCase(),
                "package", "package " + basePackage + "." + type.toLowerCase() + ";",
                "packagename", basePackage,
                "imports", getImports(type),
                "usesmapper", getUsesMapper(),
                "fields", getFieldBlock(type)
        );
    }

    private String getTargetDirectory(String type) {
        return switch (type.toLowerCase()) {
            case "controller" -> Main.config.getControllerPath();
            case "service" -> Main.config.getServicePath();
            case MAPPER -> Main.config.getMapperPath();
            case ENTITY -> Main.config.getEntityPath();
            case "dto" -> Main.config.getDtoPath();
            default -> Main.config.getRepositoryPath();
        };
    }

    private String getImports(String type) {
        return switch (type.toLowerCase()) {
            case ENTITY -> String.join("\n", importsEntity);
            case "dto" -> String.join("\n", importsDTO);
            case MAPPER -> String.join("\n", importsMapper);
            default -> "";
        };
    }

    private String getFieldBlock(String type) {
        return switch (type.toLowerCase()) {
            case ENTITY -> entityFields.toString();
            case "dto" -> dtoFields.toString();
            default -> "";
        };
    }

    private void parseFields(String fields) {
        String[] items = fields.split(";");
        for (int i = 0; i < items.length; i++) {
            String[] parts = items[i].trim().split(":");
            if (parts.length != 2) continue;

            String name = parts[0].trim();
            String type = parts[1].trim();
            boolean isLast = (i == items.length - 1);
            String comma = isLast ? "" : ",";

            if (name.equalsIgnoreCase("id")) continue;

            if (isEntityType(type)) {
                entityFields.append("    @ManyToOne\n")
                        .append("    @JoinColumn(name = \"").append(name.toLowerCase()).append("_id\")\n");
                dtoFields.append("    ").append(type).append("DTO ").append(name).append(comma).append("\n");

                addRelatedImports(type);
                usedMappers.add(type + "Mapper.class");

            } else {
                if (!name.equals(name.toLowerCase())) {
                    entityFields.append("    @Column(name = \"").append(toSnakeCase(name)).append("\")\n");
                }
                dtoFields.append("    ").append(type).append(" ").append(name).append(comma).append("\n");
                addPrimitiveImport(type);
            }

            entityFields.append("    private ").append(type).append(" ").append(name).append(";\n\n");
        }
    }

    private boolean isEntityType(String type) {
        return !Set.of("String", "int", "long", "double", "boolean", "UUID", "LocalDate", "LocalDateTime", "Boolean", "Integer", "Long", "Double").contains(type);
    }

    private void addPrimitiveImport(String type) {
        switch (type) {
            case "UUID" -> importsEntity.add("import java.util.UUID;");
            case "LocalDate" -> {
                importsEntity.add("import java.time.LocalDate;");
                importsDTO.add("import java.time.LocalDate;");
            }
            case "LocalDateTime" -> {
                importsEntity.add("import java.time.LocalDateTime;");
                importsDTO.add("import java.time.LocalDateTime;");
            }
            default -> {
                //
            }
        }
    }

    private void addRelatedImports(String type) {
        String pkg = Main.config.getPackageName();
        String cap = capitalize(type);
        importsEntity.add(IMPORT + "  " + pkg + ".entity." + cap + ";");
        importsDTO.add(IMPORT + "  " + pkg + ".dto." + cap + "DTO;");
        importsMapper.add(IMPORT + "  " + pkg + ".mapper." + cap + "Mapper;");
    }

    private String getUsesMapper() {
        return usedMappers.isEmpty() ? "" : ", uses = { " + String.join(", ", usedMappers) + " }";
    }

    private String toSnakeCase(String camel) {
        return camel.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private String capitalize(String text) {
        return (text == null || text.isEmpty()) ? text : text.substring(0, 1).toUpperCase() + text.substring(1);
    }

}
