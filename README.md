# API Platform Interceptor - v0.1 - POC

## Description
This server is used to intercept all internal calls to API Platform, and return a predefined/calculated payload and status code for some or all the URLs called, including the possibility to specify also HTTP verbs (eg GET, POST, etc).

This tool can be used to simulate weird response conditions from API Platform either on the Cloud or On-Prem, including cluster+load balancer where n nodes are down, throttled API Platform, etc.

## Usage
### API Gateway configuration
Example production configuration, using wrapper.conf file:
```
wrapper.java.additional.17=-Danypoint.platform.platform_base_uri=https://127.0.0.1:8992/prod/1234/apiplatform
wrapper.java.additional.18=-Danypoint.platform.coreservice_base_uri=https://127.0.0.1:8992/prod/1234/accounts
wrapper.java.additional.20=-Danypoint.platform.contracts_base_uri=https://127.0.0.1:8992/prod/1234/apigateway/ccs
```
Where `1234` is the Runtime ID (it must be unique per runtime), `prod` is the target environment (currently, it can be prod, stg or qa) and `https://127.0.0.1:8992` is where the interceptor is listening to.

### Interceptor definitions
POST http://127.0.0.1:8991/http/status
Content-type: application/json
#### Payload
Using number of successful calls:
```
{
    "runtimeId" : "1234",
    "statusCode" : "500",
    "payload" : "returned payload from postman",
    "pathIntercepted" : "/apiplatform/",
    "HttpMethod" : "GET",
    "failingStrategy" : "FAILS_VS_OKS",
    "msgBeforeFailing" : 3
}
```
Using percentage of successful calls:
```
{
    "runtimeId" : "1234",
    "statusCode" : "500",
    "payload" : "returned payload from postman",
    "pathIntercepted" : "/apiplatform/",
    "HttpMethod" : "GET",
    "failingStrategy" : "PORCENTAGE_OF_FAILING",
     "porcentage" : 10
}
```
