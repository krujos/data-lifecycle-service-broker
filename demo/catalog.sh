#Catalog
set -x
curl -H "X-Broker-Api-Version: 2.4" http://$BROKER_USERNAME:$BROKER_PASSWORD@localhost:8080/v2/catalog | jq '.'
