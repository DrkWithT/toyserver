# Toy (HTTP) Server
## By: Derek Tan / DrkWithT (GitHub)

### Summary:
This Java repo contains files and code for a toy _HTTP 1.1_ server. The server is unintended for production usage and nor has full compliance with HTTP/1.1 for now. Although this will be at best a mediocre implementation, I will continue this project for learning about the HTTP protocol and network programming.

### References:
 - Ensure compliance with other HTTP/1.1 requirements based on this link: [HTTP Made Really Easy](https://www.jmarshall.com/easy/http/#http1.1s1)
 - RFC 9110: _HTTP Semantics_

### Project Roadmap:
 1. Create basic worker and HTTP request-response classes. (DONE)
    - Create and integrate a request handler class!
 2. Support persistent / non-persistent connections. (DONE FOR NOW)
 3. Rewrite server resource code. (DONE)
 4. Add HEAD method support. (DONE)
 5. Support `If-Unmodified-Since` header. Honor this whenever recieved with a `200` or `304` based on the header's vs. resource's updating date.
 6. Support `100 Continue` responses.
 7. Support chunked requests.

### Other Todos:
 1. The `SimpleRequest` class needs an overhaul to support chunked requests. It might be better to encapsulate the `responseStream` within another `ChunkedRequest` class for an easy fix, as single vs chunked responses have some different grammar.
