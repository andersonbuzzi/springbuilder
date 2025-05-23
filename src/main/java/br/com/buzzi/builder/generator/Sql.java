package br.com.buzzi.builder.generator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.type.Type;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

@Getter
@Setter
public class Sql {

    private Map<String, String> javaToSqlMap = new HashMap<>();
    private String filename;

    public Sql() {
        javaToSqlMap.put("String", "VARCHAR");
        javaToSqlMap.put("Long", "BIGINT");
        javaToSqlMap.put("Integer", "INT");
        javaToSqlMap.put("UUID", "UUID");
        javaToSqlMap.put("LocalDate", "DATE");
        javaToSqlMap.put("LocalDateTime", "TIMESTAMP");
        javaToSqlMap.put("Boolean", "BOOLEAN");
        javaToSqlMap.put("Double", "DOUBLE");
    }

    public String getSqlContent() throws Exception {
        File file = new File(filename);
        String content = Files.readString(file.toPath());
        StringBuilder sb = new StringBuilder();

        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> result = parser.parse(content);

        if (result.isSuccessful() && result.getResult().isPresent()) {
            if (result.getResult().isPresent()) {
                CompilationUnit cu = result.getResult().get();
                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                    if (!cls.isAnnotationPresent("Entity")) return;

                    String tableName = cls.getNameAsString().toLowerCase();
                    sb.append("CREATE TABLE ").append(tableName).append(" (\n");

                    List<String> columns = new ArrayList<>();
                    List<String> foreignKeys = new ArrayList<>();

                    for (FieldDeclaration field : cls.getFields()) {
                        String fieldName = field.getVariable(0).getNameAsString();
                        Type javaType = field.getElementType();
                        String typeName = javaType.isClassOrInterfaceType()
                                ? javaType.asClassOrInterfaceType().getNameAsString()
                                : javaType.asString();

                        boolean isId = field.isAnnotationPresent("Id");
                        boolean isFk = field.isAnnotationPresent("ManyToOne");
                        String sqlType = isFk ? "UUID" : javaToSqlMap.getOrDefault(typeName, "TEXT");

                        // ↓ resolve nome da coluna final
                        String columnName = resolveColumnName(field, fieldName, isFk);

                        boolean isNullable = true;
                        boolean isUnique = false;
                        int length = 255;

                        Optional<AnnotationExpr> columnAnn = field.getAnnotationByName("Column");
                        if (columnAnn.isPresent() && columnAnn.get().isNormalAnnotationExpr()) {
                            for (MemberValuePair pair : columnAnn.get().asNormalAnnotationExpr().getPairs()) {
                                switch (pair.getNameAsString()) {
                                    case "nullable" -> isNullable = Boolean.parseBoolean(pair.getValue().toString());
                                    case "unique" -> isUnique = Boolean.parseBoolean(pair.getValue().toString());
                                    case "length" -> length = Integer.parseInt(pair.getValue().toString());
                                    default -> {
                                        //
                                    }
                                }
                            }
                        }

                        if (sqlType.equals("VARCHAR")) sqlType += "(" + length + ")";

                        StringBuilder line = new StringBuilder("    ").append(columnName).append(" ").append(sqlType);
                        if (isId) line.append(" PRIMARY KEY");
                        if (!isNullable) line.append(" NOT NULL");
                        if (isUnique) line.append(" UNIQUE");

                        columns.add(line.toString());

                        // foreign key
                        if (isFk) {
                            String referencedTable = typeName.toLowerCase();
                            String referencedColumn = "id";

                            Optional<AnnotationExpr> joinAnn = field.getAnnotationByName("JoinColumn");
                            if (joinAnn.isPresent() && joinAnn.get().isNormalAnnotationExpr()) {
                                for (MemberValuePair pair : joinAnn.get().asNormalAnnotationExpr().getPairs()) {
                                    if (pair.getNameAsString().equals("referencedColumnName")) {
                                        referencedColumn = pair.getValue().toString().replace("\"", "");
                                    }
                                }
                            }

                            foreignKeys.add("    FOREIGN KEY (" + columnName + ") REFERENCES " +
                                    referencedTable + "(" + referencedColumn + ")");
                        }
                    }

                    sb.append(String.join(",\n", columns));
                    if (!foreignKeys.isEmpty()) {
                        sb.append(",\n").append(String.join(",\n", foreignKeys));
                    }

                    sb.append("\n);\n\n");
                });
            }
        }

        return sb.toString();
    }

    private String resolveColumnName(FieldDeclaration field, String defaultName, boolean isManyToOne) {
        // Se tiver @JoinColumn(name = "..."), usa ele
        Optional<AnnotationExpr> joinAnn = field.getAnnotationByName("JoinColumn");
        if (joinAnn.isPresent() && joinAnn.get().isNormalAnnotationExpr()) {
            for (MemberValuePair pair : joinAnn.get().asNormalAnnotationExpr().getPairs()) {
                if (pair.getNameAsString().equals("name")) {
                    return pair.getValue().toString().replace("\"", "");
                }
            }
        }

        // Se for @ManyToOne sem @JoinColumn, usa padrão: campo + "_id"
        if (isManyToOne) {
            return defaultName + "_id";
        }

        // Se tiver @Column(name = "..."), usa ele
        Optional<AnnotationExpr> columnAnn = field.getAnnotationByName("Column");
        if (columnAnn.isPresent() && columnAnn.get().isNormalAnnotationExpr()) {
            for (MemberValuePair pair : columnAnn.get().asNormalAnnotationExpr().getPairs()) {
                if (pair.getNameAsString().equals("name")) {
                    return pair.getValue().toString().replace("\"", "");
                }
            }
        }

        return defaultName;
    }
}
