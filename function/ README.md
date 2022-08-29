

### DemoApplication via cURL

```shell
curl -v -X POST http://localhost:8080/ \
    -H 'Content-type: application/json' \
    -H 'one: two' \
    -d '{"name": "hello foo"}'
```
