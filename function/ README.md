### DemoApplication via cURL

The dmeo can be exercised by executing the following `cURL` command:
```shell
curl -v -d '{"value": "bar5150"}' \
    -H 'Content-type: application/json' \
    -H 'ce-id: 1' \
    -H 'ce-source: spring-wasm-function-sample' \
    -H 'ce-type: com.example.driver.Foo' \
    -H 'ce-specversion: 1.0' \
    http://localhost:8080/decorateEventWithWasm
```
The output should look as follows:
```shell
* Connected to localhost (127.0.0.1) port 8080 (#0)
> POST /decorateEventWithWasm HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.79.1
> Accept: */*
> Content-type: application/json
> ce-id: 1
> ce-source: spring-wasm-function-sample
> ce-type: com.example.driver.Foo
> ce-specversion: 1.0
> Content-Length: 20
>
* Mark bundle as not supporting multiuse
< HTTP/1.1 200
< user-agent: curl/7.79.1
< ce-id: 1-5150
< ce-source: spring-wasm-function-sample
< ce-type: com.example.driver.Foo
< ce-specversion: 1.0
< uri: /decorateEventWithWasm
< ce-datacontenttype: application/json
< accept: */*
< ce-decoratedby: function
< message-type: cloudevent
< host: localhost:8080
< contenttype: application/json
< timestamp: 1662404391956
< Content-Type: application/json
< Content-Length: 20
< Date: Mon, 05 Sep 2022 18:59:52 GMT
<
* Connection #0 to host localhost left intact
{"value": "bar5150"}%
```

**NOTE**: The outgoing event was decorated by the WASM w/ `ce-id: 1-5150` and by the function with `ce-decoratedby: function`. 
