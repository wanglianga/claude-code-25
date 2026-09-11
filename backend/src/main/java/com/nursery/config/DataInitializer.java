package com.nursery.config;

import com.nursery.entity.*;
import com.nursery.repository.*;
import com.nursery.service.AlertService;
import com.nursery.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 首次启动时写入演示账号与演示业务数据（幂等：已有用户则跳过）。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepo;
    private final ClassroomRepository classroomRepo;
    private final ChildRepository childRepo;
    private final EmergencyContactRepository contactRepo;
    private final PickupAuthorizationRepository authRepo;
    private final EnrollmentApplicationRepository applicationRepo;
    private final ClassAssignmentRepository assignmentRepo;
    private final HealthAssessmentRepository assessmentRepo;
    private final MorningCheckRepository morningCheckRepo;
    private final CareRecordRepository careRecordRepo;
    private final MedicationRequestRepository medicationRepo;
    private final TempDelegationRepository delegationRepo;
    private final PickupMessageRepository pickupMessageRepo;
    private final PickupRecordRepository pickupRecordRepo;
    private final NurseryEventRepository eventRepo;
    private final EventMessageRepository messageRepo;
    private final PasswordEncoder passwordEncoder;
    private final EventService eventService;
    private final AlertService alertService;

    public DataInitializer(UserRepository userRepo, ClassroomRepository classroomRepo,
                           ChildRepository childRepo, EmergencyContactRepository contactRepo,
                           PickupAuthorizationRepository authRepo,
                           EnrollmentApplicationRepository applicationRepo,
                           ClassAssignmentRepository assignmentRepo,
                           HealthAssessmentRepository assessmentRepo,
                           MorningCheckRepository morningCheckRepo,
                           CareRecordRepository careRecordRepo,
                           MedicationRequestRepository medicationRepo,
                           TempDelegationRepository delegationRepo,
                           PickupMessageRepository pickupMessageRepo,
                           PickupRecordRepository pickupRecordRepo,
                           NurseryEventRepository eventRepo,
                           EventMessageRepository messageRepo,
                           PasswordEncoder passwordEncoder,
                           EventService eventService, AlertService alertService) {
        this.userRepo = userRepo;
        this.classroomRepo = classroomRepo;
        this.childRepo = childRepo;
        this.contactRepo = contactRepo;
        this.authRepo = authRepo;
        this.applicationRepo = applicationRepo;
        this.assignmentRepo = assignmentRepo;
        this.assessmentRepo = assessmentRepo;
        this.morningCheckRepo = morningCheckRepo;
        this.careRecordRepo = careRecordRepo;
        this.medicationRepo = medicationRepo;
        this.delegationRepo = delegationRepo;
        this.pickupMessageRepo = pickupMessageRepo;
        this.pickupRecordRepo = pickupRecordRepo;
        this.eventRepo = eventRepo;
        this.messageRepo = messageRepo;
        this.passwordEncoder = passwordEncoder;
        this.eventService = eventService;
        this.alertService = alertService;
    }

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) {
            log.info("已有数据，跳过演示数据初始化");
            return;
        }
        log.info("初始化演示账号与演示数据...");
        LocalDate today = LocalDate.now();

        // ---------- 用户 ----------
        User director = user("director", "王园长", User.Role.DIRECTOR, "13900000001");
        User health = user("health", "刘保健", User.Role.HEALTH, "13900000002");
        User teacher1 = user("teacher1", "陈老师", User.Role.TEACHER, "13900000003");
        User teacher2 = user("teacher2", "林老师", User.Role.TEACHER, "13900000004");
        User frontdesk = user("frontdesk", "赵前台", User.Role.FRONTDESK, "13900000005");
        User parent1 = user("parent1", "张家长", User.Role.PARENT, "13800000001");
        User parent2 = user("parent2", "李家长", User.Role.PARENT, "13800000002");
        User parent3 = user("parent3", "王家长", User.Role.PARENT, "13800000003");

        // ---------- 班级 ----------
        Classroom c1 = classroom("小芽班", 12, "1-2岁", "低龄照护班，重点生活照料");
        c1.getTeachers().add(teacher1);
        classroomRepo.save(c1);
        Classroom c2 = classroom("苗苗班", 15, "2-3岁", "习惯养成班");
        c2.getTeachers().add(teacher2);
        classroomRepo.save(c2);
        Classroom c3 = classroom("果果班", 18, "3-4岁", "入园衔接班");
        classroomRepo.save(c3);

        // ---------- 张乐乐：已入托（苗苗班），花生过敏 ----------
        Child lele = child(parent1, "张乐乐", "男", LocalDate.of(2023, 6, 15),
                "花生及花生制品过敏，曾出现荨麻疹",
                "按国家免疫规划完成接种，本季度流感疫苗已接种",
                "需安抚巾，入睡约20分钟",
                "如厕训练中，会表达便意",
                "氯雷他定糖浆（过敏时遵医嘱）",
                "花生过敏，需过敏餐，避免含花生食材");
        lele.setStatus(Child.Status.ENROLLED);
        lele.setClassroom(c2);
        lele.setAllergyMealRequired(true);
        childRepo.save(lele);
        contact(lele, "张建国", "父亲", "13800000001", 1);
        contact(lele, "王芳", "母亲", "13811110001", 2);
        authorization(lele, "张建国", "父亲", "13800000001", "110101199001011234");
        authorization(lele, "李秀兰", "奶奶", "13822223333", "110101196505152222");
        EnrollmentApplication leleApp = application(lele, "希望培养自理能力，就近入托",
                EnrollmentApplication.Status.COMPLETED, today.minusDays(20));
        assignment(leleApp, c2, director, "苗苗班师幼比1:7，有过敏儿童照护经验；满足家长就近期望", today.minusDays(19));
        assessment(leleApp, health, HealthAssessment.Result.ALLERGY_MEAL, true, false, null,
                "花生过敏明确，厨房已备案过敏餐", today.minusDays(18));

        // 喂药委托（今日在服）
        MedicationRequest mr = new MedicationRequest();
        mr.setChild(lele);
        mr.setMedicineName("氯雷他定糖浆");
        mr.setDosage("5ml");
        mr.setTimePlan("每日一次，午饭后");
        mr.setStartDate(today.minusDays(3));
        mr.setEndDate(today.plusDays(7));
        mr.setParentNote("近期过敏季，保健老师已知晓，药品已交陈老师处");
        medicationRepo.save(mr);

        // 今日临时委托：外婆来接
        TempDelegation delegation = new TempDelegation();
        delegation.setChild(lele);
        delegation.setDelegateName("王秀珍");
        delegation.setDelegatePhone("13833334444");
        delegation.setDelegateIdNumber("110101196603103333");
        delegation.setValidDate(today);
        delegation.setNote("爸妈出差，今天外婆接");
        delegationRepo.save(delegation);

        // 今日接送留言
        pickupMessage(lele, today, "今天外婆（王秀珍）来接，已提交临时委托，谢谢！");

        // 历史晨检：两次发热异常 + 昨日正常
        morningCheck(lele, today.minusDays(7), teacher2, "37.6", "正常", "低落", "食欲一般",
                "换洗衣物、安抚巾", true, MorningCheck.Result.ISOLATION, "额温偏高，建议隔离观察");
        morningCheck(lele, today.minusDays(3), teacher2, "37.8", "正常", "烦躁", "食欲差",
                "换洗衣物", true, MorningCheck.Result.PARENT_PICKUP, "发热，已通知家长接回就医");
        morningCheck(lele, today.minusDays(1), teacher2, "36.5", "正常", "愉快", "早餐吃完",
                "换洗衣物、安抚巾、氯雷他定", true, MorningCheck.Result.ENTER_CLASS, null);

        // 昨日照护记录
        careRecord(lele, today.minusDays(1), teacher2, CareRecord.CareType.NOON_CARE,
                "午餐过敏餐单独供应，进食良好", CareRecord.Severity.NORMAL, null);
        careRecord(lele, today.minusDays(1), teacher2, CareRecord.CareType.SLEEP,
                "午睡12:30-14:10，入睡顺利", CareRecord.Severity.NORMAL, null);
        careRecord(lele, today.minusDays(1), teacher2, CareRecord.CareType.TOILET,
                "自主表达便意2次，需协助整理衣物", CareRecord.Severity.NORMAL, null);

        // 昨日接送：奶奶证件核验放行
        pickupRecord(lele, today.minusDays(1), "李秀兰", "110101196505152222",
                PickupRecord.AuthType.AUTHORIZED, PickupRecord.VerifyMethod.ID_CARD,
                PickupRecord.Result.SUCCESS, null, frontdesk);

        // 历史发热事件（已解决）
        NurseryEvent fever1 = eventService.createEvent(NurseryEvent.EventType.FEVER, lele,
                "晨检发热 37.6℃（隔离观察）", "晨检体温偏高，隔离观察并通知家长。", teacher2);
        closeEvent(fever1, today.minusDays(7), "留观后体温恢复正常，家长知晓，未用药。");
        NurseryEvent fever2 = eventService.createEvent(NurseryEvent.EventType.FEVER, lele,
                "晨检发热 37.8℃（通知家长接回）", "晨检发热，通知家长接回就医。", teacher2);
        closeEvent(fever2, today.minusDays(3), "家长接回就医，诊断普通感冒，次日退烧，凭复课证明返园。");

        // ---------- 李糖糖：已入托（苗苗班），有抓咬事件史 ----------
        Child tangtang = child(parent2, "李糖糖", "女", LocalDate.of(2022, 11, 5),
                "无", "已完成全部规划疫苗", "午睡良好", "可自主如厕", "无", "无特殊照护需求");
        tangtang.setStatus(Child.Status.ENROLLED);
        tangtang.setClassroom(c2);
        childRepo.save(tangtang);
        contact(tangtang, "李强", "父亲", "13800000002", 1);
        authorization(tangtang, "李强", "父亲", "13800000002", "110101198812124444");
        EnrollmentApplication ttApp = application(tangtang, "希望多参加户外活动",
                EnrollmentApplication.Status.COMPLETED, today.minusDays(30));
        assignment(ttApp, c2, director, "按年龄段分入苗苗班", today.minusDays(29));
        assessment(ttApp, health, HealthAssessment.Result.PASS, false, false, null,
                "体检合格", today.minusDays(28));
        careRecord(tangtang, today.minusDays(5), teacher2, CareRecord.CareType.INJURY,
                "户外活动与同伴争抢玩具，手臂轻微抓痕，已消毒处理", CareRecord.Severity.ATTENTION,
                CareRecord.InjuryType.SCRATCH_BITE);
        NurseryEvent bite = eventService.createEvent(NurseryEvent.EventType.BITE_INCIDENT, tangtang,
                "班级抓咬事件（需关注）", "户外活动争抢玩具被抓伤手臂，已消毒处理。", teacher2);
        closeEvent(bite, today.minusDays(5), "双方家长已沟通，伤口痊愈无感染，班级增设玩具数量并调整分组。");
        message(bite, parent2, "孩子回家说了这件事，希望园里关注一下。");
        message(bite, teacher2, "已分开两位孩子活动区域，加强看护，伤口每日观察。");

        // ---------- 李朵朵：待分班（园长任务） ----------
        Child duoduo = child(parent2, "李朵朵", "女", LocalDate.of(2024, 1, 20),
                "无", "已完成周岁疫苗", "抱睡过渡中", "纸尿裤", "无",
                "分离焦虑较明显，需要渐进适应");
        duoduo.setStatus(Child.Status.ASSESSING);
        childRepo.save(duoduo);
        contact(duoduo, "李强", "父亲", "13800000002", 1);
        contact(duoduo, "周敏", "母亲", "13855556666", 2);
        authorization(duoduo, "李强", "父亲", "13800000002", "110101198812124444");
        authorization(duoduo, "周敏", "母亲", "13855556666", "110101199003035555");
        application(duoduo, "希望进入同龄班，先半天过渡再全天", EnrollmentApplication.Status.PENDING_CLASS,
                today.minusDays(1));

        // ---------- 王豆豆：已分班，待健康评估（保健老师任务） ----------
        Child doudou = child(parent3, "王豆豆", "男", LocalDate.of(2022, 9, 10),
                "牛奶轻度不耐受", "疫苗齐全", "午睡规律", "可自主如厕", "无",
                "牛奶不耐受，请留意餐后腹胀");
        doudou.setStatus(Child.Status.ASSIGNED);
        doudou.setClassroom(c3);
        childRepo.save(doudou);
        contact(doudou, "王军", "父亲", "13800000003", 1);
        authorization(doudou, "王军", "父亲", "13800000003", "110101198706066666");
        EnrollmentApplication ddApp = application(doudou, "希望多运动，牛奶不耐受请留意",
                EnrollmentApplication.Status.PENDING_HEALTH, today.minusDays(2));
        assignment(ddApp, c3, director, "果果班活动量大，符合家长期望；容量充足", today.minusDays(1));

        // ---------- 异常预警（乐乐近30天多次异常） ----------
        alertService.checkChild(lele);

        log.info("演示数据初始化完成。演示账号：director/health/teacher1/teacher2/frontdesk/parent1/parent2/parent3，密码均为 123456");
    }

    // ---------- 辅助方法 ----------

    private User user(String username, String name, User.Role role, String phone) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("123456"));
        u.setName(name);
        u.setRole(role);
        u.setPhone(phone);
        return userRepo.save(u);
    }

    private Classroom classroom(String name, int capacity, String ageRange, String description) {
        Classroom c = new Classroom();
        c.setName(name);
        c.setCapacity(capacity);
        c.setAgeRange(ageRange);
        c.setDescription(description);
        return classroomRepo.save(c);
    }

    private Child child(User parent, String name, String gender, LocalDate birthDate,
                        String allergy, String vaccination, String nap, String toilet,
                        String medications, String specialCare) {
        Child c = new Child();
        c.setParent(parent);
        c.setName(name);
        c.setGender(gender);
        c.setBirthDate(birthDate);
        c.setAllergyHistory(allergy);
        c.setVaccinationStatus(vaccination);
        c.setNapHabit(nap);
        c.setToiletAbility(toilet);
        c.setMedications(medications);
        c.setSpecialCareNeeds(specialCare);
        return c;
    }

    private void contact(Child child, String name, String relation, String phone, int priority) {
        EmergencyContact e = new EmergencyContact();
        e.setChild(child);
        e.setName(name);
        e.setRelation(relation);
        e.setPhone(phone);
        e.setPriority(priority);
        contactRepo.save(e);
    }

    private void authorization(Child child, String name, String relation, String phone, String idNumber) {
        PickupAuthorization a = new PickupAuthorization();
        a.setChild(child);
        a.setName(name);
        a.setRelation(relation);
        a.setPhone(phone);
        a.setIdNumber(idNumber);
        authRepo.save(a);
    }

    private EnrollmentApplication application(Child child, String expectations,
                                              EnrollmentApplication.Status status, LocalDate createdDate) {
        EnrollmentApplication app = new EnrollmentApplication();
        app.setChild(child);
        app.setParentExpectations(expectations);
        app.setStatus(status);
        app.setCreatedAt(createdDate.atTime(9, 0));
        app.setUpdatedAt(createdDate.atTime(9, 0));
        return applicationRepo.save(app);
    }

    private void assignment(EnrollmentApplication app, Classroom classroom, User director,
                            String note, LocalDate date) {
        ClassAssignment a = new ClassAssignment();
        a.setApplication(app);
        a.setClassroom(classroom);
        a.setAssignedBy(director);
        a.setNote(note);
        a.setCreatedAt(date.atTime(10, 0));
        assignmentRepo.save(a);
    }

    private void assessment(EnrollmentApplication app, User health, HealthAssessment.Result result,
                            boolean allergyMeal, boolean observation, String supplementRequest,
                            String note, LocalDate date) {
        HealthAssessment h = new HealthAssessment();
        h.setApplication(app);
        h.setAssessor(health);
        h.setResult(result);
        h.setAllergyMealRequired(allergyMeal);
        h.setObservationRequired(observation);
        h.setSupplementRequest(supplementRequest);
        h.setNote(note);
        h.setCreatedAt(date.atTime(11, 0));
        assessmentRepo.save(h);
    }

    private void morningCheck(Child child, LocalDate date, User teacher, String temperature,
                              String skin, String mood, String diet, String carriedItems,
                              Boolean medicationBrought, MorningCheck.Result result, String note) {
        MorningCheck mc = new MorningCheck();
        mc.setChild(child);
        mc.setCheckDate(date);
        mc.setTeacher(teacher);
        mc.setTemperature(new BigDecimal(temperature));
        mc.setSkinStatus(skin);
        mc.setMood(mood);
        mc.setDiet(diet);
        mc.setCarriedItems(carriedItems);
        mc.setMedicationBrought(medicationBrought);
        mc.setResult(result);
        mc.setNote(note);
        mc.setCreatedAt(date.atTime(8, 30));
        morningCheckRepo.save(mc);
    }

    private void careRecord(Child child, LocalDate date, User teacher, CareRecord.CareType type,
                            String detail, CareRecord.Severity severity, CareRecord.InjuryType injuryType) {
        CareRecord r = new CareRecord();
        r.setChild(child);
        r.setRecordDate(date);
        r.setType(type);
        r.setDetail(detail);
        r.setSeverity(severity);
        r.setInjuryType(injuryType);
        r.setRecordedBy(teacher);
        r.setCreatedAt(date.atTime(14, 0));
        careRecordRepo.save(r);
    }

    private void pickupMessage(Child child, LocalDate date, String content) {
        PickupMessage m = new PickupMessage();
        m.setChild(child);
        m.setMsgDate(date);
        m.setContent(content);
        m.setCreatedBy(child.getParent());
        pickupMessageRepo.save(m);
    }

    private void pickupRecord(Child child, LocalDate date, String personName, String idNumber,
                              PickupRecord.AuthType authType, PickupRecord.VerifyMethod method,
                              PickupRecord.Result result, String denyReason, User operator) {
        PickupRecord p = new PickupRecord();
        p.setChild(child);
        p.setPickupDate(date);
        p.setPickupPersonName(personName);
        p.setPickupPersonIdNumber(idNumber);
        p.setAuthType(authType);
        p.setVerifyMethod(method);
        p.setResult(result);
        p.setDenyReason(denyReason);
        p.setOperator(operator);
        p.setCreatedAt(date.atTime(17, 0));
        pickupRecordRepo.save(p);
    }

    private void message(NurseryEvent event, User sender, String content) {
        EventMessage m = new EventMessage();
        m.setEvent(event);
        m.setSender(sender);
        m.setContent(content);
        m.setCreatedAt(event.getCreatedAt().plusHours(2));
        messageRepo.save(m);
    }

    private void closeEvent(NurseryEvent event, LocalDate resolveDate, String resolution) {
        event.setStatus(NurseryEvent.Status.RESOLVED);
        event.setResolutionNote(resolution);
        event.setCreatedAt(resolveDate.atTime(9, 0));
        event.setUpdatedAt(resolveDate.atTime(18, 0));
        eventRepo.save(event);
    }
}
