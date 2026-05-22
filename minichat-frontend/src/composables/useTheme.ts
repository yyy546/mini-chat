import { ref, watch } from 'vue'
import { useUserStore } from '@/store/user'

const STORAGE_PREFIX = 'theme'

function getStorageKey(userId: number): string {
  return `${STORAGE_PREFIX}:${String(userId)}`
}

export function useTheme() {
  const isDark = ref(false)
  const userStore = useUserStore()

  function applyTheme(dark: boolean) {
    isDark.value = !!dark
    document.documentElement.classList.toggle('dark', isDark.value)
  }

  function resolveInitial(userId: number | null): boolean {
    if (!userId) return false
    const saved = localStorage.getItem(getStorageKey(userId))
    if (saved === 'dark') return true
    if (saved === 'light') return false
    return false
  }

  function toggle() {
    const userId = userStore.userId
    const next = !isDark.value
    applyTheme(next)
    if (userId) {
      localStorage.setItem(getStorageKey(userId), next ? 'dark' : 'light')
    }
  }

  watch(
    () => userStore.userId,
    (newId, oldId) => {
      if (newId === oldId) return
      applyTheme(newId ? resolveInitial(newId) : false)
    }
  )

  return { isDark, applyTheme, resolveInitial, toggle }
}
