import { ElMessage } from 'element-plus'

export const showError = error => { if (error.status !== 401) ElMessage.error(error.message || '操作失败，请稍后重试') }
export const formatTime = value => value ? value.replace('T', ' ').slice(0, 19) : '—'
export const isCancel = error => error === 'cancel' || error === 'close'
export function textLength(min, max, label) {
  return { validator: (_rule, value, callback) => {
    const length = String(value ?? '').trim().length
    callback(length >= min && length <= max ? undefined : new Error(label + '长度为 ' + min + '–' + max + ' 个字符'))
  }, trigger: 'blur' }
}
export function trimmedRequired(label) {
  return { validator: (_rule, value, callback) => value?.trim() ? callback() : callback(new Error('请输入' + label)), trigger: 'blur' }
}
export const usernameRules = [trimmedRequired('用户名'), textLength(3, 30, '用户名'), {
  validator: (_rule, value, callback) => /^[A-Za-z0-9_]+$/.test(value?.trim() || '') ? callback() : callback(new Error('用户名只允许英文字母、数字和下划线')),
  trigger: 'blur',
}]
export function passwordRule(optional = false) {
  return { validator: (_rule, value, callback) => {
    const password = String(value ?? '').trim()
    if (optional && !password) return callback()
    if (password.length < 8 || password.length > 64) return callback(new Error('密码长度为 8–64 个字符'))
    if (new TextEncoder().encode(password).length > 72) return callback(new Error('密码的 UTF-8 长度不能超过 72 字节，请缩短中文或特殊字符密码'))
    callback()
  }, trigger: 'blur' }
}
