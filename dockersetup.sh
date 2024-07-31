#!/bin/bash

# Check if environment variables are set
if [ -z "$ASTRA_DB_USERNAME" ]; then
  echo "Error: ASTRA_DB_USERNAME is not set."
  exit 1
fi

if [ -z "$ASTRA_DB_KEYSPACE" ]; then
  echo "Error: ASTRA_DB_KEYSPACE is not set."
  exit 1
fi

if [ -z "$ASTRA_DB_ID" ]; then
  echo "Error: ASTRA_DB_ID is not set."
  exit 1
fi

if [ -z "$DATASTAX_ASTRA_PASSWORD" ]; then
  echo "Error: DATASTAX_ASTRA_PASSWORD is not set."
  exit 1
fi

# Send POST request to obtain the download link for SCB
RESPONSE=$(curl -s --location --request POST https://api.astra.datastax.com/v2/databases/$ASTRA_DB_ID/secureBundleURL \
    --header "Content-Type: application/json" \
    --header "Authorization: Bearer $DATASTAX_ASTRA_PASSWORD")

echo $RESPONSE
# Extract download URL from response
DOWNLOAD_URL=$(echo $RESPONSE | jq -r '.downloadURL')

if [ "$DOWNLOAD_URL" == "null" ]; then
  echo "Error: Unable to obtain download URL."
  echo "Response: $RESPONSE"
  exit 1
fi

# Download Secure Connect Bundle (SCB) using the obtained URL
SCB_DEST="/app/src/main/resources/secure-connect-database.zip"
curl -L -o $SCB_DEST $DOWNLOAD_URL

SCB_RN="/app/src/main/resources/secure-connect-cass5-stac.zip"

if [ $? -ne 0 ]; then
  echo "Error downloading SCB."
  exit 1
fi

mv $SCB_DEST $SCB_RN

echo "SCB downloaded and saved to $SCB_RN"

