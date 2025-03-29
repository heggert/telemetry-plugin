#!/bin/bash
set -e

bash gradlew build
docker compose up
