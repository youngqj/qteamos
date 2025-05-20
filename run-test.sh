#!/bin/bash
cd "$(dirname "$0")"
java --add-opens java.base/java.net=ALL-UNNAMED -jar test-app/target/plugin-tester-1.0.0-jar-with-dependencies.jar 