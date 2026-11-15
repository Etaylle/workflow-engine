import assert from "node:assert/strict";
import { test } from "node:test";
import { greeting } from "./main.ts";

test("greeting says hello", () => {
	assert.equal(greeting(), "Hello from the console program");
});
