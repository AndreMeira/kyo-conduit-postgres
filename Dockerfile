FROM amazoncorretto:21.0.5-alpine

RUN apk add --no-cache bash

ENV GROUP_ID=10000
ENV USER_ID=10000

RUN addgroup --gid $GROUP_ID group && \
    adduser --uid $USER_ID --disabled-password --no-create-home --ingroup group --shell /sbin/nologin user

ADD --chown=$GROUP_ID:$USER_ID ./target/universal/stage /app

WORKDIR /app
USER $USER_ID

EXPOSE 8080

ENTRYPOINT ["/app/bin/conduit-kyo-postgres"]
