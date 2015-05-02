#!/usr/bin/env bash

PLATFORM='unknown'
UNAMESTR=`uname`
if [[ "$UNAMESTR" == 'Darwin' ]]; then
  platform='Mac'
fi

CF_USER=cloudbeesci
CF_ORG=brokers
CF_SPACE=lifecycle
CF_DOMAIN=apps.pcf.jkruckcloud.com
CF_API_ENDPOINT=api.run.pcf.jkruckcloud.com
APP_PREFIX=cdm-service-broker
APP_VERSIONED=$APP_PREFIX-$BUILD_VERSION

if [[ $platform == 'Mac' ]]; then
  curl -L "https://cli.run.pivotal.io/stable?release=macosx64&source=pws" -o "installer-osx-amd64.pkg"
  mkdir -p cf-pkg
  cd cf-pkg
  xar -xf ../installer-osx-amd64.pkg
  cd com.cloudfoundry.cli.pkg
  cat Payload | gunzip -dc | cpio -i
  mv usr/local/bin/cf ../..
  cd ../..
else
  wget http://go-cli.s3-website-us-east-1.amazonaws.com/releases/latest/cf-linux-amd64.tgz
  tar -zxvf cf-linux-amd64.tgz
fi
./cf --version
#./cf login -a https://${CF_API_ENDPOINT} -u ${CF_USER} -p ${CF_PASSWORD} -o ${CF_ORG} -s ${CF_SPACE}
#Use this version instead of the above if you need to disable SSL validation
./cf login -a https://${CF_API_ENDPOINT} -u ${CF_USER} -p ${CF_PASSWORD} -o ${CF_ORG} -s ${CF_SPACE} --skip-ssl-validation

DEPLOYED_VERSION_CMD=$(CF_COLOR=false ./cf apps | grep ${APP_PREFIX} | cut -d" " -f1)
DEPLOYED_VERSION="$DEPLOYED_VERSION_CMD"
ROUTE_VERSION=$(echo "${BUILD_VERSION}" | cut -d"." -f1-3 | tr '.' '-')
echo "Deployed Version: $DEPLOYED_VERSION"
echo "Route Version: $ROUTE_VERSION"

./cf push "$APP_VERSIONED" -n "$APP_VERSIONED" -d ${CF_DOMAIN} -p artifacts/$APP_VERSIONED.jar -f manifest.yml --no-start

# Extract the set-env-cf.sh script from the jar and run it.
unzip -o artifacts/$APP_VERSIONED.jar "set-cf-env.sh" "manifest.yml"
chmod +x set-cf-env.sh
export APPNAME=$APP_VERSIONED
PATH=$PATH:.
./set-cf-env.sh

./cf start "$APP_VERSIONED"
./cf map-route "$APP_VERSIONED" ${CF_DOMAIN} -n $APP_PREFIX
if [ ! -z "$DEPLOYED_VERSION" -a "$DEPLOYED_VERSION" != " " -a "$DEPLOYED_VERSION" != "$APP_VERSIONED" ]; then
  echo "Performing zero-downtime cutover to $BUILD_VERSION"
  while read line
  do
    if [[ ! -z "$line" && "$line" != " " && "$line" != "${APP_VERSIONED}" && "$line" == "${APP_PREFIX}-"* ]]; then
      echo "Scaling down, unmapping and removing $line"
      ./cf scale "$line" -i 1
      ./cf unmap-route "$line" ${CF_DOMAIN} -n $APP_PREFIX
      ./cf delete "$line" -f -r
    else
      echo "Skipping $line"
    fi
  done <<EOF
< "$DEPLOYED_VERSION"
fi
EOF