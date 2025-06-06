#!/bin/sh

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 project-id"
  exit 1 # Exit with a non-zero status to indicate an error
fi

PROJECT_ID=$1

cat << EOF > Dockerfile
FROM maven:3.9.6-eclipse-temurin-17
ENTRYPOINT ["/caims/sources/run.sh", "workload"]

RUN git clone https://github.com/jpassing/caims.git
EOF

docker build -t us-docker.pkg.dev/$PROJECT_ID/cs-repo/maven-debug .
docker push us-docker.pkg.dev/$PROJECT_ID/cs-repo/maven-debug:latest-debug:latest