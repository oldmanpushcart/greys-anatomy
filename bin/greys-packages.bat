cd..
call mvn clean package -Dmaven.test.skip=true
cd bin
mkdir ..\greys
copy greys ..\greys
copy ..\target\greys-anatomy-jar-with-dependencies.jar ..\greys\greys.jar
cd ..
@pause
