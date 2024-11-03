# Spaceship API

## Description
RESTful API for managing a catalog of fictional spaceships. CRUD operations on spaceships, including features such 
as name-based filtering, pagination, and Kafka integration for event streaming.

## Key Features
- Full CRUD operations for spaceship management
- Pagination and sorting capabilities for efficient data retrieval
- Name-based search functionality
- Secure endpoints with basic authentication
- Swagger UI for API documentation and testing
- Actuator endpoints for monitoring and health checks
- Dockerized application for easy deployment
- Kafka integration for event streaming

## Technologies
- Java: 21
- Spring Boot: 3.3.5
- Spring Security: For API authentication
- H2 Database: In-memory database for development and testing
- Swagger/OpenAPI: For API documentation
- Docker: For containerization
- Maven: For project management and build automation
- Apache Kafka: For event streaming

## API Security
All API endpoints are secured with basic authentication:
```properties
 Username: user
 Password: pass
```

## API Endpoints
| Endpoint | Description                           |
|----------|---------------------------------------|
| GET /api/spaceships | Retrieve all spaceships (paginated)   |
| GET /api/spaceships/{id} | Get a specific spaceship by ID        |
| GET /api/spaceships/search | Search spaceships by name (paginated) |
| POST /api/spaceships | Create a new spaceship                |
| PUT /api/spaceships/{id} | Update an existing spaceship          |
| DELETE /api/spaceships/{id} | Delete a spaceship                    |
| DELETE /api/spaceships | Delete all spaceships                 |
|GET /api/spaceships/kafka| 	Retrieve all ships from Kafka topic  |

## Building and Running

### Local
1. Clone the repository
2. Run `mvn clean install` to build the project
3. Run `docker compose up` and stop `spaceship-app` container
4. Add VM option `-Dspring.kafka.bootstrap-servers=localhost:29092`
5. Start the application by running `SpaceshipApplication.java`

### Docker
1. Build the Docker image:
    * `mvn clean package`
    * `docker build -t spaceship .`
2. Run with Docker Compose:
   * `mvn clean package`
   * `docker compose up`

## Useful Links
- [Swagger UI](http://localhost:8080/swagger-ui/index.html)
- [Actuator](http://localhost:8080/actuator)
- [Kafka UI](http://localhost:8081/)