#!/bin/sh
# ******************************************************************************
#
# Pentaho
#
# Copyright (C) 2026 by Pentaho Canada Inc. : http://www.pentaho.com
#
# Use of this software is governed by the Business Source License included
# in the LICENSE.TXT file.
#
# Change Date: 2030-06-15
# ******************************************************************************
BASEDIR=`dirname $0`
cd $BASEDIR

java -Xmx1024m -Djava.net.preferIPv4Stack=false -Dsun.lang.ClassLoader.allowArraySyntax=true -Dhazelcast.serializer.shared=true  $JAVA_OPTS -cp lib/hazelcast-*.jar:lib/* pt.webdetails.cdc.hazelcast.CfgTestApp cdaCache
        
cd -
