# Saved searches (KQL)

Create each in **Discover** → save with the given name.

| Name | KQL |
|------|-----|
| VycePay — All errors | `log.level: "ERROR"` |
| VycePay — Warn and errors | `log.level: ("ERROR" or "WARN")` |
| VycePay — By requestId | `requestId: "<paste-id>"` |
| VycePay — Choice BaaS | `event.dataset: "choice.baas"` |
| VycePay — Choice failures | `event.dataset: "choice.baas" and event.outcome: "failure"` |
| VycePay — Callbacks | `event.dataset: "choice.callback"` |
| VycePay — BFF | `service.name: "vycepay-bff"` |
| VycePay — Auth | `service.name: "vycepay-auth-service"` |
| VycePay — Transaction | `service.name: "vycepay-transaction-service"` |
| VycePay — Callback service | `service.name: "vycepay-callback-service"` |
| VycePay — Admin | `service.name: "vycepay-admin-service"` |
| VycePay — SMS | `event.dataset: "vyce.sms"` |
| VycePay — Push | `event.dataset: "vyce.push"` |
| VycePay — By onboardingRequestId | `onboardingRequestId: "<paste-id>"` |
| VycePay — By choiceBankRequestId | `choiceBankRequestId: "<paste-id>"` |
| VycePay — By transactionId | `transactionId: "<paste-id>"` |
