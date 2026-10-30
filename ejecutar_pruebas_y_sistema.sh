#!/usr/bin/env bash
set -e
echo "========================================================================="
echo " SISTEMA DE GESTION COMERCIAL - MINIMARKET DON PEPE E.I.R.L. (JAVA 17)"
echo " DevTeam - Minimarket Don Pepe"
echo "========================================================================="
mkdir -p target/classes target/test-classes
java -m jdk.compiler/com.sun.tools.javac.Main -cp "lib/commons-lang3-3.12.0.jar:lib/enterprise-support-libs.jar" -d target/classes $(find src/main/java -name "*.java")
java -m jdk.compiler/com.sun.tools.javac.Main -cp "lib/commons-lang3-3.12.0.jar:lib/enterprise-support-libs.jar:target/classes" -d target/test-classes $(find src/test/java -name "*.java")
java -cp "lib/commons-lang3-3.12.0.jar:lib/enterprise-support-libs.jar:target/classes:target/test-classes" pe.com.donpepe.minimarket.MinimarketTddTestSuite
java -cp "lib/commons-lang3-3.12.0.jar:lib/enterprise-support-libs.jar:target/classes" pe.com.donpepe.minimarket.MainApplication
