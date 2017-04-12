#! /usr/bin/env bash

abspath() {
  # $1 : relative filename
  echo "$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
}

#PROPS=$(abspath `dirname "$0"`/../config/transactor.properties)
pushd ${DATOMIC_HOME:=~/opt/datomic-pro-0.9.5561}

bin/console -p 8081 dev datomic:dev://localhost:4334/
