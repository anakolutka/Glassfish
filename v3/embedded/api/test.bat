set XYZ=-cp target/test-classes;target/gf-embedded-api-1.0-alpha-5-SNAPSHOT-with-full-v3.jar  org.glassfish.embed.Main

if "%1"=="debug" goto debug

java %XYZ%
goto end


:debug
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1323 %XYZ%

:end

