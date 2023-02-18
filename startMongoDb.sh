#!/bin/sh
docker run --name mongodb -d \
-p 27017:27017 \
-e MONGO_INITDB_ROOT_USERNAME=jim \
-e MONGO_INITDB_ROOT_PASSWORD=thedoors123 \
mongodb/mongodb-community-server:6.0.4-ubi8