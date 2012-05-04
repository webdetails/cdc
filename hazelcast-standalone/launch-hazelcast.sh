#!/bin/sh
java -Xmx1024m -Djava.net.preferIPv4Stack=true -Dsun.lang.ClassLoader.allowArraySyntax=true -Dhazelcast.serializer.shared=true  $JAVA_OPTS -cp lib/hazelcast-*.jar;lib/* com.hazelcast.examples.StartServer
