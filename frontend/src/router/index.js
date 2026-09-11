import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台' } },
      // 家长
      { path: 'parent/enrollment', component: () => import('../views/parent/Enrollment.vue'), meta: { title: '入托申请', roles: ['PARENT'] } },
      { path: 'parent/children/:id', component: () => import('../views/parent/ChildDetail.vue'), meta: { title: '孩子详情', roles: ['PARENT'] } },
      { path: 'parent/meal-subs', component: () => import('../views/parent/MealSubs.vue'), meta: { title: '替换餐确认', roles: ['PARENT'] } },
      // 园长
      { path: 'director/tasks', component: () => import('../views/director/Tasks.vue'), meta: { title: '入托评估任务', roles: ['DIRECTOR'] } },
      { path: 'director/classes', component: () => import('../views/director/Classes.vue'), meta: { title: '班级管理', roles: ['DIRECTOR'] } },
      { path: 'director/alerts', component: () => import('../views/director/Alerts.vue'), meta: { title: '异常预警', roles: ['DIRECTOR', 'HEALTH'] } },
      // 保健老师
      { path: 'health/tasks', component: () => import('../views/health/HealthTasks.vue'), meta: { title: '健康评估', roles: ['HEALTH'] } },
      { path: 'health/meal-subs', component: () => import('../views/health/MealSubs.vue'), meta: { title: '替换餐确认', roles: ['HEALTH'] } },
      // 老师
      { path: 'teacher/morning-check', component: () => import('../views/teacher/MorningCheck.vue'), meta: { title: '每日晨检', roles: ['TEACHER'] } },
      { path: 'teacher/day-care', component: () => import('../views/teacher/DayCare.vue'), meta: { title: '在园照护记录', roles: ['TEACHER'] } },
      { path: 'teacher/meal-serving', component: () => import('../views/teacher/MealServing.vue'), meta: { title: '分餐与替换餐', roles: ['TEACHER'] } },
      // 厨房
      { path: 'kitchen/menus', component: () => import('../views/kitchen/Menus.vue'), meta: { title: '每日菜单', roles: ['KITCHEN'] } },
      { path: 'kitchen/substitutions', component: () => import('../views/kitchen/Substitutions.vue'), meta: { title: '替换餐执行', roles: ['KITCHEN'] } },
      // 前台
      { path: 'frontdesk/pickup', component: () => import('../views/frontdesk/Pickup.vue'), meta: { title: '接送核验', roles: ['FRONTDESK'] } },
      // 共用
      { path: 'events', component: () => import('../views/Events.vue'), meta: { title: '协同事件' } },
      { path: 'events/:id', component: () => import('../views/EventDetail.vue'), meta: { title: '事件详情' } },
      { path: 'timeline', component: () => import('../views/Timeline.vue'), meta: { title: '时间轴', roles: ['DIRECTOR', 'HEALTH', 'TEACHER', 'FRONTDESK'] } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  if (to.meta.public) {
    if (token) return '/dashboard'
    return true
  }
  if (!token) return '/login'
  if (to.meta.roles && user && !to.meta.roles.includes(user.role)) {
    return '/dashboard'
  }
  return true
})

export default router
