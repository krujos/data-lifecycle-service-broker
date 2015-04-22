#Catalog
curl -H "X-Broker-Api-Version: 2.4" http://$SECURITY_USER_NAME:$SECURITY_USER_PASSWORD@$BROKER_URI/v2/catalog | jq '.'
