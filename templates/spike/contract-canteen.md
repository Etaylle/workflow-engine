# Confirmed contract: school canteen (snapshot v1)

Source: the canteen example of the engine's stubs (`StubWorkflowWriter`, `StubQuestionWriter`,
`StubRequirementWriter`). This file is the contract the builder must satisfy. It never changes.

## Wish

Students order a meal at the school canteen on their phone and pick it up. The kitchen sees the
orders, prepares them and tells the student when the meal is ready.

## Answers to the questions

- How do students pay? Online at the order.
- What can a student do? Order a meal, see the menu.
- How long is the pick-up window? 30 minutes.

## Requirements (epic "Ordering" and epic "Kitchen")

- FR: A student can order a meal from the menu. (must)
- FR: A student pays online when ordering. (should)
- QR: An order is confirmed within 3 seconds. (should)
- FR: The kitchen sees every open order and marks it ready. (must)
- CON: A meal can be picked up within the pick-up window. (must)

## Workflow (lanes Student and Kitchen)

start "Hungry" (Student) -> task "Order a meal" (Student, software) -> task "Prepare the meal"
(Kitchen, manual) -> task "Notify the student" (Kitchen, software) -> task "Pick up the meal"
(Student, manual) -> end "Meal served" (Student)

## The task to build in this run

Task "Order a meal" (lane Student, type software). Description from the contract: "Pick a meal and
pay." It is the first software task of the workflow; "Notify the student" is built later and is
out of scope here. The result of this task must be visible in the running application: a student
sees the menu, picks a meal, pays (a simulated payment is enough, no real provider), and gets an
order confirmation with an order number. The order is stored in PostgreSQL with status
`ordered`, so that the later task "Prepare the meal" can read it.
