#!/bin/sh

if [ "$#" -gt 0 ]; then
    EXEC_ARGS_PARAM="-Dexec.args=\"$@\""
else
    EXEC_ARGS_PARAM=""
fi

mvn -e -q compile exec:java ${EXEC_ARGS_PARAM}

if [ "$?" -ne 0 ]; then
    echo "The command failed."
    exit 1 # Exit with a non-zero status to indicate failure
fi
