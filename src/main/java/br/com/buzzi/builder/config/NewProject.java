package br.com.buzzi.builder.config;

import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public class NewProject {

    public static void addProperties(String propFilePath, String dbUrl, String dbUser, String dbPassword, String dbSchema) {
        List<String> lines = List.of(
                "spring.datasource.url=" + dbUrl,
                "spring.datasource.username=" + dbUser,
                "spring.datasource.password=" + dbPassword,
                "spring.flyway.schemas=" + dbSchema,
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "spring.flyway.baseline-on-migrate=true"
        );

        try {
            Path path = Paths.get(propFilePath);
            Files.write(path, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to " + propFilePath, e);
        }
    }

    public static void addDependencies(String pomFilePath) {
        try {
            Model model;
            try (FileReader reader = new FileReader(pomFilePath)) {
                model = new MavenXpp3Reader().read(reader);
            }

            if (model == null) throw new IllegalStateException("Model is null");

            // Adiciona dependências novas
            addNewDependencies(model);

            // Garante build/plugins/configuração
            addAnnotationProcessorPaths(model);

            // Salva alterações
            try (FileWriter writer = new FileWriter(pomFilePath)) {
                new MavenXpp3Writer().write(writer, model);
            }

            System.out.println("Dependencies and processor paths added to: " + pomFilePath);

        } catch (Exception e) {
            throw new RuntimeException("Failed to modify pom.xml at " + pomFilePath, e);
        }
    }

    private static void addAnnotationProcessorPaths(Model model) {
        Build build = model.getBuild();
        if (build == null) {
            build = new Build();
            model.setBuild(build);
        }

        Build finalBuild = build;
        Plugin compilerPlugin = build.getPlugins().stream()
                .filter(p -> "maven-compiler-plugin".equals(p.getArtifactId()))
                .findFirst()
                .orElseGet(() -> {
                    Plugin plugin = new Plugin();
                    plugin.setGroupId("org.apache.maven.plugins");
                    plugin.setArtifactId("maven-compiler-plugin");
                    plugin.setVersion("3.11.0");
                    finalBuild.addPlugin(plugin);
                    return plugin;
                });

        Xpp3Dom configuration = (Xpp3Dom) compilerPlugin.getConfiguration();
        if (configuration == null) {
            configuration = new Xpp3Dom("configuration");
            compilerPlugin.setConfiguration(configuration);
        }

        Xpp3Dom annotationProcessorPaths = configuration.getChild("annotationProcessorPaths");
        if (annotationProcessorPaths == null) {
            annotationProcessorPaths = new Xpp3Dom("annotationProcessorPaths");
            configuration.addChild(annotationProcessorPaths);
        }

        addOrUpdateAnnotationPath(annotationProcessorPaths, "org.projectlombok", "lombok", "1.18.38");
        addOrUpdateAnnotationPath(annotationProcessorPaths, "org.mapstruct", "mapstruct-processor", "1.5.5.Final");
    }

    private static void addOrUpdateAnnotationPath(Xpp3Dom paths, String groupId, String artifactId, String version) {
        for (Xpp3Dom child : paths.getChildren()) {
            if (groupId.equals(getChildValue(child, "groupId")) &&
                    artifactId.equals(getChildValue(child, "artifactId"))) {

                Xpp3Dom versionNode = child.getChild("version");
                if (versionNode == null) {
                    versionNode = new Xpp3Dom("version");
                    child.addChild(versionNode);
                }
                versionNode.setValue(version);
                return;
            }
        }

        // Se não encontrou, adiciona novo
        paths.addChild(createPath(groupId, artifactId, version));
    }

    private static String getChildValue(Xpp3Dom node, String childName) {
        Xpp3Dom child = node.getChild(childName);
        return (child != null) ? child.getValue() : null;
    }

    private static Xpp3Dom createPath(String groupId, String artifactId, String version) {
        Xpp3Dom path = new Xpp3Dom("path");

        Xpp3Dom g = new Xpp3Dom("groupId");
        g.setValue(groupId);
        path.addChild(g);

        Xpp3Dom a = new Xpp3Dom("artifactId");
        a.setValue(artifactId);
        path.addChild(a);

        if (version != null) {
            Xpp3Dom v = new Xpp3Dom("version");
            v.setValue(version);
            path.addChild(v);
        }

        return path;
    }

    private static void addNewDependencies(Model model) {
        List<Dependency> dependencies = model.getDependencies();
        dependencies.add(createDependency("org.mapstruct", "mapstruct", "1.5.5.Final"));
        dependencies.add(createDependency("org.mapstruct", "mapstruct-processor", "1.5.5.Final"));
        dependencies.add(createDependency("org.flywaydb", "flyway-core", "11.8.2"));
        dependencies.add(createDependency("org.flywaydb", "flyway-database-postgresql", "11.8.2"));
    }

    private static Dependency createDependency(String groupId, String artifactId, String version) {
        Dependency dep = new Dependency();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setVersion(version);
        return dep;
    }
}
