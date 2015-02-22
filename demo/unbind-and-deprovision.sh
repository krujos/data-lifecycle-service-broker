set -x
#Unbind Copy
curl -X DELETE -i -H "X-Broker-API-Version: 2.4" "http://$BROKER_USERNAME:$BROKER_PASSWORD@localhost:8080/v2/service_instances/copy/service_bindings/copybind?service_id=postgrescdm&plan_id=copy"
#Unbind Prod
curl -X DELETE -i -H "X-Broker-API-Version: 2.4" "http://$BROKER_USERNAME:$BROKER_PASSWORD@localhost:8080/v2/service_instances/prod/service_bindings/prodbind?service_id=postgrescdm&plan_id=prod"
#Deprovision copy
curl -X DELETE -i -H "X-Broker-API-Version: 2.4" "http://$BROKER_USERNAME:$BROKER_PASSWORD@localhost:8080/v2/service_instances/copy?service_id=postgrescdm&plan_id=copy"
#Deprovision prod
curl -X DELETE -i -H "X-Broker-API-Version: 2.4" "http://$BROKER_USERNAME:$BROKER_PASSWORD@localhost:8080/v2/service_instances/prod?service_id=postgrescdm&plan_id=prod"

#Get running instances
curl $BROKER_USERNAME:$BROKER_PASSWORD@localhost:8080/api/instances

#Get bound apps
curl $BROKER_USERNAME:$BROKER_PASSWORD@localhost:8080/api/bindings
