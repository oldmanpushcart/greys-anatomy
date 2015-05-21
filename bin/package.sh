#!/bin/sh
mvn clean package -Dmaven.test.skip=true -f ../pom.xml
