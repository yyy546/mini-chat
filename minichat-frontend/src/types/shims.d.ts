declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}

declare module 'stompjs' {
  const Stomp: {
    over: (ws: unknown) => Stomp.Client
    client: (url: string, headers?: Record<string, string>) => Stomp.Client
  }
  namespace Stomp {
    interface Client {
      connected: boolean
      connect(
        headers: Record<string, string>,
        onConnect: (frame: Frame) => void,
        onError: (error: Frame) => void
      ): void
      disconnect(callback?: () => void): void
      subscribe(destination: string, callback: (message: Message) => void): Subscription
      send(destination: string, headers: Record<string, string>, body: string): void
      debug: ((msg: string) => void) | null
    }
    interface Frame {
      command?: string
      headers: Record<string, string>
      body: string
      message?: string
    }
    interface Message {
      headers: Record<string, string>
      body: string
    }
    interface Subscription {
      id: string
      unsubscribe(): void
    }
  }
  export default Stomp
}

declare module 'sockjs-client' {
  const SockJS: new (url: string) => WebSocket
  export default SockJS
}
