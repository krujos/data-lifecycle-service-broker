#!/bin/sh 
cf service lifecycle-sb-db 2>&1 > /dev/null
if [ $? -ne 0 ] ; then 
	echo "INFO: Creating database for broker"
	cf create-service p-mysql 100mb-dev lifecycle-sb-db
else
	echo "INFO: Database for broker already exists.... skipping"
fi

echo "INFO: Pushing broker"
cf push --no-start
#This assumes your credentials are in the same file as mine.
#see set-cf-env.sh for what the expectations are on the content
#of this file, or README.md
. ../creds/aws-cdm-creds

echo "INFO: Setting up broker environment variables"
./set-cf-env.sh

echo "INFO: Restaging app"
cf start lifecycle-sb

echo "INFO: Creating broker"
cf create-service-broker lifecycle-sb $SECURITY_USER_NAME $SECURITY_USER_PASSWORD http://lifecycle-sb.apps.pcf.jkruckcloud.com
cf enable-service-access lifecycle-sb