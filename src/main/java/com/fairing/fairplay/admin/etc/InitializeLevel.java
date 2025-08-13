package com.fairing.fairplay.admin.etc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.fairing.fairplay.admin.entity.FunctionLevel;

public class InitializeLevel {

    public List<FunctionLevel> getFunctionLevels() {

        String[][] methods = {
                // { "cancelApplication", "1" },
                // { "createBoothExperience", "2" },
                { "getBoothExperiences", "3", "부스 체험 목록 조회" },
                { "getExperienceReservations", "4", "예약자 목록 조회" },
                { "updateReservationStatus", "5", "예약 상태 변경" },
                { "getList", "20", "신청 목록 조회" },
                // { "getDetail", "21" },
                // { "updateStatus", "22" },
                // { "updatePaymentStatus", "23" },
                // { "createEventDetail", "24" },
                // { "checkEventPermission", "25" },
                // { "updateEvent", "26" },
                { "updateEventDetail", "27", "행사 상세 업데이트" },
                // { "createModificationRequest", "28" },
                // { "getPendingModificationRequest", "29" },
                // { "getEventVersions", "30" },
                // { "getEventVersion", "31" },
                // { "createVersionRestoreRequest", "32" },
                // { "compareVersions", "33" },
                { "getAllPayments", "34", "결제 전체 조회" },
                { "getReservations", "35", "전체 예약 조회" },
                { "getReservationAttendees", "36", "예약자 명단 조회" },
                // { "downloadAttendeesExcel", "37" },
                // { "getPageData", "38" },
                // { "getEventsByStatus", "39" },
                // { "getEventDashboard", "40" },
                // { "getEventAggregatedPopularity", "41" },
                // { "getSearchPopularity", "42" },
                // { "getEventReportPopularity", "43" },
                // { "getHourlyStatistics", "44" },
                // { "getHourlyStatisticsByDate", "45" },
                // { "getSalesStatistics", "46" },
                // { "createSchedule", "47" },
                // { "getSchedules", "48" },
                // { "getSchedule", "49" },
                // { "updateSchedule", "50" },
                // { "deleteSchedule", "51" },
                // { "registerScheduleTicket", "52" },
                // { "getScheduleTickets", "53" },
                // { "createTicket", "54" },
                // { "updateTicket", "55" },
                // { "deleteTicket", "56" },
                // { "updateEventAdminInfo", "57" },
                { "getLogs", "100", "로그 조회" },
                // { "disableUser", "101" },
                // { "getUsers", "102" },
                // { "createBanner", "103" },
                { "updateBanner", "104", "배너 수정" },
                // { "updateBannerStatus", "105" },
                // { "updatePriority", "106" },
                // { "listAll", "107" },
                // { "getPendingApplications", "108" },
                // { "processEventApplication", "109" },
                // { "createEvent", "110" },
                // { "softDeleteEvent", "111" },
                // { "deleteEvent", "112" },
                // { "forcedDeleteEvent", "113" },
                // { "getModificationRequests", "114" },
                // { "processModificationRequest", "115" },
                // { "requestPayment", "116" },
                // { "completePayment", "117" },
                // { "approveRefund", "118" },
                // { "rejectRefund", "119" },
                // { "getAllRefunds", "120" },
                // { "getPendingRefunds", "121" },
                // { "adminForceCheck", "122" },
                // { "getAggregatedPopularity", "123" },
                // { "getMonthTrend", "124" },
                // { "getDailyTrend", "125" },
                // { "getSearchReservation", "126" },
                // { "getReportPopularity", "127" },
                // { "getEventAdmin", "128" },
        };

        List<FunctionLevel> functionLevels = new ArrayList<>();
        for (String[] method : methods) {
            String methodName = method[0];
            int number = Integer.parseInt(method[1]);
            String methodNameKr = method[2];

            BigInteger bigIntValue = BigInteger.ONE.shiftLeft(number);
            BigDecimal bigDecimalValue = new BigDecimal(bigIntValue);

            FunctionLevel functionLevel = new FunctionLevel();
            functionLevel.setFunctionName(methodName);
            functionLevel.setLevel(bigDecimalValue);
            functionLevel.setFunctionNameKr(methodNameKr);
            functionLevels.add(functionLevel);
        }

        return functionLevels;
    }

}
