./package.sh
mkdir -p ../target/ga
cp ga ../target/ga
cp ../target/greys-anatomy-jar-with-dependencies.jar ../target/ga/ga.jar
cd ../target/
zip -r ga.zip ga/
cd -

