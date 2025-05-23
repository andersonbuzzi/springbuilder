package br.com.buzzi.builder.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpringProject {
    private String bootVersion;
    private String javaVersion;
    private String baseDir;
    private String groupId;
    private String artifactId;
    private String name;
    private String packageName;
    private String dependencies;
    private String createDir;
    private String dbUrl;
    private String schema;
    private String user;
    private String password;
}