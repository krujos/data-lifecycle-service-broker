set -x 
#Provision Copy
curl -i -H "X-Broker-API-Version: 2.5" -H "Content-Type: application/json" http://$SECURITY_USER_NAME:$SECURITY_USER_PASSWORD@$BROKER_URI/v2/service_instances/copy -d '{"service_id": "postgrescdm", "plan_id": "copy", "organization_guid": "org_guid", "space_guid": "s_guid"}' -X PUT
#Provision Prod
curl -i -H "X-Broker-API-Version: 2.5" -H "Content-Type: application/json" http://$SECURITY_USER_NAME:$SECURITY_USER_PASSWORD@$BROKER_URI/v2/service_instances/prod -d '{"service_id": "postgrescdm", "plan_id": "prod", "organization_guid": "org_guid", "space_guid": "s_guid"}' -X PUT

sleep 10;
#Bind Copy
curl -i -H "X-Broker-API-Version: 2.5" -H "Content-Type: application/json" http://$SECURITY_USER_NAME:$SECURITY_USER_PASSWORD@$BROKER_URI/v2/service_instances/copy/service_bindings/copybind -d '{ "plan_id": "copy", "service_id": "postgrescdm", "app_guid": "app-guid-here" }' -X PUT
#Bind Prod
curl -i -H "X-Broker-API-Version: 2.5" -H "Content-Type: application/json" http://$SECURITY_USER_NAME:$SECURITY_USER_PASSWORD@$BROKER_URI/v2/service_instances/prod/service_bindings/prodbind -d '{ "plan_id": "prod", "service_id": "postgrescdm", "app_guid": "app-guid-here" }' -X PUT

#Get running instances
curl $SECURITY_USER_NAME:$SECURITY_USER_PASSWORD@$BROKER_URI/api/instances | jq '.'

#Get bound apps
curl $SECURITY_USER_NAME:$SECURITY_USER_PASSWORD@$BROKER_URI/api/bindings | jq '.'
