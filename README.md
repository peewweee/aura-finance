# Aura Finance

Aura Finance is a web app that helps users log expenses in natural language, review extracted transactions, understand spending patterns, and get AI-assisted financial guidance.

Public test web:
- aura-finance.me

## What The App Does

- lets users type purchases in normal language like `Groceries 1850, kape 150, at jeep 13 today`
- extracts possible transactions from that text
- allows users to review and save selected entries
- shows spending analysis and category breakdowns
- explains the long-term cost of spending through opportunity-cost simulation
- gives AI-generated financial strategy guidance based on saved entries

## Main Features

- Quick Log for natural-language transaction entry
- Saved Entries view for confirmed transactions
- Spending Report with totals and insights
- Cost of Spending simulation
- Aura's Advice for strategy recommendations

## Current Architecture

- frontend served by Spring Boot
- Spring Boot backend for API and business logic
- anonymous session-based usage, no account required
- Neon PostgreSQL for persistent storage
- Gemini API for hosted AI features
- Redis kept off for the current public test deployment

## Tech Stack

- Java 21
- Spring Boot
- Spring Data JPA
- H2 for local development
- PostgreSQL / Neon for hosted deployment
- vanilla HTML, CSS, and JavaScript
- Gemini API for AI-assisted extraction and analysis

## Notes

- This is currently a public test app, not a fully hardened production system.
- Users do not need to create an account.
- Each browser session is handled through anonymous session tracking.
