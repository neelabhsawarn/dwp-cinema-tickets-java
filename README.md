# Cinema Tickets — Java Solution

## Overview
A Java 21 implementation of the DWP Cinema Tickets
coding exercise.

The service validates ticket purchase requests,
calculates the amount payable, and reserves seats
using the provided external service interfaces.

## Business Rules
- Adult tickets cost £25.
- Child tickets cost £15.
- Infant tickets are free and require no seat.
- Purchases are limited to 25 tickets.
- An adult ticket is required for a valid purchase.
- Account IDs must be positive.
- Ticket quantities must be positive.
- Invalid purchases are rejected before external
  services are called.
- The 25-ticket purchase limit includes Adult, Child and Infant tickets, even though Infant     
tickets do not require payment or a seat.


## Design
TicketServiceImpl implements the supplied
TicketService interface.

Constructor injection allows the payment and
seat reservation services to be mocked in tests.

TicketTypeRequest is immutable.

## Assumptions
- At least one adult ticket is required per purchase.
- Multiple infants can accompany an adult because
  the specification does not explicitly define an
  infant-to-adult ratio.
- All valid accounts have sufficient funds.
- The external services succeed as specified.

## Testing
The project uses JUnit 5 and Mockito.

Run the tests with:

    mvn clean test

Run the full Maven verification with:

    mvn clean verify

The test suite covers valid purchases, pricing,
seat allocation, ticket limits, invalid requests,
and external service interactions.

## Requirements
- Java 21
- Apache Maven
