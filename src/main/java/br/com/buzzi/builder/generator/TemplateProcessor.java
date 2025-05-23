package br.com.buzzi.builder.generator;

import java.io.InputStream;
import java.util.Map;

public class TemplateProcessor {


    private TemplateProcessor() {

    }

    public static String fillTemplate(String filePath, Map<String, String> vars)  {
        String content = loadModelAsString(filePath);
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            content = content.replace(placeholder, entry.getValue());
        }
        return content;
    }

    public static InputStream loadModel(String relativePath) {
        InputStream stream = TemplateProcessor.class.getClassLoader().getResourceAsStream(relativePath);
        if (stream == null) {
            throw new RuntimeException("Resource not found: " + relativePath);
        }
        return stream;
    }

    public static String loadModelAsString(String relativePath) {
        try (InputStream is = loadModel(relativePath)) {
            return new String(is.readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException("Error reading resource: " + relativePath, e);
        }
    }



}
