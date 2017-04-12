#! /usr/bin/env bash

pushd ${DATOMIC_HOME:=~/opt/datomic-pro-0.9.5561}
bin/datomic -Xmx4g -Xms4g backup-db "datomic:dev://localhost:4334/firstdb" "file:/home/user/tmp/datomic_backups"
