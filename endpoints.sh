#curl -H "X-Broker-Api-Version: 2.4" http://user:$BROKER_PASSWORD@localhost:8080/v2/catalog 
curl -H "X-Broker-API-Version: 2.4" -H "Content-Type: application/json" http://user:$BROKER_PASSWORD@localhost:8080/v2/service_instances/foo -d '{"service_id": "postgrescdm", "plan_id": "copy", "organization_guid": "org_guid", "space_guid": "s_guid"}' -X PUT
curl -H "X-Broker-API-Version: 2.4" -H "Content-Type: application/json" http://user:$BROKER_PASSWORD@localhost:8080/v2/service_instances/foo/service_bindings/foobind -d '{ "plan_id": "copy", "service_id": "postgrescdm", "app_guid": "app-guid-here" }' -X PUT -i
curl -H "X-Broker-API-Version: 2.4" http://username:$BROKER_PASSWORD@localhost:8080/v2/service_instances/foo/service_bindings/foobind?service_id=postgrescdm&plan_id=copy -X DELETE
