## Update status

Possible statuses:
- open
- pending
- resolved
- closed
- waiting_on_customer
- waiting_on_3rd_party
- waiting_on_developer
- waiting_on_senior_stuff_input
- waiting_on_jira
- cie_deletion

```
POST http://localhost:8080/api/status/34677/waiting_on_customer
Accept: application/json
```

## Send message

```
PUT http://localhost:8080/api/message/34677
Accept: application/json
Content-Type: application/json

{
  "message": "Hello from integration"
}
```
