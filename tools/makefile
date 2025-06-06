REPOSITORY = cs-repo
PROJECT_ID := $(shell gcloud config get core/project)

.PHONY: debug-image
debug-image:
	docker build -t us-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/maven-debug . -f Dockerfile.debug
	docker push us-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/maven-debug:latest