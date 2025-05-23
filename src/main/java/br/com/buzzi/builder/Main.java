package br.com.buzzi.builder;

import br.com.buzzi.builder.config.Properties;
import br.com.buzzi.builder.config.NewProject;
import br.com.buzzi.builder.config.SpringProject;
import br.com.buzzi.builder.downloader.SpringDownloader;
import br.com.buzzi.builder.generator.Class;
import br.com.buzzi.builder.generator.Migration;
import br.com.buzzi.builder.generator.Sql;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import static com.github.javaparser.utils.Utils.capitalize;


public class Main {

    public static Properties config;

    private static final Logger logger = Logger.getLogger(Main.class.getName());


    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            displayMainMenu();
            return;
        }

        String command = args[0].toLowerCase();

        switch (command) {
            case "start-springboot" -> {
                if (args.length != 1 && args.length < 10) help();
                startSpringboot(args);
            }
            case "add" -> addEntity();
            default -> help();
        }
    }


    private static void displayMainMenu() {
        Scanner scanner = new Scanner(System.in);
        String option = "";

        while (true) {
            System.out.println("Welcome to the Java Builder");
            System.out.println("What do you want to do?");
            System.out.println("1 - Create a new Spring Boot project");
            System.out.println("2 - Add a new entity to an existing project");
            System.out.print("Choose [1/2]: ");

            option = scanner.nextLine().trim();

            if ("1".equals(option)) {
                try {
                    startSpringboot(new String[]{"start-springboot"});
                } catch (Exception e) {
                    logger.warning("Error creating Spring Boot project: " + e.getMessage());
                }
                break;
            } else if ("2".equals(option)) {
                addEntity();
                break;
            } else {
                System.out.println("Invalid option. Please enter 1 or 2.");
            }
        }
    }



    private static void startSpringboot(String[] args) throws Exception {
        SpringProject springProject;

        if (args.length == 1) {
            springProject = askUserForConfig();
        } else {
            springProject = new SpringProject(
                    args[1], // springVersion
                    args[2], // javaVersion
                    args[3], // baseDir
                    args[4], // groupId
                    args[5], // artifactId
                    args[6], // name
                    args[7], // packageName
                    args[8], // dependencies
                    args[9], // createDir
                    args[10],// dbUrl
                    args[11],// schema
                    args[12],// user
                    args[13] // password
            );
        }

        Path zipFile = SpringDownloader.download(springProject);

        Path projectPath = Paths.get(springProject.getCreateDir(), springProject.getBaseDir());
        Path pomPath = projectPath.resolve("pom.xml");
        Path propertiesPath = projectPath.resolve("src/main/resources/application.properties");

        System.out.println("Extracting project to: " + projectPath.toAbsolutePath());

        SpringDownloader.extract(zipFile, Paths.get(springProject.getCreateDir()));
        NewProject.addDependencies(pomPath.toString());
        NewProject.addProperties(
                propertiesPath.toString(),
                springProject.getDbUrl(),
                springProject.getUser(),
                springProject.getPassword(),
                springProject.getSchema()
        );

        System.out.println("Spring Boot project successfully generated in: " + springProject.getBaseDir());

        Path builderFile = Paths.get("builder.properties");

        String content = String.join("\n", List.of(
                "builder.path=" + springProject.getCreateDir() + springProject.getBaseDir(),
                "builder.package=" + springProject.getPackageName(),
                "builder.entitypath=/entity",
                "builder.controllerpath=/controller",
                "builder.servicepath=/service",
                "builder.repositorypath=/repository",
                "builder.dtopath=/dto",
                "builder.mapperpath=/mapper",
                "builder.migrationpath=/src/main/resources/db/migration"
        ));

        Files.writeString(builderFile, content);
        System.out.println("builder.properties created at: " + builderFile.toAbsolutePath());

        Scanner scanner = new Scanner(System.in);
        System.out.print("Do you want to add an entity now? [y/N]: ");
        String response = scanner.nextLine().trim().toLowerCase();

        while (response.equals("y") || response.equals("yes")) {
            addEntity();
            System.out.print("Do you want to add another entity? [y/N]: ");
            response = scanner.nextLine().trim().toLowerCase();
        }
    }

    // método extraído para reutilização
    private static void runEntityWizardLoop() {
        config = new Properties(new String[]{"add"});
        Scanner scanner = new Scanner(System.in);

        while (true) {
            String entityName;
            String fields;

            System.out.println("---------------------------------------------");
            System.out.println("Welcome to the Entity Generator!");
            System.out.println("This will create an entity with DTOs, mapper, controller, service, and migration.");
            System.out.println("Type 'exit' to cancel.\n");
            System.out.println("Using configuration from: " + config.getPathProperties());

            System.out.print("\nEntity name: ");
            entityName = scanner.nextLine().trim();
            if (entityName.equalsIgnoreCase("exit") || entityName.isEmpty()) {
                System.out.println("Operation canceled.");
                break;
            }

            StringBuilder fieldBuilder = new StringBuilder();
            int fieldIndex = 1;
            while (true) {
                System.out.print("Field " + fieldIndex + " (e.g., name:String): ");
                String field = scanner.nextLine().trim();

                if (field.equalsIgnoreCase("exit")) {
                    System.out.println("Operation canceled.");
                    return;
                }

                if (field.isEmpty()) break;

                fieldBuilder.append(field).append(";");
                fieldIndex++;
            }

            fields = fieldBuilder.toString();

            try {
                Class classGenerator = new Class(entityName);
                classGenerator.generateFiles(fields);

                Path entityFilePath = Paths.get(config.getPath(), config.getEntityPath(), capitalize(entityName) + ".java");

                Sql sqlGenerator = new Sql();
                sqlGenerator.setFilename(entityFilePath.toString());

                Migration migration = new Migration(config.getPath(), config.getMigrationPath());
                migration.saveMigration(sqlGenerator.getSqlContent(), capitalize(entityName));

                System.out.println("Entity " + capitalize(entityName) + " successfully created.");
            } catch (Exception e) {
                logger.warning("Error while creating the entity: " + e.getMessage());
            }

            System.out.print("Do you want to add another entity? [y/N]: ");
            String again = scanner.nextLine().trim().toLowerCase();
            if (!again.equals("y") && !again.equals("yes")) break;
        }
    }

    private static void addEntity() {
        runEntityWizardLoop();
    }

    private static SpringProject askUserForConfig() {
        System.out.println("\nWelcome to the Spring Boot Project Generator!");
        System.out.println("This tool will guide you step-by-step to create a standard Spring Boot project.");
        System.out.println("Answer the prompts below to customize your project.");
        System.out.println("Type 'exit' to cancel at any time.");
        System.out.println("Press <Enter> to use the default values.");
        System.out.println("--------------------------------------------------");

        Scanner scanner = new Scanner(System.in);

        String bootVersion = getNextLine(scanner, "Spring Boot version", "3.4.5");
        String javaVersion = getNextLine(scanner, "Java version", "21");
        String baseDir = getNextLine(scanner, "Project base directory name", "demo");
        String packageName = getNextLine(scanner, "Base package name", "com.example.demo");

        String groupId = packageName.substring(0, packageName.lastIndexOf('.'));
        String artifactId = packageName.substring(packageName.lastIndexOf('.') + 1);
        System.out.println("Derived Group ID: " + groupId);
        System.out.println("Derived Artifact ID: " + artifactId);
        System.out.println("Project Name: " + artifactId);

        String dependencies = getNextLine(scanner, "Dependencies (comma-separated)", "web,data-jpa,postgresql,lombok");
        String createDir = getNextLine(scanner, "Target directory", "./");

        System.out.println("Configuring Postgres");
        String server = getNextLine(scanner, "PostgreSQL server", "localhost");
        String port = getNextLine(scanner, "Port", "5432");
        String database = getNextLine(scanner, "Database name", "postgres");
        String dbUrl = "jdbc:postgresql://" + server + ":" + port + "/" + database;
        String schema = getNextLine(scanner, "Database schema", "public");
        String user = getNextLine(scanner, "Database user", "");
        String password = getNextLine(scanner, "Database password", "");


        if (!createDir.endsWith("/")) createDir = createDir + "/";

        return new SpringProject(
                bootVersion,
                javaVersion,
                baseDir,
                groupId,
                artifactId,
                artifactId,
                packageName,
                dependencies,
                createDir,
                dbUrl,
                schema,
                user,
                password
        );
    }

    private static String getNextLine(Scanner scanner, String prompt, String defaultValue) {
        System.out.print(prompt + " [" + defaultValue + "]: ");
        String input = scanner.nextLine().trim();

        if ("exit".equalsIgnoreCase(input)) {
            System.out.println("Configuration canceled.");
            System.exit(1);
        }

        return input.isEmpty() ? defaultValue : input;
    }

    private static void help() {
        logger.warning("\n==================== HELP ====================\n");

        logger.warning("To generate a new Spring Boot project:");
        logger.warning("  java -jar builder.jar start-springboot <springVersion> <javaVersion> <baseDir> <groupId> <artifactId> <name> <packageName> <dependencies> <createDir>");

        logger.warning("\nExample:");
        logger.warning("  java -jar builder.jar start-springboot 3.4.5 21 demo com.example demo demo com.example.demo web,data-jpa,security ./");

        logger.warning("\nInteractive mode:");
        logger.warning("  java -jar builder.jar start-springboot");

        logger.warning("\n---------------------------------------------\n");

        logger.warning("To add a new entity to the project (with mapper, DTO, controller, service, and migration):");
        logger.warning("  java -jar builder.jar add <EntityName> <field1:type> <field2:type> ... <fieldN:type>");

        logger.warning("\nExamples:");
        logger.warning("  java -jar builder.jar add Company name:String");
        logger.warning("  java -jar builder.jar add Branch name:String company:Company foundingDate:LocalDate");

        logger.warning("\nInteractive mode:");
        logger.warning("  java -jar builder.jar add");

        logger.warning("\n=============================================\n");

        System.exit(1);
    }

}