cd webapp
ant default remove-webapp
cd ..
../bio/scripts/project_build -b -v localhost ~/candidamine-dump
cd webapp
ant default remove-webapp release-webapp
