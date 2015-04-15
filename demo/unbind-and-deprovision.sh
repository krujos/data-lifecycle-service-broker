set -x
#Unbind Copy
curl -X DELETE -i -H "X-Broker-API-Version: 2.5" "http://$SECURITY_USER_NAME:$SECURITY_USER_PASSWORD@$BROKER_URI/v2/service_instances/copy/service_bindings/copybind?service_id=postgrescdm&plan_id=copy"
#Unbind Prod
curl -X DELETE -i -H "X-Broker-API-Version: 2.5" "http://$SECURITY_USER_NAME:$SECURITY_USER_PASSWORD@$BROKER_URI/v2/service_instances/prod/service_bindings/prodbind?service_id=postgrescdm&plan_id=prod"
#Deprovision copy
curl -X DELETE -i -H "X-Broker-API-Version: 2.5" "http://$SECURITY_USER_NAME:$SECURITY_USER_PASSWORD@$BROKER_URI/v2/service_instances/copy?service_id=postgrescdm&plan_id=copy"
#Deprovision prod
curl -X DELETE -i -H "X-Broker-API-Version: 2.5" "http://$SECURITY_USER_NAME:$SECURITY_USER_PASSWORD@$BROKER_URI/v2/service_instances/prod?service_id=postgrescdm&plan_id=prod"

#Get running instances
curl $SECURITY_USER_NAME:$SECURITY_USER_PASSWORD@$BROKER_URI/api/instances

#Get bound apps
curl $SECURITY_USER_NAME:$SECURITY_USER_PASSWORD@$BROKER_URI/api/bindings
