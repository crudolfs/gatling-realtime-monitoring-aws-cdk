#!/bin/sh

help_text() {
    cat <<EOF
    Usage: $0 [ -gh | --graphitehost GATLING_GRAPHITE_HOST ] [ -gp | --graphiteport GATLING_GRAPHITE_PORT ] [--help]
        --graphitehost GATLING_GRAPHITE_HOST         (optional) The host where the Graphite service is located.
        --graphiteport GATLING_GRAPHITE_PORT         (optional) The port to which the Graphite service listens to.
EOF
    exit 1
}

GATLING_GRAPHITE_HOST="localhost"
GATLING_GRAPHITE_PORT=2003

while [ $# -gt 0 ]; do
    arg=$1
    case $arg in
        --help)
            help_text
        ;;
        -gh|--graphitehost)
            export GATLING_GRAPHITE_HOST="$2"
            shift; shift
        ;;
        -gp|--graphiteport)
            export GATLING_GRAPHITE_PORT="$2"
            shift; shift
        ;;
        *)
            echo "ERROR: Unrecognised option: ${arg}"
            help_text
            exit 1
        ;;
    esac
done

echo GATLING_GRAPHITE_HOST=${GATLING_GRAPHITE_HOST}
echo GATLING_GRAPHITE_PORT=${GATLING_GRAPHITE_PORT}

java -DGATLING_GRAPHITE_HOST=${GATLING_GRAPHITE_HOST} -DGATLING_GRAPHITE_PORT=${GATLING_GRAPHITE_PORT} -jar gatling-runner.jar -s simulations.BasicSimulation
