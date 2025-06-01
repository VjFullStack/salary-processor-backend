#!/bin/bash

# Navigate to the project root
cd "$(dirname "$0")"

# Create target directory if it doesn't exist
mkdir -p target/classes

# Compile the Java files
javac -cp "target/classes:${HOME}/.m2/repository/org/apache/poi/poi/5.2.3/poi-5.2.3.jar:${HOME}/.m2/repository/org/apache/poi/poi-ooxml/5.2.3/poi-ooxml-5.2.3.jar:${HOME}/.m2/repository/org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar:${HOME}/.m2/repository/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar:${HOME}/.m2/repository/org/apache/commons/commons-compress/1.21/commons-compress-1.21.jar:${HOME}/.m2/repository/commons-codec/commons-codec/1.15/commons-codec-1.15.jar:${HOME}/.m2/repository/org/apache/logging/log4j/log4j-api/2.18.0/log4j-api-2.18.0.jar" -d target/classes src/main/java/com/salaryprocessor/util/ExcelAnalyzer.java

# Run the analyzer
java -cp "target/classes:${HOME}/.m2/repository/org/apache/poi/poi/5.2.3/poi-5.2.3.jar:${HOME}/.m2/repository/org/apache/poi/poi-ooxml/5.2.3/poi-ooxml-5.2.3.jar:${HOME}/.m2/repository/org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar:${HOME}/.m2/repository/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar:${HOME}/.m2/repository/org/apache/commons/commons-compress/1.21/commons-compress-1.21.jar:${HOME}/.m2/repository/commons-codec/commons-codec/1.15/commons-codec-1.15.jar:${HOME}/.m2/repository/org/apache/logging/log4j/log4j-api/2.18.0/log4j-api-2.18.0.jar" com.salaryprocessor.util.ExcelAnalyzer
