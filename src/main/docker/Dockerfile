FROM ubuntu:22.04

RUN apt-get update && apt-get install -y openjdk-17-jre && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /var/books/preview/ /opt/bookserver/ebooks/
VOLUME ["/var/books/preview/", "/opt/bookserver/ebooks/"]

COPY maven/BookDownloadServer-0.5.jar /opt/bookserver/BookDownloadServer.jar

EXPOSE 8080

WORKDIR /opt/bookserver
CMD ["/usr/bin/java", "-jar", "-Dspring.profiles.active=docker", "BookDownloadServer.jar"]
