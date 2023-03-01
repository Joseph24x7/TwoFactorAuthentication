# Two Factor Authentication

This project is a Spring Boot application that provides a RESTful API for user registration, authentication, and two-factor authentication.

This code defines a system design for an authentication service that provides the following functionality:

User registration: creates a new user account, generates a unique username, sends a one-time password (OTP) to the user's email, generates a JSON web token (JWT) for the user's session, and returns an authentication response with user info, JWT, and success status.

User authentication: validates the user's credentials, generates and stores a new two-factor authentication code, sends the OTP to the user's email, generates a new JWT for the user's session, and returns an authentication response with user info, JWT, and success status.

Two-factor authentication verification: asynchronously authenticates the user's two-factor authentication code, sends a success notification with the AuthResponse and the completed CompletableFuture, and returns an authentication response with user info, JWT, and success status.

The system design leverages Spring Framework, RabbitMQ, Kafka, and Java validation APIs. The design also uses a TokenUtil class for token generation, a UserRepository for database interaction, and a Random class for generating random numbers. The system design is organized as a set of services, each with its specific functionality. Finally, the system design incorporates asynchronous programming constructs such as CompletableFuture to provide better performance and scalability.

## Features

- User registration
- User authentication
- Two-factor authentication
- RabbitMQ for sending OTP
- Kafka for user data publishing

## Technology Used

- Spring Boot
- Spring Security
- RabbitMQ
- Kafka
- Postfix
- PostgreSQL

## Getting Started

### Prerequisites
- Java 17
- RabbitMQ
- Kafka
- Postfix
- PostgreSQL

### Installation

1. Clone the repository:
git clone https://github.com/username/two-factor-authentication-service.git

2. Build the project:
mvn clean install

3. Run the project:
mvn spring-boot:run

4. Usage: http://host:port/swagger-ui/index.html#/
