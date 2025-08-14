package com.fairing.fairplay.core.etc;

import java.math.BigInteger;
import java.util.Arrays;

public enum FunctionLevelEnum {

    // 부스 관리자
    CANCEL_APPLICATION("cancelApplication", BigInteger.ONE.shiftLeft(1)),
    CREATE_BOOTH_EXPERIENCE("createBoothExperience", BigInteger.ONE.shiftLeft(2)),
    GET_BOOTH_EXPERIENCES("getBoothExperiences", BigInteger.ONE.shiftLeft(3)),
    GET_EXPERIENCE_RESERVATIONS("getExperienceReservations", BigInteger.ONE.shiftLeft(4)),
    UPDATE_RESERVATION_STATUS("updateReservationStatus", BigInteger.ONE.shiftLeft(5)),

    // 이벤트 관리자
    GET_LIST("getList", BigInteger.ONE.shiftLeft(20)),
    GET_DETAIL("getDetail", BigInteger.ONE.shiftLeft(21)),
    UPDATE_STATUS("updateStatus", BigInteger.ONE.shiftLeft(22)),
    UPDATE_PAYMENT_STATUS("updatePaymentStatus", BigInteger.ONE.shiftLeft(23)),
    CREATE_EVENT_DETAIL("createEventDetail", BigInteger.ONE.shiftLeft(24)),
    CHECK_EVENT_PERMISSION("checkEventPermission", BigInteger.ONE.shiftLeft(25)),
    UPDATE_EVENT("updateEvent", BigInteger.ONE.shiftLeft(26)),
    UPDATE_EVENT_DETAIL("updateEventDetail", BigInteger.ONE.shiftLeft(27)),
    CREATE_MODIFICATION_REQUEST("createModificationRequest", BigInteger.ONE.shiftLeft(28)),
    GET_PENDING_MODIFICATION_REQUEST("getPendingModificationRequest", BigInteger.ONE.shiftLeft(29)),
    GET_EVENT_VERSIONS("getEventVersions", BigInteger.ONE.shiftLeft(30)),
    GET_EVENT_VERSION("getEventVersion", BigInteger.ONE.shiftLeft(31)),
    CREATE_VERSION_RESTORE_REQUEST("createVersionRestoreRequest", BigInteger.ONE.shiftLeft(32)),
    COMPARE_VERSIONS("compareVersions", BigInteger.ONE.shiftLeft(33)),
    GET_ALL_PAYMENTS("getAllPayments", BigInteger.ONE.shiftLeft(34)),
    GET_RESERVATIONS("getReservations", BigInteger.ONE.shiftLeft(35)),
    GET_RESERVATION_ATTENDEES("getReservationAttendees", BigInteger.ONE.shiftLeft(36)),
    DOWNLOAD_ATTENDEES_EXCEL("downloadAttendeesExcel", BigInteger.ONE.shiftLeft(37)),
    GET_PAGE_DATA("getPageData", BigInteger.ONE.shiftLeft(38)),
    GET_EVENTS_BY_STATUS("getEventsByStatus", BigInteger.ONE.shiftLeft(39)),
    GET_EVENT_DASHBOARD("getEventDashboard", BigInteger.ONE.shiftLeft(40)),
    GET_EVENT_AGGREGATED_POPULARITY("getEventAggregatedPopularity", BigInteger.ONE.shiftLeft(41)),
    GET_SEARCH_POPULARITY("getSearchPopularity", BigInteger.ONE.shiftLeft(42)),
    GET_EVENT_REPORT_POPULARITY("getEventReportPopularity", BigInteger.ONE.shiftLeft(43)),
    GET_HOURLY_STATISTICS("getHourlyStatistics", BigInteger.ONE.shiftLeft(44)),
    GET_HOURLY_STATISTICS_BY_DATE("getHourlyStatisticsByDate", BigInteger.ONE.shiftLeft(45)),
    GET_SALES_STATISTICS("getSalesStatistics", BigInteger.ONE.shiftLeft(46)),
    CREATE_SCHEDULE("createSchedule", BigInteger.ONE.shiftLeft(47)),
    GET_SCHEDULES("getSchedules", BigInteger.ONE.shiftLeft(48)),
    GET_SCHEDULE("getSchedule", BigInteger.ONE.shiftLeft(49)),
    UPDATE_SCHEDULE("updateSchedule", BigInteger.ONE.shiftLeft(50)),
    DELETE_SCHEDULE("deleteSchedule", BigInteger.ONE.shiftLeft(51)),
    REGISTER_SCHEDULE_TICKET("registerScheduleTicket", BigInteger.ONE.shiftLeft(52)),
    GET_SCHEDULE_TICKETS("getScheduleTickets", BigInteger.ONE.shiftLeft(53)),
    CREATE_TICKET("createTicket", BigInteger.ONE.shiftLeft(54)),
    UPDATE_TICKET("updateTicket", BigInteger.ONE.shiftLeft(55)),
    DELETE_TICKET("deleteTicket", BigInteger.ONE.shiftLeft(56)),
    UPDATE_EVENT_ADMIN_INFO("updateEventAdminInfo", BigInteger.ONE.shiftLeft(57)),

    // 전체 관리자
    GET_LOGS("getLogs", BigInteger.ONE.shiftLeft(100)),
    DISABLE_USER("disableUser", BigInteger.ONE.shiftLeft(101)),
    GET_USERS("getUsers", BigInteger.ONE.shiftLeft(102)),
    CREATE_BANNER("createBanner", BigInteger.ONE.shiftLeft(103)),
    UPDATE_BANNER("updateBanner", BigInteger.ONE.shiftLeft(104)),
    UPDATE_BANNER_STATUS("updateBannerStatus", BigInteger.ONE.shiftLeft(105)),
    UPDATE_PRIORITY("updatePriority", BigInteger.ONE.shiftLeft(106)),
    LIST_ALL("listAll", BigInteger.ONE.shiftLeft(107)),
    GET_PENDING_APPLICATIONS("getPendingApplications", BigInteger.ONE.shiftLeft(108)),
    PROCESS_EVENT_APPLICATION("processEventApplication", BigInteger.ONE.shiftLeft(109)),
    CREATE_EVENT("createEvent", BigInteger.ONE.shiftLeft(110)),
    SOFT_DELETE_EVENT("softDeleteEvent", BigInteger.ONE.shiftLeft(111)),
    DELETE_EVENT("deleteEvent", BigInteger.ONE.shiftLeft(112)),
    FORCED_DELETE_EVENT("forcedDeleteEvent", BigInteger.ONE.shiftLeft(113)),
    GET_MODIFICATION_REQUESTS("getModificationRequests", BigInteger.ONE.shiftLeft(114)),
    PROCESS_MODIFICATION_REQUEST("processModificationRequest", BigInteger.ONE.shiftLeft(115)),
    REQUEST_PAYMENT("requestPayment", BigInteger.ONE.shiftLeft(116)),
    COMPLETE_PAYMENT("completePayment", BigInteger.ONE.shiftLeft(117)),
    APPROVE_REFUND("approveRefund", BigInteger.ONE.shiftLeft(118)),
    REJECT_REFUND("rejectRefund", BigInteger.ONE.shiftLeft(119)),
    GET_ALL_REFUNDS("getAllRefunds", BigInteger.ONE.shiftLeft(120)),
    GET_PENDING_REFUNDS("getPendingRefunds", BigInteger.ONE.shiftLeft(121)),
    ADMIN_FORCE_CHECK("adminForceCheck", BigInteger.ONE.shiftLeft(122)),
    GET_AGGREGATED_POPULARITY("getAggregatedPopularity", BigInteger.ONE.shiftLeft(123)),
    GET_MONTH_TREND("getMonthTrend", BigInteger.ONE.shiftLeft(124)),
    GET_DAILY_TREND("getDailyTrend", BigInteger.ONE.shiftLeft(125)),
    GET_SEARCH_RESERVATION("getSearchReservation", BigInteger.ONE.shiftLeft(126)),
    GET_REPORT_POPULARITY("getReportPopularity", BigInteger.ONE.shiftLeft(127)),
    GET_EVENT_ADMIN("getEventAdmin", BigInteger.ONE.shiftLeft(128));

    private final String functionName;
    private final BigInteger bit;

    FunctionLevelEnum(String functionName, BigInteger bit) {
        this.functionName = functionName;
        this.bit = bit;
    }

    public String getFunctionName() {
        return functionName;
    }

    public BigInteger getBit() {
        return bit;
    }

    public static FunctionLevelEnum fromFunctionName(String functionName) {
        return Arrays.stream(values())
                .filter(f -> f.getFunctionName().equals(functionName))
                .findFirst()
                .orElse(null);
    }
}
