FROM maven:3.9.6-eclipse-temurin-17

EXPOSE 8080
ENTRYPOINT ["/usr/bin/sleep", "1d"]

RUN git clone https://github.com/jpassing/caims.git
