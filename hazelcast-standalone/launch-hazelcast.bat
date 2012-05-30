@echo off
:start
java -Xmx1024m -Djava.net.preferIPv4Stack=false -Dsun.lang.ClassLoader.allowArraySyntax=true -Dhazelcast.serializer.shared=true  %JAVA_OPTS% -cp lib\hazelcast-*.jar;lib\* pt.webdetails.cdc.hazelcast.StartServer
if ERRORLEVEL 1 goto restart
goto exit
:restart
echo "#### Restarting... ####"
goto start
:exit