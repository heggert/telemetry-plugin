#!/bin/bash
set -e

bash gradlew clean shadowJar
docker compose up
