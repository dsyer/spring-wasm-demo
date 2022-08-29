

### DemoApplication via cURL

```shell
curl -v http://localhost:8080/ \
    -H 'Content-type: application/json' \
    -H 'one: two' \
    -d '{"payload": "hello foo"}'
```
