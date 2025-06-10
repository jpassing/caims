FROM maven:3.9.6-eclipse-temurin-17
#ENTRYPOINT ["/caims/sources/run.sh", "workload"]

EXPOSE 8080
ENTRYPOINT ["/usr/bin/sleep", "1d"]

RUN git clone https://github.com/jpassing/caims.git
