FROM openjdk:17-oracle
EXPOSE 8000
ADD target/twofactorauth.jar twofactorauth.jar
ENTRYPOINT ["java","-jar","/twofactorauth.jar"]