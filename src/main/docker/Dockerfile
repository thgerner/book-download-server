FROM ubuntu:18.04

RUN apt-get update && apt-get install -y openjdk-8-jre && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /var/books/preview/ /opt/bookserver/ebooks/
VOLUME ["/var/books/preview/", "/opt/bookserver/ebooks/"]

COPY maven/BookDownloadServer-0.1.jar /opt/bookserver

EXPOSE 8080

WORKDIR /opt/bookserver
CMD ["/usr/bin/java", "-jar", "-Dspring.profiles.active=docker", "BookDownloadServer-0.1.jar"]
