$ErrorActionPreference = 'Stop'

# Removing the volume resets the database on the next run.
docker rm -f -v shop-postgres
if ($LASTEXITCODE -ne 0) {
    throw 'Could not remove shop-postgres.'
}
