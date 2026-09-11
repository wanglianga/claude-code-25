/** 枚举 → 中文/标签颜色 字典 */
export const dicts = {
  childStatus: {
    PENDING: { label: '待提交', type: 'info' },
    ASSESSING: { label: '评估中', type: 'warning' },
    ASSIGNED: { label: '已分班', type: 'primary' },
    ENROLLED: { label: '已入托', type: 'success' },
    SUSPENDED: { label: '暂停入托', type: 'danger' }
  },
  appStatus: {
    PENDING_CLASS: { label: '待分班', type: 'warning' },
    PENDING_HEALTH: { label: '待健康评估', type: 'primary' },
    NEED_SUPPLEMENT: { label: '需补充资料', type: 'danger' },
    COMPLETED: { label: '已入托', type: 'success' },
    REJECTED: { label: '已拒绝', type: 'info' }
  },
  healthResult: {
    PASS: { label: '通过', type: 'success' },
    ALLERGY_MEAL: { label: '通过（需过敏餐）', type: 'warning' },
    TEMP_OBSERVATION: { label: '通过（需临时观察）', type: 'warning' },
    NEED_SUPPLEMENT: { label: '需家长补充资料', type: 'danger' }
  },
  morningResult: {
    ENTER_CLASS: { label: '正常入班', type: 'success' },
    ISOLATION: { label: '隔离观察', type: 'warning' },
    PARENT_PICKUP: { label: '通知家长接回', type: 'danger' }
  },
  careType: {
    NOON_CARE: { label: '午间照护', type: 'primary' },
    MEDICATION: { label: '喂药', type: 'warning' },
    TOILET: { label: '如厕', type: 'info' },
    SLEEP: { label: '睡眠', type: 'success' },
    INJURY: { label: '活动伤情', type: 'danger' },
    MEAL: { label: '用餐', type: 'success' }
  },
  severity: {
    NORMAL: { label: '一般', type: 'success' },
    ATTENTION: { label: '需关注', type: 'warning' },
    SERIOUS: { label: '严重', type: 'danger' }
  },
  injuryType: {
    FALL: '跌倒',
    BUMP: '磕碰',
    SCRATCH_BITE: '抓伤/咬伤',
    OTHER: '其他'
  },
  eventType: {
    UNAUTHORIZED_PICKUP: { label: '未授权接送', type: 'danger' },
    FEVER: { label: '发热', type: 'danger' },
    MEDICATION_MISSING: { label: '药品漏带', type: 'warning' },
    BITE_INCIDENT: { label: '抓咬事件', type: 'danger' },
    INJURY: { label: '活动伤情', type: 'warning' },
    REFUND_DISPUTE: { label: '退费争议', type: 'warning' },
    OTHER: { label: '其他', type: 'info' }
  },
  eventStatus: {
    OPEN: { label: '待处理', type: 'danger' },
    PROCESSING: { label: '处理中', type: 'warning' },
    RESOLVED: { label: '已解决', type: 'success' },
    CLOSED: { label: '已关闭', type: 'info' }
  },
  pickupResult: {
    SUCCESS: { label: '已放行', type: 'success' },
    DENIED: { label: '已拒绝', type: 'danger' }
  },
  verifyMethod: {
    FACE: '人脸核验',
    ID_CARD: '证件核验'
  },
  authType: {
    AUTHORIZED: '固定授权人',
    TEMP_DELEGATION: '临时委托'
  },
  delegationStatus: {
    ACTIVE: { label: '生效中', type: 'success' },
    USED: { label: '已使用', type: 'info' },
    CANCELLED: { label: '已取消', type: 'info' }
  },
  medicationStatus: {
    ACTIVE: { label: '在服', type: 'success' },
    FINISHED: { label: '已结束', type: 'info' },
    CANCELLED: { label: '已取消', type: 'info' }
  },
  suggestion: {
    CLASS_ADJUST: { label: '建议调整班级', type: 'warning' },
    HEALTH_OBSERVATION: { label: '建议增加保健观察', type: 'primary' },
    PARENT_MEETING: { label: '建议约谈家长', type: 'danger' }
  },
  alertStatus: {
    OPEN: { label: '待处理', type: 'danger' },
    HANDLED: { label: '已处理', type: 'success' }
  },
  timelineCategory: {
    APPLICATION: { label: '入托申请', type: 'primary' },
    ASSIGNMENT: { label: '分班', type: 'primary' },
    HEALTH: { label: '健康评估', type: 'success' },
    MORNING_CHECK: { label: '晨检', type: 'warning' },
    CARE: { label: '在园照护', type: 'success' },
    PICKUP: { label: '接送', type: 'primary' },
    EVENT: { label: '事件', type: 'danger' },
    COMMUNICATION: { label: '沟通', type: 'warning' },
    ALERT: { label: '预警', type: 'danger' },
    MEAL: { label: '过敏餐替换', type: 'warning' }
  },
  mealType: {
    LUNCH: { label: '午餐', type: 'primary' },
    SNACK: { label: '午点', type: 'success' }
  },
  mealReason: {
    INGREDIENT_SHORTAGE: { label: '食材缺货', type: 'warning' },
    ALLERGEN_RISK: { label: '过敏源风险', type: 'danger' }
  },
  mealSubStatus: {
    PENDING_HEALTH: { label: '待保健老师确认', type: 'warning' },
    PENDING_TEACHER: { label: '待班级老师确认', type: 'warning' },
    PENDING_PARENT: { label: '待家长确认', type: 'warning' },
    CONFIRMED: { label: '待厨房出餐', type: 'primary' },
    EXECUTED: { label: '已出餐待分餐', type: 'primary' },
    SERVED: { label: '已分餐', type: 'success' },
    REJECTED: { label: '已拒绝', type: 'danger' },
    CANCELLED: { label: '已取消', type: 'info' }
  },
  role: {
    PARENT: '家长',
    TEACHER: '老师',
    HEALTH: '保健老师',
    DIRECTOR: '园长',
    FRONTDESK: '前台',
    KITCHEN: '厨房'
  }
}

export function d(group, key) {
  return dicts[group]?.[key]?.label ?? key ?? '—'
}

export function dType(group, key) {
  return dicts[group]?.[key]?.type ?? 'info'
}
