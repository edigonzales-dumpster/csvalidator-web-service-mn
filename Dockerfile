FROM oracle/graalvm-ce:20.1.0-java8 as graalvm
RUN gu install native-image

COPY . /home/app/csvvalidator
WORKDIR /home/app/csvvalidator

RUN native-image --no-server -cp build/libs/csvvalidator-*-all.jar

FROM frolvlad/alpine-glibc
RUN apk update && apk add libstdc++
EXPOSE 8080
COPY --from=graalvm /home/app/csvvalidator/csvvalidator /app/csvvalidator
ENTRYPOINT ["/app/csvvalidator"]
