# Sourced by the deploy scripts. Everything that differs per app lives here.
ARTIFACT=app-starter-0.0.1-SNAPSHOT.jar

STAGE_JAR=app-stage.jar
PROD_JAR=app-prod.jar

STAGE_SERVICE=app-demo.service
PROD_SERVICE=app.service

# The system java is usually older than the app needs; point at the JDK that builds it.
JDK_HOME=${JDK_HOME:-/usr/lib/jvm/jdk-25}
MVN=${MVN:-mvn}
