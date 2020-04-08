#!/bin/bash
set -x -e

#Run only on master
if grep isMaster /mnt/var/lib/info/instance.json | grep true;
then
	sudo usermod -aG hadoop,zeppelin zeppelin
fi