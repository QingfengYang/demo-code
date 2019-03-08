#!/usr/bin/env bash

param_date=$1
echo $param_date
su - hadoopuser -c "bash /www/uprofile-etl-project/wifi/shell/wifi-recognize.sh $param_date"
