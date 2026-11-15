/** Short confirmations after a save; announced politely, gone after a few seconds. */
const SHOW_MS = 4000;

class Toasts {
	items = $state<{ id: number; text: string }[]>([]);
	#next = 1;

	show(text: string): void {
		const id = this.#next++;
		this.items = [...this.items, { id, text }];
		setTimeout(() => this.dismiss(id), SHOW_MS);
	}

	dismiss(id: number): void {
		this.items = this.items.filter((t) => t.id !== id);
	}
}

export const toasts = new Toasts();
