# Driver Service

A Spring Boot microservice for managing driver locations and trip acceptance in the UIT-GO rideshare platform.

## Overview

The Driver Service is part of a microservices architecture that handles driver-related operations including location tracking, driver status management, and trip acceptance. It integrates with other services through Apache Kafka messaging and provides RESTful APIs for driver management.

## Features

- **Driver Location Management**: Track and update driver coordinates in real-time
- **Driver Status Control**: Turn drivers on/off for availability
- **Trip Acceptance**: Handle driver trip acceptance through event-driven architecture
- **Security**: JWT-based authentication and authorization
- **Event-Driven**: Kafka integration for inter-service communication
- **Database**: MongoDB for driver data persistence

## Technology Stack

- **Java 17**
- **Spring Boot 3.4.6**
- **Spring Security** - Authentication and authorization
- **Spring Data MongoDB** - Database operations
- **Spring Kafka** - Event streaming
- **OpenFeign** - Inter-service communication
- **JWT (JSON Web Tokens)** - Security tokens
- **Lombok** - Code generation
- **Maven** - Dependency management

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MongoDB database
- Apache Kafka
- Docker (optional, for containerized dependencies)

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd driver-service
```

### 2. Configure Dependencies

Ensure you have the following services running:

- **MongoDB**: Default connection `mongodb://admin:admin123@localhost:27017/driver-db`
- **Apache Kafka**: Default broker `localhost:29092`

### 3. Configuration

Update `src/main/resources/application.properties` if needed:

```properties
# Application
spring.application.name=driver-service
server.port=3031

# MongoDB
spring.data.mongodb.uri=mongodb://admin:admin123@localhost:27017/driver-db?authSource=admin&retryWrites=true&w=majority

# Kafka
spring.kafka.bootstrap-servers=localhost:29092
auto.create.topics.enable=true

# JWT
jwt.secretKey=MySuperSecretKey12345678901234567890
jwt.header=Authorization
```

### 4. Build and Run

```bash
# Build the project
./mvnw clean compile

# Run the application
./mvnw spring-boot:run
```

The service will start on port `3031`.

## API Endpoints

### Driver Location Management

- **PUT** `/api/drivers/{id}/location`
  - Update driver location
  - Body: `DriverLocationRequest`

- **GET** `/api/drivers/{id}/location`
  - Get driver current location
  - Returns: `DriverLocationResponse`

### Driver Status Management

- **PUT** `/api/drivers/{id}/on`
  - Turn driver status ON (available)

- **PUT** `/api/drivers/{id}/off`
  - Turn driver status OFF (unavailable)

### Trip Management

- **POST** `/api/drivers`
  - Accept a trip
  - Body: `AcceptTripRequest`

## Data Models

### Driver
```java
{
    "id": "string",
    "driverId": "string",
    "latitude": "string",
    "longitude": "string", 
    "status": "ON|OFF",
    "detailLocation": "string"
}
```

### Request/Response Objects

- `DriverLocationRequest`: Contains latitude, longitude, and detail location
- `DriverLocationResponse`: Returns driver location information  
- `AcceptTripRequest`: Contains driverId and tripId for trip acceptance

## Event-Driven Architecture

The service publishes and consumes Kafka events:

### Published Events
- `AcceptTripEvent`: When a driver accepts a trip
- `CreateTripEvent`: Trip creation notifications

### Event Listeners
- `CreateTripListener`: Handles incoming trip creation events

## Security

The service implements JWT-based security:
- All endpoints require valid JWT tokens
- Token validation through custom security filters
- Integration with user service for authentication

## CI/CD Pipeline

This project uses GitHub Actions for continuous integration and deployment. The pipeline includes:

### Workflow Features
- **Automated Build**: Triggers on manual workflow dispatch
- **Java 17 Setup**: Uses Temurin distribution
- **Maven Build**: Compiles and packages the application
- **Docker Integration**: Builds and pushes Docker images
- **Docker Hub Deployment**: Automatically publishes to Docker Hub registry

### Workflow Configuration
The CI/CD pipeline is defined in `.github/workflows/ci-cd.yml` and includes:

1. **Code Checkout**: Retrieves the latest code from the repository
2. **Java Setup**: Configures JDK 17 environment
3. **Maven Build**: Executes `mvn -B package` to build the application
4. **Docker Login**: Authenticates with Docker Hub using secrets
5. **Image Build**: Creates Docker image with `latest` tag
6. **Image Push**: Publishes the image to Docker Hub

### Required Secrets
Configure these secrets in your GitHub repository settings:
- `DOCKERHUB_USERNAME`: Your Docker Hub username
- `DOCKERHUB_TOKEN`: Your Docker Hub access token

### Manual Deployment
To trigger a deployment manually:
1. Go to the "Actions" tab in your GitHub repository
2. Select the "build" workflow
3. Click "Run workflow" to start the pipeline

## Testing

Run the test suite:

```bash
./mvnw test
```

Test files are located in `src/test/java/com/example/driver_service/`

## Project Structure

```
src/
├── main/java/com/example/driver_service/
│   ├── client/          # External service clients (Feign)
│   ├── config/          # Configuration classes
│   ├── controller/      # REST controllers
│   ├── DTO/            # Data Transfer Objects
│   ├── ENUM/           # Enumerations
│   ├── event/          # Event classes
│   ├── eventListener/  # Kafka event listeners
│   ├── model/          # Entity models
│   ├── properties/     # Configuration properties
│   ├── repository/     # Data repositories
│   ├── request/        # Request DTOs
│   ├── response/       # Response DTOs
│   └── service/        # Business logic services
└── test/               # Test classes
```

## Development

### Adding New Features

1. Create appropriate DTOs in `request/` or `response/` packages
2. Update the `Driver` model if needed
3. Add business logic to `DriverService`
4. Implement REST endpoints in `DriverController`
5. Add event handling if required
6. Write unit tests
7. The CI/CD pipeline will automatically build and test your changes

### Docker Deployment

The application can be run using Docker:

```bash
# Pull the latest image from Docker Hub
docker pull <dockerhub-username>/driver-service:latest

# Run the container
docker run -p 3031:3031 \
  -e SPRING_DATA_MONGODB_URI=mongodb://admin:admin123@mongodb:27017/driver-db \
  -e SPRING_DATA_REDIS_HOST=redis \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  <dockerhub-username>/driver-service:latest
```

### Code Style

- Use Lombok annotations for reducing boilerplate code
- Follow Spring Boot best practices
- Maintain separation of concerns between layers
- Use meaningful names for classes and methods

## Troubleshooting

### Common Issues

1. **MongoDB Connection Failed**
   - Ensure MongoDB is running on mongodb:27017
   - Check credentials and database name

2. **Redis Connection Issues**
   - Verify Redis server is running on redis:6379
   - Check if Redis is accessible from the application

3. **Kafka Connection Issues**
   - Verify Kafka broker is running on kafka:9092
   - Check if topics are created properly

4. **JWT Authentication Errors**
   - Ensure JWT secret key is properly configured
   - Verify token format and expiration

5. **Docker Build Issues**
   - Ensure Docker is running and accessible
   - Check Docker Hub credentials in GitHub secrets
   - Verify Dockerfile exists in the project root

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is part of the UIT-GO platform. Please refer to the main project license.

## Contact

For questions or support, please contact the development team.