package com.fairing.fairplay.admin.etc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.fairing.fairplay.admin.entity.FunctionLevel;

public class Initialize_level {

    public List<FunctionLevel> getFunctionLevels() {

        String[][] methods = {
                { "getLogs", "100" },
                { "getList", "20" },
                { "cancelApplication", "1" },
                { "disableUser", "101" },
                { "getDetail", "21" },
                { "createBoothExperience", "2" },
                { "getUsers", "102" },
                { "updateStatus", "22" },
                { "getBoothExperiences", "3" },
                { "createBanner", "103" },
                { "updatePaymentStatus", "23" },
                { "getExperienceReservations", "4" },
                { "updateBanner", "104" },
                { "createEventDetail", "24" },
                { "updateStatus", "105" },
                { "checkEventPermission", "25" },
                { "updatePriority", "106" },
                { "updateEvent", "26" },
                { "listAll", "107" },
                { "updateEventDetail", "27" },
                { "getPendingApplications", "108" },
                { "createModificationRequest", "28" },
                { "processEventApplication", "109" },
                { "getPendingModificationRequest", "29" },
                { "createEvent", "110" },
                { "getEventVersions", "30" },
                { "softDeleteEvent", "111" },
                { "getEventVersion", "31" },
                { "deleteEvent", "112" },
                { "createVersionRestoreRequest", "32" },
                { "forcedDeleteEvent", "113" },
                { "compareVersions", "33" },
                { "getModificationRequests", "114" },
                { "getAllPayments", "34" },
                { "processModificationRequest", "115" },
                { "getReservations", "35" },
                { "requestPayment", "116" },
                { "getReservationAttendees", "36" },
                { "completePayment", "117" },
                { "downloadAttendeesExcel", "37" },
                { "approveRefund", "118" },
                { "getPageData", "38" },
                { "rejectRefund", "119" },
                { "getEventsByStatus", "39" },
                { "getAllRefunds", "120" },
                { "getEventDashboard", "40" },
                { "getPendingRefunds", "121" },
                { "getAggregatedPopularity", "41" },
                { "adminForceCheck", "122" },
                { "getSearchPopularity", "42" },
                { "getAggregatedPopularity", "123" },
                { "getReportPopularity", "43" },
                { "getMonthTrend", "124" },
                { "getHourlyStatistics", "44" },
                { "getDailyTrend", "125" },
                { "getHourlyStatisticsByDate", "45" },
                { "getSearchReservation", "126" },
                { "getSalesStatistics", "46" },
                { "getReportPopularity", "127" },
                { "createSchedule", "47" },
                { "getEventAdmin", "128" },
                { "getSchedules", "48" },
                { "getSchedule", "49" },
                { "updateSchedule", "50" },
                { "deleteSchedule", "51" },
                { "registerScheduleTicket", "52" },
                { "getScheduleTickets", "53" },
                { "createTicket", "54" },
                { "updateTicket", "55" },
                { "deleteTicket", "56" },
                { "updateEventAdminInfo", "57" }
        };

        List<FunctionLevel> functionLevels = new ArrayList<>();
        for (String[] method : methods) {
            String methodName = method[0];
            int number = Integer.parseInt(method[1]);

            BigInteger bigIntValue = BigInteger.ONE.shiftLeft(number).subtract(BigInteger.ONE);
            BigDecimal bigDecimalValue = new BigDecimal(bigIntValue);

            FunctionLevel functionLevel = new FunctionLevel();
            functionLevel.setFunctionName(methodName);
            functionLevel.setLevel(bigDecimalValue);
            functionLevels.add(functionLevel);
        }

        return functionLevels;
    }

}
