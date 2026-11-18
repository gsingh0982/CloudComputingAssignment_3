#!/usr/bin/env bash
set -e

# Removing the volume resets the database on the next run.
docker rm -f -v shop-postgres
