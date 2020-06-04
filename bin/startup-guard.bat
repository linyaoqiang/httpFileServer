cd /d %~dp0
cd ..
start javaw -cp  httpFileServer-1.1.jar com.study.httpFileServer.ConsoleServer conf/server.properties conf/server-log.properties