# 開発用イメージ。
FROM public.ecr.aws/amazonlinux/amazonlinux:2023
RUN dnf install -y java-25-amazon-corretto-headless
RUN dnf install -y tar
RUN dnf install -y gzip

RUN curl -o /tmp/rds-ca-bundle.pem https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem

RUN keytool -importcert -noprompt \
    -alias rds-global-ca \
    -file /tmp/rds-ca-bundle.pem \
    -keystore /usr/lib/jvm/java-25-amazon-corretto/lib/security/cacerts \
    -storepass changeit

RUN rm /tmp/rds-ca-bundle.pem

WORKDIR /app
