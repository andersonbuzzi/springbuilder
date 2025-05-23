# Java Spring Boot Project Builder

This command-line tool helps you quickly bootstrap new Spring Boot projects and generate standard entities with associated layers such as DTOs, Mappers, Controllers, Services, Repositories, and database migrations.

## Features

- Interactive or argument-based project generation.
- Automatic download and setup of Spring Boot starter projects.
- Generates fully wired entity classes including:
  - Java Entity
  - DTO
  - Mapper
  - Controller
  - Service
  - Repository
  - SQL migration script
- Generates a `builder.properties` file to store project configuration.

## Requirements

- Java 21+
- Maven

## Getting Started

### 1. Build the JAR

Clone the repository and run:

```bash
mvn clean package
```

This will generate a file like `target/springboot-builder.jar`

### 2. Create a New Spring Boot Project

Run in interactive mode:

```bash
java -jar target/builder-1.0.jar start-springboot
```

Or with arguments:

```bash
java -jar target/builder-1.0.jar start-springboot 3.4.5 21 demo com.example demo demo com.example.demo web,data-jpa,postgresql,lombok ./ localhost 5432 postgres public dbuser dbpassword
```

### 3. Add an Entity

Interactive mode:

```bash
java -jar target/builder-1.0.jar add
```

Or via command-line arguments:

```bash
java -jar target/builder-1.0.jar add Company name:String
java -jar target/builder-1.0.jar add Branch name:String company:Company foundingDate:LocalDate
```

This will generate:
- `Entity.java`
- `EntityDTO.java`
- `EntityMapper.java`
- `EntityService.java`
- `EntityController.java`
- Flyway migration script (`V__x__create_{Entity}.sql`, where `x` is the next version based on existing migrations)


## Configuration

After creating the project, a `builder.properties` file is generated to define paths for each component (entities, controllers, services, etc.). This file is used by the entity generator to locate and organize the source code.

Example:

```properties
builder.path=./demo
builder.package=com.example.demo
builder.entitypath=/entity
builder.controllerpath=/controller
builder.servicepath=/service
builder.repositorypath=/repository
builder.dtopath=/dto
builder.mapperpath=/mapper
builder.migrationpath=/src/main/resources/db/migration
```

## Notes

- This tool is ideal for backend developers who prefer working from the terminal.
- The SQL script is generated based on the entity definition and placed under the Flyway migrations folder.

## License

This project is open source and contributions are welcome. Feel free to fork, copy, or submit pull requests.
