# XRP Payment App 

A Spring Boot–based service that enables seamless XRP transactions using the XRP Ledger via [xrpl4j](https://github.com/XRPLF/xrpl4j). This app allows you to programmatically send payments, check account balances, and interact with the XRP Testnet or Mainnet.

WIP - Work In Progress
---

## Features

- Secure key management (via Spring config or environment variables)
- Send XRP payments (Partially Complete)
- Retrieve transaction status (Pending)
- Fetch account balance (Pending)
- Connect to Ripple Testnet (default)
- Easily extendable for custom transactions

---

## Tech Stack

- Java 17+
- Spring Boot 3.x
- [xrpl4j](https://github.com/XRPLF/xrpl4j)
- OkHttp for HTTP client
- Maven
- Docker

---

## Configuration

Set the following environment variables or add them to `application.properties`:

