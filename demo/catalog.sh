#Catalog
set -x
curl -H "X-Broker-Api-Version: 2.5" http://$SECURITY_USER_NAME:$SECURITY_USER_PASSWORD@$BROKER_URI/v2/catalog | jq '.'
