# Aura Demo Readiness

## Sample Prompts

### 1. Transaction ingestion assistant
Use this with `POST /transaction-extraction`

```text
Bought groceries for 1850, coffee for 150, and paid jeep fare 13 today.
```

### 2. Spending analysis
Use this with `POST /spending-analysis`

```json
{
  "startDate": "2026-03-01",
  "endDate": "2026-03-31"
}
```

### 3. Financial strategy explanation
Use this with `POST /financial-strategy/explain`

```json
{
  "startDate": "2026-03-01",
  "endDate": "2026-03-31",
  "plannedPurchaseAmount": 50000.00,
  "expectedMonthlyReturnRate": 0.01,
  "timeHorizonMonths": 36
}
```

## Sample Transaction History Dataset

File:
[sample-transaction-history.json](c:/Users/Phoebe%20Rhone%20Gangoso/Downloads/aura-finance/demo/sample-transaction-history.json)

Use it by posting chunks of the data to:

```text
POST /transaction-extraction/confirm
```

Suggested March narrative in the dataset:
- groceries are the largest recurring expense
- utilities are the next heavy category
- food and transport are frequent but smaller
- savings exists, which makes the strategy story stronger

## Strong Demo Story

### Title
From messy expenses to strategy: "Should I buy this now?"

### Flow
1. Show raw natural-language input.

Use:

```text
Bought groceries for 1850, coffee for 150, and paid jeep fare 13 today.
```

Say:
`Aura turns messy real-world text into structured financial records.`

2. Confirm and save extracted transactions.

Say:
`The user stays in control. AI suggests transactions, and the user confirms before anything is saved.`

3. Show spending analysis for last month.

Use:

```json
{
  "startDate": "2026-03-01",
  "endDate": "2026-03-31"
}
```

Say:
`Aura can explain where the money is going, not just store transactions.`

Key question:
`What did I spend most on last month?`

Expected theme:
- groceries should dominate
- utilities should also stand out

4. Show purchase simulation.

Use:

```json
{
  "purchaseAmount": 50000.00,
  "expectedMonthlyReturnRate": 0.01,
  "timeHorizonMonths": 36
}
```

Key question:
`What is the long-term cost if I buy this instead of investing it?`

Say:
`Aura quantifies the opportunity cost of the purchase.`

5. Show financial strategy explanation.

Use:

```json
{
  "startDate": "2026-03-01",
  "endDate": "2026-03-31",
  "plannedPurchaseAmount": 50000.00,
  "expectedMonthlyReturnRate": 0.01,
  "timeHorizonMonths": 36
}
```

Key question:
`Should I buy this now?`

Say:
`Aura combines spending behavior and opportunity cost to generate a more strategic recommendation.`

## Fast Demo Script

```text
Project Aura reduces financial decision fatigue.
First, it converts messy natural-language expense input into structured transactions.
Second, it analyzes spending patterns over time.
Third, it simulates the opportunity cost of a purchase.
Finally, it explains a practical strategy recommendation using both spending behavior and purchase simulation.
```
