# Aura Live Demo Runbook

## 0. Start The App

Open a PowerShell terminal in the project root and run:

```powershell
mvn spring-boot:run
```

Wait until Spring Boot finishes starting.

## 1. Extract Transactions From Raw Text

Say:
`Aura converts messy natural-language expense notes into structured transactions.`

Run:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/transaction-extraction -ContentType "application/json" -Body '{"rawText":"Bought groceries for 1850, coffee for 150, and paid jeep fare 13 today.","referenceDate":"2026-04-09"}'
```

Expected theme:
- `Groceries`
- `Coffee`
- `Jeep fare`

## 2. Confirm And Save Extracted Transactions

Say:
`The user stays in control. AI suggests transactions first, and only confirmed items are saved.`

Run:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/transaction-extraction/confirm -ContentType "application/json" -Body '{"transactions":[{"description":"Groceries","amount":1850.00,"category":"GROCERIES","transactionDate":"2026-04-09"},{"description":"Coffee","amount":150.00,"category":"FOOD","transactionDate":"2026-04-09"},{"description":"Jeep fare","amount":13.00,"category":"TRANSPORT","transactionDate":"2026-04-09"}]}'
```

Expected theme:
- response includes generated transaction `id`s

## 3. Load Sample March History

Say:
`For the strategy features, I prepared a sample monthly transaction history.`

Run:

```powershell
$transactions = Get-Content .\demo\sample-transaction-history.json -Raw
Invoke-RestMethod -Method Post -Uri http://localhost:8080/transaction-extraction/confirm -ContentType "application/json" -Body "{`"transactions`":$transactions}"
```

Expected theme:
- response shows many saved entries

## 4. Ask What Was Spent Most Last Month

Say:
`Aura can explain where the money went, not just store it.`

Run:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/spending-analysis -ContentType "application/json" -Body '{"startDate":"2026-03-01","endDate":"2026-03-31"}'
```

Key line to say:
`What did I spend most on last month?`

Expected theme:
- `GROCERIES` should be the top category
- `UTILITIES` should also be significant

## 5. Show The Opportunity Cost Simulator

Say:
`Aura also models the long-term opportunity cost of a potential purchase.`

Run:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/purchase-simulation -ContentType "application/json" -Body '{"purchaseAmount":50000.00,"expectedMonthlyReturnRate":0.01,"timeHorizonMonths":36}'
```

Key line to say:
`What is the long-term cost if I buy this instead of investing it?`

Expected theme:
- shows `futureValueIfInvested`
- shows `opportunityCost`
- includes a plain-English explanation

## 6. Show The AI Financial Strategy Recommendation

Say:
`This is the key Aura moment: it combines actual spending behavior with opportunity cost and generates a strategy recommendation.`

Run:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/financial-strategy/explain -ContentType "application/json" -Body '{"startDate":"2026-03-01","endDate":"2026-03-31","plannedPurchaseAmount":50000.00,"expectedMonthlyReturnRate":0.01,"timeHorizonMonths":36}'
```

Key line to say:
`Should I buy this now?`

Expected theme:
- `spendingInsight`
- `cautionFlags`
- `recommendationText`

## 7. Closing Script

Say:

```text
Project Aura reduces financial decision fatigue.
It converts messy financial text into structured transactions,
analyzes spending behavior,
simulates purchase opportunity cost,
and generates strategy guidance from structured financial data.
```

## 8. Backup Demo Path

If H2 was cleared because the app restarted:

1. Run Step 3 again to reload the sample dataset.
2. Then continue with Steps 4, 5, and 6.

## 9. Fast Reference

Endpoints used:

```text
POST /transaction-extraction
POST /transaction-extraction/confirm
POST /spending-analysis
POST /purchase-simulation
POST /financial-strategy/explain
```
