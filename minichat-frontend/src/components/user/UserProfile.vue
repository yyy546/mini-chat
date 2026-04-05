<template>
  <el-dialog
    v-model="visible"
    title="修改个人资料"
    width="500px"
    :before-close="handleClose"
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
      v-loading="loading"
    >
      <div class="avatar-container">
        <div class="avatar-wrapper" @click="triggerFileSelect">
          <el-avatar :size="80" :src="form.avatar" />
          <div class="avatar-mask">
            <el-icon><Camera /></el-icon>
          </div>
        </div>
        <input 
          type="file" 
          ref="fileInput" 
          style="display: none" 
          accept="image/*"
          @change="handleFileChange"
        >
      </div>

      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="请输入昵称" />
      </el-form-item>
      
      <el-form-item label="性别" prop="gender">
        <el-radio-group v-model="form.gender">
          <el-radio label="未知">未知</el-radio>
          <el-radio label="男">男</el-radio>
          <el-radio label="女">女</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="个性签名" prop="signature">
        <el-input 
          v-model="form.signature" 
          type="textarea" 
          :rows="3"
          placeholder="请输入个性签名" 
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          提交
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { getUserDetail, updateUserDetail } from '../../api/auth'
import { uploadFileUnified } from '../../api/upload'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../store/user'
import { Camera } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const userStore = useUserStore()
const fileInput = ref(null)
const rawAvatarFile = ref(null)

const formRef = ref(null)
const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度必须在2到20之间', trigger: 'blur' }
  ]
}

const form = reactive({
  nickname: '',
  gender: '未知',
  signature: '',
  avatar: ''
})

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    fetchData()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getUserDetail()
    if (res.code === 1 && res.data) {
      Object.assign(form, {
        nickname: res.data.nickname || '',
        gender: res.data.gender || '未知',
        signature: res.data.signature || '',
        avatar: res.data.avatar || ''
      })
    }
  } catch (e) {
    console.error('获取个人资料失败', e)
    ElMessage.error('获取个人资料失败')
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  visible.value = false
}

const triggerFileSelect = () => {
  fileInput.value.click()
}

const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (!file) return
  
  // 限制文件大小 (例如 2MB)
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    return
  }

  rawAvatarFile.value = file
  const reader = new FileReader()
  reader.onload = (e) => {
    form.avatar = e.target.result
  }
  reader.readAsDataURL(file)
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid, fields) => {
    if (valid) {
      submitting.value = true
      try {
        let avatarUrl = form.avatar

        if (rawAvatarFile.value) {
          const result = await uploadFileUnified({
            file: rawAvatarFile.value,
            scene: 'userAvatar'
          })
          avatarUrl = result.fileUrl
        }

        if (avatarUrl && avatarUrl.startsWith('data:image')) {
             avatarUrl = '' 
        }

        // 2. 构造更新DTO
        const updateData = {
          nickname: form.nickname,
          gender: form.gender,
          signature: form.signature || '',
          avatar: avatarUrl
        }

        const res = await updateUserDetail(updateData)
        if (res.code === 1) {
          ElMessage.success('修改成功')
          // 更新Store中的用户信息
          if (res.data) {
            userStore.userInfo = { ...userStore.userInfo, ...res.data }
          }
          visible.value = false
          emit('success')
        } else {
          ElMessage.error(res.msg || '修改失败')
        }
      } catch (e) {
        console.error('修改个人资料失败', e)
        ElMessage.error('修改个人资料失败')
      } finally {
        submitting.value = false
      }
    } else {
      console.log('表单校验失败', fields)
    }
  })
}
</script>

<style scoped>
.avatar-container {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: #fff;
  font-size: 24px;
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}
</style>
