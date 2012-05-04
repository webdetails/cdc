#!/bin/sh
while java -Xmx1024m -Xms256m -Djava.net.preferIPv4Stack=true -Dsun.lang.ClassLoader.allowArraySyntax=true -Dhazelcast.serializer.shared=true  $JAVA_OPTS -cp lib/hazelcast-*.jar:lib/* pt.webdetails.cdc.hazelcast.StartServer
do
echo "## Restarting... ##"
done
