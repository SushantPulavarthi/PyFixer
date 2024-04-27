#!/usr/bin/bash

# Will build the project if not already built or if the --rebuild flag is passed
if [ ! -d "./build/install/PyFixer" ] || [ "$1" == "--rebuild" ]
then
    ./gradlew installDist
    ./build/install/PyFixer/bin/PyFixer $2
else
    ./build/install/PyFixer/bin/PyFixer $1
fi
