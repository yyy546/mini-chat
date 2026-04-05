<template>
  <el-dialog
    v-model="visible"
    title="创建群聊"
    width="500px"
    @close="handleClose"
    class="create-group-dialog"
  >
    <div class="create-group-container">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" label-position="left">
        <el-form-item label="群名称" prop="groupName">
          <el-input v-model="form.groupName" placeholder="请输入群名称" maxlength="20" show-word-limit />
        </el-form-item>
        
        <el-form-item label="群公告" prop="announcement">
          <el-input v-model="form.announcement" type="textarea" :rows="2" placeholder="请输入群公告（选填）" maxlength="100" show-word-limit />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
             <el-form-item label="最大成员" prop="maxMembers">
              <el-input-number v-model="form.maxMembers" :min="3" :max="200" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="加入策略" prop="joinPolicy">
              <el-select v-model="form.joinPolicy" placeholder="请选择">
                <el-option label="自由加入" :value="0" />
                <el-option label="需审批" :value="1" />
                <el-option label="仅邀请" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邀请策略" prop="invitePolicy">
              <el-select v-model="form.invitePolicy" placeholder="请选择">
                <el-option label="所有成员" :value="0" />
                <el-option label="管理员" :value="1" />
                <el-option label="群主" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="member-select-area">
        <div class="area-header">
          <span>选择好友 ({{ selectedMembers.length }})</span>
        </div>
        <el-input 
          v-model="searchKeyword" 
          placeholder="搜索好友" 
          prefix-icon="Search" 
          clearable 
          class="search-input" 
        />
        
        <el-scrollbar height="250px" class="friend-list-scroll">
          <el-checkbox-group v-model="selectedMembers">
            <div v-for="friend in filteredFriends" :key="friend.id" class="friend-item-row">
              <el-checkbox :label="friend.id" class="custom-checkbox">
                <div class="friend-content">
                  <el-avatar :size="32" :src="friend.avatar" class="friend-avatar">
                    {{ (friend.remark || friend.nickname || friend.name || '?').charAt(0) }}
                  </el-avatar>
                  <span class="friend-name">{{ friend.remark || friend.nickname || friend.name }}</span>
                </div>
              </el-checkbox>
            </div>
          </el-checkbox-group>
          <el-empty v-if="filteredFriends.length === 0" description="未找到好友" :image-size="60" />
        </el-scrollbar>
      </div>
    </div>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit" :loading="loading">立即创建</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useFriendStore } from '../../store/friend'
import { createGroup } from '../../api/group'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'created'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const friendStore = useFriendStore()
const formRef = ref(null)
const loading = ref(false)

watch(visible, async (val) => {
  if (val && (!friendStore.friends || friendStore.friends.length === 0)) {
    await friendStore.fetchFriends()
  }
})


const form = reactive({
  groupName: '',
  announcement: '',
  maxMembers: 200,
  joinPolicy: 0,
  invitePolicy: 0
})

const rules = {
  groupName: [{ required: true, message: '请输入群名称', trigger: 'blur' }],
  maxMembers: [{ required: true, message: '请输入最大成员数量', trigger: 'blur' }],
  joinPolicy: [{ required: true, message: '请选择加入策略', trigger: 'change' }],
  invitePolicy: [{ required: true, message: '请选择邀请策略', trigger: 'change' }]
}

const selectedMembers = ref([])
const searchKeyword = ref('')

// Filter only actual friends (not groups if mixed, though store.friends usually only has users)
const filteredFriends = computed(() => {
  const friends = friendStore.friends || []
  // Filter out if type is 1 (group) just in case
  const userFriends = friends.filter(f => f.type === 0 || f.type === undefined)
  
  if (!searchKeyword.value) return userFriends
  
  const k = searchKeyword.value.toLowerCase()
  return userFriends.filter(f => 
    (f.remark || '').toLowerCase().includes(k) ||
    (f.nickname || '').toLowerCase().includes(k) ||
    (f.name || '').toLowerCase().includes(k)
  )
})

const handleClose = () => {
  formRef.value?.resetFields()
  selectedMembers.value = []
  searchKeyword.value = ''
  // Reset form to defaults if needed, though resetFields handles props
  form.maxMembers = 200
  form.joinPolicy = 0
  form.invitePolicy = 0
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const payload = {
          ...form,
          memberIds: selectedMembers.value
        }
        const res = await createGroup(payload)
        ElMessage.success('创建群聊成功')
        visible.value = false
        emit('created', res.data)
        handleClose()
      } catch (e) {
        console.error(e)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.create-group-container {
  padding: 0 10px;
}
.member-select-area {
  border-top: 1px solid var(--el-border-color-lighter);
  margin-top: 10px;
  padding-top: 15px;
}
.area-header {
  font-weight: 600;
  margin-bottom: 10px;
  font-size: 14px;
  color: var(--el-text-color-regular);
}
.search-input {
  margin-bottom: 12px;
}
.friend-list-scroll {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  padding: 5px;
}
.friend-item-row {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.friend-item-row:last-child {
  border-bottom: none;
}
.friend-item-row:hover {
  background-color: var(--el-fill-color-light);
}
.custom-checkbox {
  width: 100%;
  margin-right: 0;
  height: auto;
  display: flex;
  align-items: center;
}
/* Deep selector to adjust element-plus checkbox layout */
:deep(.el-checkbox__label) {
  flex: 1;
}
.friend-content {
  display: flex;
  align-items: center;
  gap: 10px;
}
.friend-avatar {
  flex-shrink: 0;
}
.friend-name {
  font-size: 14px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
