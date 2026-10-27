/** Stands in for the browser's EventSource, which jsdom lacks. Tests push events through the open instances. */
export class FakeEventSource {
  static instances: FakeEventSource[] = []

  readonly url: string
  onopen: ((event: Event) => void) | null = null
  onmessage: ((event: MessageEvent<string>) => void) | null = null
  closed = false

  constructor(url: string) {
    this.url = url
    FakeEventSource.instances.push(this)
  }

  open() {
    this.onopen?.(new Event('open'))
  }

  emit(data: unknown) {
    this.onmessage?.(new MessageEvent('message', { data: JSON.stringify(data) }))
  }

  close() {
    this.closed = true
  }

  static reset() {
    FakeEventSource.instances = []
  }
}
