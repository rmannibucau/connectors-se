#! /bin/sh

GRAALVM_HOME="${GRAALVM_HOME:-$JAVA_HOME}"

mvnDependenciesClasspath="$HOME/.m2/repository/org/apache/johnzon/johnzon-core/1.2.1/johnzon-core-1.2.1.jar:$HOME/.m2/repository/javax/inject/javax.inject/1/javax.inject-1.jar:$HOME/.m2/repository/org/apache/johnzon/johnzon-mapper/1.2.1/johnzon-mapper-1.2.1.jar:$HOME/.m2/repository/org/talend/sdk/component/component-api/1.1.15-SNAPSHOT/component-api-1.1.15-SNAPSHOT.jar:$HOME/.m2/repository/org/apache/geronimo/specs/geronimo-json_1.1_spec/1.3/geronimo-json_1.1_spec-1.3.jar:$HOME/.m2/repository/org/apache/xbean/xbean-asm7-shaded/4.14/xbean-asm7-shaded-4.14.jar:$HOME/.m2/repository/org/apache/geronimo/specs/geronimo-annotation_1.3_spec/1.2/geronimo-annotation_1.3_spec-1.2.jar:$HOME/.m2/repository/org/talend/sdk/component/component-spi/1.1.15-SNAPSHOT/component-spi-1.1.15-SNAPSHOT.jar:$HOME/.m2/repository/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar:$HOME/.m2/repository/org/apache/johnzon/johnzon-jsonb/1.2.1/johnzon-jsonb-1.2.1.jar:$HOME/.m2/repository/org/talend/sdk/component/component-runtime-manager/1.1.15-SNAPSHOT/component-runtime-manager-1.1.15-SNAPSHOT.jar:$HOME/.m2/repository/org/talend/sdk/component/container-core/1.1.15-SNAPSHOT/container-core-1.1.15-SNAPSHOT.jar:$HOME/.m2/repository/org/apache/xbean/xbean-reflect/4.14/xbean-reflect-4.14.jar:$HOME/.m2/repository/org/talend/sdk/component/component-runtime-impl/1.1.15-SNAPSHOT/component-runtime-impl-1.1.15-SNAPSHOT.jar:$HOME/.m2/repository/org/apache/xbean/xbean-finder-shaded/4.14/xbean-finder-shaded-4.14.jar:$HOME/.m2/repository/commons-codec/commons-codec/1.11/commons-codec-1.11.jar:$HOME/.m2/repository/com/zaxxer/HikariCP/3.1.0/HikariCP-3.1.0.jar:$HOME/.m2/repository/org/projectlombok/lombok/1.18.8/lombok-1.18.8.jar:$HOME/.m2/repository/org/apache/geronimo/specs/geronimo-jsonb_1.0_spec/1.2/geronimo-jsonb_1.0_spec-1.2.jar:$HOME/.m2/repository/org/slf4j/slf4j-simple/1.7.28/slf4j-simple-1.7.28.jar"
driverClasspath="$HOME/.m2/repository/com/h2database/h2/1.4.199/h2-1.4.199.jar"
classpath="target/test-classes:target/classes:$mvnDependenciesClasspath:$driverClasspath"
main=org.talend.components.jdbc.graalvm.MainTableNameInputEmitter

echo "Compiling with GraalVM..." &&

native-image $NATIVIFY_OPTS \
    -classpath "$classpath" \
    -H:DynamicProxyConfigurationFiles="$PWD/graal.dynamixproxy.json" \
    -H:ReflectionConfigurationFiles="$PWD/graal.reflection.json" \
    -H:MaxRuntimeCompileMethods=10000 \
    -H:+EnforceMaxRuntimeCompileMethods \
    -H:+AddAllCharsets \
    -H:+ReportExceptionStackTraces \
    -H:+TraceClassInitialization \
    -H:IncludeResourceBundles=org.talend.components.jdbc.service.Messages \
    --no-server \
    --no-fallback \
    --static \
    --allow-incomplete-classpath \
    --report-unsupported-elements-at-runtime \
    --enable-all-security-services \
    --report-unsupported-elements-at-runtime \
    --initialize-at-build-time="org.h2.Driver,org.talend.components.jdbc.service.I18nMessage,org.talend.sdk.component.runtime.manager.reflect.ReflectionService\$Messages" \
    $main \
    target/main-table-name-input-emitter.native &&

echo &&
echo "You can now run: ./target/main-table-name-input-emitter.native" &&
echo
