REM ******************************************************************************
REM
REM Pentaho
REM
REM Copyright (C) 2026 by Pentaho Canada Inc. : http://www.pentaho.com
REM
REM Use of this software is governed by the Business Source License included
REM in the LICENSE.TXT file.
REM
REM Change Date: 2030-06-15
REM ******************************************************************************
@echo off
java -Xmx1024m -Djava.net.preferIPv4Stack=false -Dsun.lang.ClassLoader.allowArraySyntax=true -Dhazelcast.serializer.shared=true  %JAVA_OPTS% -cp lib\hazelcast-*.jar;lib\* pt.webdetails.cdc.hazelcast.CfgTestApp cdaCache
        