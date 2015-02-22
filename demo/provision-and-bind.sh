set -x 
#Provision Copy
curl -i -H "X-Broker-API-Version: 2.4" -H "Content-Type: application/json" http://$BROKER_USERNAME:$BROKER_PASSWORD@localhost:8080/v2/service_instances/copy -d '{"service_id": "postgrescdm", "plan_id": "copy", "organization_guid": "org_guid", "space_guid": "s_guid"}' -X PUT
#Provision Prod
curl -i -H "X-Broker-API-Version: 2.4" -H "Content-Type: application/json" http://$BROKER_USERNAME:$BROKER_PASSWORD@localhost:8080/v2/service_instances/prod -d '{"service_id": "postgrescdm", "plan_id": "prod", "organization_guid": "org_guid", "space_guid": "s_guid"}' -X PUT
#Bind Copy
curl -i -H "X-Broker-API-Version: 2.4" -H "Content-Type: application/json" http://$BROKER_USERNAME:$BROKER_PASSWORD@localhost:8080/v2/service_instances/copy/service_bindings/copybind -d '{ "plan_id": "copy", "service_id": "postgrescdm", "app_guid": "app-guid-here" }' -X PUT
#Bind Prod
curl -i -H "X-Broker-API-Version: 2.4" -H "Content-Type: application/json" http://$BROKER_USERNAME:$BROKER_PASSWORD@localhost:8080/v2/service_instances/prod/service_bindings/prodbind -d '{ "plan_id": "prod", "service_id": "postgrescdm", "app_guid": "app-guid-here" }' -X PUT

#Get running instances
curl $BROKER_USERNAME:$BROKER_PASSWORD@localhost:8080/api/instances | jq '.'

#Get bound apps
curl $BROKER_USERNAME:$BROKER_PASSWORD@localhost:8080/api/bindings | jq '.'
