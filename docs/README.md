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
 3. Rewrite server resource code.
 4. Add HEAD method support.
 5. Support `100 Continue` responses.
