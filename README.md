# Two Factor Authentication

This project is a Spring Boot application that provides a RESTful API for user registration, authentication, and two-factor authentication.

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