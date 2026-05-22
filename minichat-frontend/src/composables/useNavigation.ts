import { ref, watch } from 'vue'
import { useChatStore } from '@/store/chat'

export function useNavigation() {
  const activeTab = ref<'message' | 'contacts' | 'space'>('message')
  const contactTab = ref<'friend' | 'group'>('friend')
  const chatStore = useChatStore()

  watch(activeTab, async (tab) => {
    if (tab === 'message') {
      await chatStore.fetchSessions()
    }
  })

  function switchToMessage(user: Record<string, unknown>) {
    activeTab.value = 'message'
    chatStore.setActiveUser(user as unknown as { id: number; type: number; [key: string]: unknown })
  }

  function switchToContact(type: 'friend' | 'group', _id: number) {
    activeTab.value = 'contacts'
    contactTab.value = type
  }

  return { activeTab, contactTab, switchToMessage, switchToContact }
}
