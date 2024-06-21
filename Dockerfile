FROM eclipse-temurin:21 AS build

ARG SL_VERSION
ARG SPARK_VERSION
ARG HADOOP_VERSION

## GCP
ARG ENABLE_BIGQUERY
ARG SPARK_BQ_VERSION

## AZURE
ARG ENABLE_AZURE
ARG HADOOP_AZURE_VERSION
ARG AZURE_STORAGE_VERSION
ARG JETTY_VERSION
ARG JETTY_UTIL_VERSION
ARG JETTY_UTIL_AJAX_VERSION

## SNOWFLAKE
ARG ENABLE_SNOWFLAKE
ARG SPARK_SNOWFLAKE_VERSION
ARG SNOWFLAKE_JDBC_VERSION

## POSTGRES
ARG ENABLE_POSTGRESQL
ARG POSTGRESQL_VERSION

## REDSHIFT
ARG ENABLE_REDSHIFT
ARG AWS_JAVA_SDK_VERSION
ARG HADOOP_AWS_VERSION
ARG REDSHIFT_JDBC_VERSION
ARG SPARK_REDSHIFT_VERSION

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
      findutils \
      jq \
      graphviz \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY starlake.s[h] /app/

RUN if [ ! -f starlake.sh ]; then curl -O https://raw.githubusercontent.com/starlake-ai/starlake/master/distrib/starlake.sh; fi && chmod +x starlake.sh

RUN   SL_VERSION="$SL_VERSION" \
      SPARK_VERSION="$SPARK_VERSION" \
      HADOOP_VERSION="$HADOOP_VERSION" \
      ENABLE_BIGQUERY=$ENABLE_BIGQUERY \
      SPARK_BQ_VERSION="$SPARK_BQ_VERSION" \
      ENABLE_AZURE=$ENABLE_AZURE \
      HADOOP_AZURE_VERSION="$HADOOP_AZURE_VERSION" \
      AZURE_STORAGE_VERSION="$AZURE_STORAGE_VERSION" \
      JETTY_VERSION="$JETTY_VERSION" \
      JETTY_UTIL_VERSION="$JETTY_UTIL_VERSION" \
      JETTY_UTIL_AJAX_VERSION="$JETTY_UTIL_AJAX_VERSION" \
      ENABLE_SNOWFLAKE=$ENABLE_SNOWFLAKE \
      SPARK_SNOWFLAKE_VERSION="$SPARK_SNOWFLAKE_VERSION" \
      SNOWFLAKE_JDBC_VERSION="$SNOWFLAKE_JDBC_VERSION" \
      ENABLE_POSTGRESQL="$ENABLE_POSTGRESQL" \
      POSTGRESQL_VERSION="$POSTGRESQL_VERSION" \
      ENABLE_REDSHIFT="$ENABLE_REDSHIFT" \
      AWS_JAVA_SDK_VERSION="$AWS_JAVA_SDK_VERSION" \
      HADOOP_AWS_VERSION="$HADOOP_AWS_VERSION" \
      REDSHIFT_JDBC_VERSION="$REDSHIFT_JDBC_VERSION" \
      SPARK_REDSHIFT_VERSION="$SPARK_REDSHIFT_VERSION" \
      ENABLE_ALL=true \
      ./starlake.sh install

FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app /app
RUN apk add --no-cache procps gcompat bash
ENTRYPOINT ["/app/starlake.sh"]
