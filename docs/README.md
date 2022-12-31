# Toy (HTTP) Server
## By: Derek Tan / DrkWithT (GitHub)

### Summary:
This Java repo contains files and code for a toy _HTTP 1.1_ server. The server is unintended for production usage and nor has full compliance with HTTP/1.1 for now. Although this will be at best a mediocre implementation, I will continue this project for learning about the HTTP protocol and network programming.

### Project Roadmap:
 1. Create basic worker and HTTP request-response classes. (WIP)
    - Create and integrate a request handler class!
    - Ensure compliance with other HTTP/1.1 requirements based on this link: [HTTP Made Really Easy](https://www.jmarshall.com/easy/http/#http1.1s1)
 2. Complete main, driver class. (DONE)
 3. Handle GET requests with `text/plain`. (DONE)
 4. Handle GET requests with `text/html`. (DONE)
 5. Get the server to serve static pages. (WIP)
