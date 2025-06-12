#
# Copyright 2025 Google LLC
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

REPOSITORY = cs-repo
PROJECT_ID := $(shell gcloud config get core/project)
REGION = asia-southeast1
ZONE = $(REGION)-a
SERVICE_ACCOUNT_NAME = cs-sa-1


#
# DEBUG targets
#
.PHONY: debug-image
debug-image:
	docker build -t us-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/maven-debug . -f workload-debug.dockerfile
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
		
	echo "Connect to 'cs-debug', then run 'sudo ctr t exec --exec-id 1 --tty tee-container /bin/bash' to attach to the container"
	
	
.PHONY: debug-vm-registration
debug-vm-registration:
	gcloud compute instances get-guest-attributes $(VM_NAME) \
		--query-path=workload-server/token \
		--zone=$(ZONE)
 

#
# Workload targets
#
.PHONY: workload-image
workload-image:
	docker build -t us-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/workload . -f workload.dockerfile
	docker push us-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/workload:latest
	
.PHONY: workload-vm
workload-vm: workload-image
	gcloud compute instances create workload \
		--confidential-compute-type=SEV \
		--shielded-secure-boot \
		--scopes=cloud-platform \
		--zone=$(ZONE) \
		--maintenance-policy=MIGRATE \
		--image-project=confidential-space-images \
		--image-family=confidential-space \
		--service-account=$(SERVICE_ACCOUNT_NAME)@$(PROJECT_ID).iam.gserviceaccount.com \
		--metadata="tee-image-reference=us-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/workload:latest,enable-guest-attributes=TRUE"
		
 
#
# Broker targets
#
.PHONY: broker-image
broker-image:
	docker build -t us-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/broker . -f broker.dockerfile
	docker push us-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/broker:latest
    
.PHONY: broker
broker: broker-image
	gcloud run deploy broker \
		--image=us-docker.pkg.dev/$(PROJECT_ID)/$(REPOSITORY)/broker:latest \
		--allow-unauthenticated \
		--region $(REGION) \
		--network default \
		--service-account=$(SERVICE_ACCOUNT_NAME)@$(PROJECT_ID).iam.gserviceaccount.com