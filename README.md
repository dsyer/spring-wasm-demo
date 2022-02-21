Spring Cloud Gateway with a custom predicate implemented as a WASM.

Run the app and then send a request (e.g. with curl) to
`localhost:8080/github/`. If there is no header with key "one" the
predicate does not match and you get a 404. If there is a header
called "one" then the request is routed to github.