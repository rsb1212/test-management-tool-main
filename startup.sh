#!/bin/bash

# Start PostgreSQL
service postgresql start

# Start Redis
service redis start

# Build the application
make build

# Run the application
./run_app --host localhost --port 8080
