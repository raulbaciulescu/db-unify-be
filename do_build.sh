#/bin/bash

docker build -t db-unify-be .
docker tag db-unify-be acrdbunify.azurecr.io/db-unify-be:latest
az acr login --name acrdbunify
docker push acrdbunify.azurecr.io/db-unify-be:latest
kubectl apply -f deployment.yaml
