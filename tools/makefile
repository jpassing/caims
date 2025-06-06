REPOSITORY = cs-repo
PROJECT_ID := $(shell gcloud config get core/project)
ZONE = asia-southeast1-a
SERVICE_ACCOUNT_NAME = cs-sa-1

.PHONY: debug-image
debug-image:
	docker build -t us-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/maven-debug . -f Dockerfile.debug
	docker push us-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/maven-debug:latest
    
.PHONY: debug-vm
debug-vm: debug-image
	gcloud compute instances create cs-debug \
		--confidential-compute-type=SEV \
		--shielded-secure-boot \
		--scopes=cloud-platform \
		--zone=$(ZONE) \
		--maintenance-policy=MIGRATE \
		--image-project=confidential-space-images \
		--image-family=confidential-space-debug \
		--service-account=$(SERVICE_ACCOUNT_NAME)@$(PROJECT_ID).iam.gserviceaccount.com \
		--metadata="tee-image-reference=us-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/maven-debug:latest,enable-guest-attributes=TRUE"
        
    echo "Connect to 'cs-debug', then run `sudo ctr t exec --exec-id 1 --tty tee-container /bin/bash` to attach to the container"