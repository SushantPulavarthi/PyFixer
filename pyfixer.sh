#!/usr/bin/bash

SCRIPT_DIR="$( cd "$( dirname "$0" )" && pwd )"

# Will build the project if not already built or if the --rebuild flag is passed
if [ ! -d "$SCRIPT_DIR/build/install/PyFixer" ] || [ "$1" == "--rebuild" ]
then
    $SCRIPT_DIR/gradlew installDist
else
    $SCRIPT_DIR/build/install/PyFixer/bin/PyFixer $1
fi
