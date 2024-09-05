# pwrteams
test task for pwrteams
# technical part
Developed using Oracle Open JDK 17.0.5.
Gradle project.
Reactive framework: webflux.
Cache: caffeine (in production it will be Redis).
Mapper: mapstruct.
Open API documentation(swagger) : generated by springdoc-openapi-starter-webflux-ui
  after Netty is up - we can find swagger page here /openapi/swagger-ui.html
  and documentation is here /openapi/v3/api-docs.
Dokerfile is in the root.
Tests use wiremock.
# functionality
/users/{username}/repositories.
can use parameter ?filter=(focked, nonforked, all (by default)).
On first query system cache the pages - pagination is used so app goes through all 
  pages and saves pages and ETags. Next time with the same page it sends etag and if
  we have 304 reponse take page from cache. (At first I have done loading the pages 
  from cache in the event of network problems, later decided against it).
All configurations in the application.yaml.

//TODO Was not validating data received from Git Hub. 
  
