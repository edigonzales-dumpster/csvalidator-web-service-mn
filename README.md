# csvvalidator-web-service-mn

```
mn create-app --features=graalvm --features file-watch ch.so.agi.csvvalidator.csvvalidator
```


```
mn create-controller Main

./gradlew eclipse
./gradlew run --continuous
```


```
./gradlew clean build -x test
docker build . -t sogis/csvvalidator -f Dockerfile.jvm
docker run -p 8080:8080 sogis/csvvalidator:latest
```

