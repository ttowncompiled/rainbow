#!/bin/bash
if [ "$1" == "" ]; then
	echo "An argument is required"
	exit 1
elif [ ! -f $1 ]; then
	echo "$1 does not exist"
	exit 1
fi

grep "target[-_]loc" $1