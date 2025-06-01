FROM maven:3.8-openjdk-11 as build

WORKDIR /app

# Copy pom.xml first for better caching
COPY pom.xml .
# Skip the dependency:go-offline step which is causing issues
# RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn package -DskipTests

# Create the runtime image
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Environment variables
ENV PORT=8080
ENV CONTENTFUL_SPACE_ID=your_space_id
ENV CONTENTFUL_ACCESS_TOKEN=your_access_token
ENV CONTENTFUL_ENVIRONMENT=master
ENV JWT_SECRET=your_jwt_secret

EXPOSE ${PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]
