```shell
curl -v -d '{"value": "Foo"}' \
    -H'Content-type: application/json' \
    -H'ce-id: 1' \
    -H'ce-source: spring-wasm-function-sample' \
    -H'ce-type: com.example.driver.Foo' \
    -H'ce-specversion: 1.0' \
    http://localhost:8080/decorateEventWithWasm
```
