#/bin/bash

docker build -t db-unify-be .
docker tag db-unify-be acrdbunify.azurecr.io/db-unify-be:latest
docker push acrdbunify.azurecr.io/db-unify-be:latest
