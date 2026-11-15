export function greeting(): string {
	return "Hello from the console program";
}

if (import.meta.main) {
	console.log(greeting());
}
