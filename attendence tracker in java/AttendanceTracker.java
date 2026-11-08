import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ==============================================================================
 * PRAGATI ATTEND — SMART SEMESTER ATTENDANCE TRACKER
 * Primary Application Server & Interactive Web Portal
 *
 * Developed for: Pragati University Students
 * Technology: Java 17 LTS (Standalone Web Server - Zero External Dependencies)
 * Color Identity: 60-30-10 (60% Midnight Navy, 30% Pearl White, 10% Royal Gold)
 * ==============================================================================
 */
public class AttendanceTracker {

    // Core state
    private static final AttendanceCalculationService calcService = new AttendanceCalculationService();
    private static double targetPercentage = 75.0;
    private static final String studentId = "PRG2024CS042";
    private static final String studentName = "Aarav Sharma";
    private static final String courseBranch = "B.Tech Computer Science & Engineering";
    private static final String semesterName = "Semester 5 (Odd Semester 2026)";
    private static final LocalDate semesterStart = LocalDate.of(2026, 8, 3);
    private static final LocalDate semesterEnd = LocalDate.of(2026, 12, 18);
    private static final int totalSemesterInstructionalDays = 85;

    // Daily records
    public enum Status { PRESENT, ABSENT, HOLIDAY }

    public static class Record {
        public LocalDate date;
        public Status status;
        public String holidayName;
        public String notes;

        public Record(LocalDate date, Status status, String holidayName, String notes) {
            this.date = date;
            this.status = status;
            this.holidayName = holidayName;
            this.notes = notes;
        }
    }

    private static final Map<LocalDate, Record> records = new TreeMap<>();

    public static void main(String[] args) throws Exception {
        seedSampleData();

        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new DashboardHandler());
        server.createContext("/record", new RecordHandler());
        server.createContext("/calculator", new CalculatorHandler());
        server.createContext("/export/csv", new CsvExportHandler());
        server.createContext("/print", new PrintHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("==================================================================");
        System.out.println("  PRAGATI ATTEND — SMART SEMESTER ATTENDANCE TRACKER");
        System.out.println("  Pragati University Student Attendance Management System");
        System.out.println("==================================================================");
        System.out.println("  >> Application is running!");
        System.out.println("  >> Open your browser and visit: http://localhost:" + port);
        System.out.println("==================================================================");
    }

    private static void seedSampleData() {
        // Pre-configure initial university semester records
        records.put(LocalDate.of(2026, 8, 3), new Record(LocalDate.of(2026, 8, 3), Status.PRESENT, null, "First day of semester"));
        records.put(LocalDate.of(2026, 8, 4), new Record(LocalDate.of(2026, 8, 4), Status.PRESENT, null, "Regular classes"));
        records.put(LocalDate.of(2026, 8, 5), new Record(LocalDate.of(2026, 8, 5), Status.PRESENT, null, "Operating Systems Lab"));
        records.put(LocalDate.of(2026, 8, 6), new Record(LocalDate.of(2026, 8, 6), Status.ABSENT, null, "Medical leave"));
        records.put(LocalDate.of(2026, 8, 7), new Record(LocalDate.of(2026, 8, 7), Status.PRESENT, null, "Regular classes"));
        records.put(LocalDate.of(2026, 8, 15), new Record(LocalDate.of(2026, 8, 15), Status.HOLIDAY, "Independence Day", "National Holiday"));
        records.put(LocalDate.of(2026, 9, 14), new Record(LocalDate.of(2026, 9, 14), Status.HOLIDAY, "Vinayaka Chavithi", "Festival Break"));
        records.put(LocalDate.of(2026, 10, 2), new Record(LocalDate.of(2026, 10, 2), Status.HOLIDAY, "Gandhi Jayanti", "Gazetted Holiday"));
        records.put(LocalDate.of(2026, 10, 20), new Record(LocalDate.of(2026, 10, 20), Status.HOLIDAY, "Dussehra", "Festival Holiday"));
        records.put(LocalDate.of(2026, 10, 21), new Record(LocalDate.of(2026, 10, 21), Status.PRESENT, null, "DBMS class"));
        records.put(LocalDate.of(2026, 10, 22), new Record(LocalDate.of(2026, 10, 22), Status.PRESENT, null, "Computer Networks"));
        records.put(LocalDate.of(2026, 10, 23), new Record(LocalDate.of(2026, 10, 23), Status.PRESENT, null, "Software Engineering"));
        records.put(LocalDate.of(2026, 10, 26), new Record(LocalDate.of(2026, 10, 26), Status.PRESENT, null, "Machine Learning"));
        records.put(LocalDate.of(2026, 10, 27), new Record(LocalDate.of(2026, 10, 27), Status.ABSENT, null, "Family function"));
        records.put(LocalDate.of(2026, 10, 28), new Record(LocalDate.of(2026, 10, 28), Status.PRESENT, null, "Full day regular"));

        // Fill additional instructional dates to simulate 33 attended and 9 missed
        for (int i = 1; i <= 26; i++) {
            LocalDate d = LocalDate.of(2026, 8, 10).plusDays(i * 2);
            if (!records.containsKey(d)) {
                if (i % 4 == 0) {
                    records.put(d, new Record(d, Status.ABSENT, null, "Missed scheduled session"));
                } else {
                    records.put(d, new Record(d, Status.PRESENT, null, "Attended regular session"));
                }
            }
        }
    }

    // Handlers
    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            long present = records.values().stream().filter(r -> r.status == Status.PRESENT).count();
            long absent = records.values().stream().filter(r -> r.status == Status.ABSENT).count();
            long holidays = records.values().stream().filter(r -> r.status == Status.HOLIDAY).count();
            long held = present + absent;

            Double pct = calcService.calculatePercentage(present, absent);
            long needed = calcService.calculateClassesNeededToReachTarget(present, absent, targetPercentage);
            long safeBuffer = calcService.calculatePermittedFutureAbsences(present, absent, targetPercentage);
            long remaining = Math.max(0, totalSemesterInstructionalDays - held);
            Double maxPossible = calcService.calculateMaxPossiblePercentage(present, absent, remaining);
            String status = calcService.determineThresholdStatus(pct, targetPercentage);

            String html = renderPage("Dashboard", renderDashboardContent(present, absent, holidays, held, pct, needed, safeBuffer, remaining, maxPossible, status));
            sendHtml(ex, html);
        }
    }

    static class CalculatorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String query = ex.getRequestURI().getQuery();
            if (query != null && query.contains("target=")) {
                try {
                    String val = query.split("target=")[1].split("&")[0];
                    targetPercentage = Double.parseDouble(val);
                } catch (Exception ignored) {}
            }
            ex.getResponseHeaders().set("Location", "/");
            ex.sendResponseHeaders(302, -1);
        }
    }

    static class RecordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                InputStreamReader isr = new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String body = br.readLine();
                Map<String, String> params = parseForm(body);

                try {
                    LocalDate date = LocalDate.parse(params.get("date"));
                    Status st = Status.valueOf(params.get("status"));
                    String hol = params.get("holidayName");
                    String note = params.get("notes");
                    records.put(date, new Record(date, st, hol, note));
                } catch (Exception ignored) {}
            }
            ex.getResponseHeaders().set("Location", "/");
            ex.sendResponseHeaders(302, -1);
        }
    }

    static class CsvExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            StringBuilder csv = new StringBuilder();
            csv.append("# PRAGATI UNIVERSITY - OFFICIAL ATTENDANCE AUDIT\n");
            csv.append("# Student: ").append(studentName).append(" (").append(studentId).append(")\n");
            csv.append("# Date,Day,Status,Holiday_Name,Notes\n");
            for (Record r : records.values()) {
                csv.append(r.date).append(",")
                   .append(r.date.getDayOfWeek()).append(",")
                   .append(r.status).append(",")
                   .append(r.holidayName != null ? r.holidayName : "").append(",")
                   .append(r.notes != null ? r.notes.replace(",", ";") : "").append("\n");
            }

            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
            ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"Pragati_Attendance_" + studentId + ".csv\"");
            ex.sendResponseHeaders(200, bytes.length);
            OutputStream os = ex.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class PrintHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            long present = records.values().stream().filter(r -> r.status == Status.PRESENT).count();
            long absent = records.values().stream().filter(r -> r.status == Status.ABSENT).count();
            Double pct = calcService.calculatePercentage(present, absent);

            StringBuilder sb = new StringBuilder();
            sb.append("<div style='text-align:center;border-bottom:2px solid #101A2C;padding-bottom:15px;margin-bottom:20px;'>");
            sb.append("<h1 style='color:#101A2C;margin:0;font-size:26px;'>PRAGATI UNIVERSITY</h1>");
            sb.append("<p style='color:#D4AF37;font-weight:bold;margin:4px 0;'>Pragati Attend — Official Semester Attendance Report</p>");
            sb.append("<p style='font-size:13px;color:#666;'>Student: <b>").append(studentName).append("</b> (").append(studentId).append(") &bull; ").append(courseBranch).append("</p>");
            sb.append("<p style='font-size:14px;'>Attendance Standing: <b style='color:#101A2C;font-size:18px;'>").append(calcService.formatPercentage(pct)).append("</b> (Target: ").append(targetPercentage).append("%)</p>");
            sb.append("</div>");

            sb.append("<table style='width:100%;border-collapse:collapse;font-size:12px;'>");
            sb.append("<tr style='background:#101A2C;color:#fff;'>")
              .append("<th style='padding:8px;border:1px solid #ddd;'>Date</th>")
              .append("<th style='padding:8px;border:1px solid #ddd;'>Day</th>")
              .append("<th style='padding:8px;border:1px solid #ddd;'>Status</th>")
              .append("<th style='padding:8px;border:1px solid #ddd;'>Holiday / Reason</th>")
              .append("<th style='padding:8px;border:1px solid #ddd;'>Notes</th></tr>");

            for (Record r : records.values()) {
                String color = r.status == Status.PRESENT ? "#2E8B57" : (r.status == Status.ABSENT ? "#D64545" : "#D4AF37");
                sb.append("<tr>")
                  .append("<td style='padding:6px;border:1px solid #ddd;'><b>").append(r.date).append("</b></td>")
                  .append("<td style='padding:6px;border:1px solid #ddd;'>").append(r.date.getDayOfWeek()).append("</td>")
                  .append("<td style='padding:6px;border:1px solid #ddd;color:").append(color).append(";font-weight:bold;'>").append(r.status).append("</td>")
                  .append("<td style='padding:6px;border:1px solid #ddd;'>").append(r.holidayName != null ? r.holidayName : "-").append("</td>")
                  .append("<td style='padding:6px;border:1px solid #ddd;'>").append(r.notes != null ? r.notes : "-").append("</td></tr>");
            }
            sb.append("</table>");
            sb.append("<div style='margin-top:50px;display:flex;justify-content:space-between;text-align:center;font-size:12px;'>");
            sb.append("<div style='border-top:1px solid #000;width:180px;padding-top:5px;'>Student Signature</div>");
            sb.append("<div style='border-top:1px solid #000;width:180px;padding-top:5px;'>Faculty Advisor</div>");
            sb.append("<div style='border-top:1px solid #000;width:180px;padding-top:5px;'>Head of Department</div></div>");
            sb.append("<script>window.print();</script>");

            String page = "<!DOCTYPE html><html><head><title>Print Report - Pragati Attend</title><style>body{font-family:sans-serif;padding:30px;}</style></head><body>" + sb + "</body></html>";
            sendHtml(ex, page);
        }
    }

    private static String renderDashboardContent(long present, long absent, long holidays, long held,
                                                 Double pct, long needed, long safeBuffer, long remaining,
                                                 Double maxPossible, String status) {
        StringBuilder sb = new StringBuilder();

        // Hero Banner
        sb.append("<div style='background:linear-gradient(135deg,#101A2C 0%,#1A2740 100%);color:#F8F7F2;border-radius:12px;padding:24px;margin-bottom:24px;border-left:6px solid #D4AF37;'>");
        sb.append("<div style='display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:16px;'>");
        sb.append("<div>");
        sb.append("<div style='font-size:12px;color:#D4AF37;font-weight:bold;text-transform:uppercase;'>Pragati University &bull; ").append(courseBranch).append("</div>");
        sb.append("<h1 style='margin:6px 0;font-size:28px;'>Welcome, ").append(studentName).append(" <span style='font-size:16px;color:#8B95A5;'>( ").append(studentId).append(" )</span></h1>");
        sb.append("<p style='margin:4px 0;color:#CBD5E1;font-size:14px;'>").append(semesterName).append(" &bull; Start: ").append(semesterStart).append(" &bull; End: ").append(semesterEnd).append("</p>");
        sb.append("<div style='margin-top:10px;'>");
        String badgeColor = status.equals("SAFE") || status.equals("PERFECT ATTENDANCE") ? "#2E8B57" : (status.equals("AT RISK") ? "#E0A526" : "#D64545");
        sb.append("<span style='background:").append(badgeColor).append(";padding:4px 10px;border-radius:20px;font-size:12px;font-weight:bold;'>").append(status).append("</span>");
        sb.append("<span style='background:rgba(212,175,55,0.2);color:#D4AF37;padding:4px 10px;border-radius:20px;font-size:12px;font-weight:bold;margin-left:8px;'>Target Goal: ").append(targetPercentage).append("%</span>");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("<div style='text-align:right;'>");
        sb.append("<div style='font-size:12px;color:#8B95A5;font-weight:bold;'>CURRENT ATTENDANCE</div>");
        sb.append("<div style='font-size:48px;font-weight:900;color:#D4AF37;'>").append(calcService.formatPercentage(pct)).append("</div>");
        sb.append("<div style='font-size:13px;color:#CBD5E1;'>").append(present).append(" Attended &bull; ").append(absent).append(" Missed</div>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("</div>");

        // Stat Cards (7 Metrics)
        sb.append("<div style='display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:16px;margin-bottom:24px;'>");
        sb.append(metricCard("Classes Held", String.valueOf(held), "Excludes holidays", "#101A2C"));
        sb.append(metricCard("Classes Attended", String.valueOf(present), "Present marks", "#2E8B57"));
        sb.append(metricCard("Classes Missed", String.valueOf(absent), "Absences", "#D64545"));
        sb.append(metricCard("Holidays", String.valueOf(holidays), "Non-instructional", "#E0A526"));
        sb.append(metricCard("Classes Needed", (needed == -1 ? "Impossible" : String.valueOf(needed)), "To reach " + targetPercentage + "%", needed > 0 ? "#D64545" : "#2E8B57"));
        sb.append(metricCard("Safe Absence Buffer", String.valueOf(safeBuffer), "Can miss safely", "#D4AF37"));
        sb.append(metricCard("Remaining Classes", String.valueOf(remaining), "Until semester exams", "#101A2C"));
        sb.append("</div>");

        // Target Preset Selector Bar
        sb.append("<div style='background:#FFFFFF;border:1px solid #E2DFD6;border-radius:10px;padding:18px;margin-bottom:24px;'>");
        sb.append("<div style='font-weight:bold;font-size:15px;color:#101A2C;margin-bottom:10px;'>Simulate Attendance Target:</div>");
        sb.append("<div style='display:flex;gap:10px;flex-wrap:wrap;'>");
        for (double t : new double[]{65.0, 70.0, 75.0, 80.0, 85.0, 90.0, 95.0, 100.0}) {
            String active = (targetPercentage == t) ? "background:#D4AF37;color:#101A2C;font-weight:bold;" : "background:#F8F7F2;color:#101A2C;";
            sb.append("<a href='/calculator?target=").append(t).append("' style='padding:6px 14px;border-radius:20px;text-decoration:none;font-size:13px;border:1px solid #D4AF37;").append(active).append("'>")
              .append((int)t).append("%</a>");
        }
        sb.append("</div>");
        sb.append("</div>");

        // What-If Matrix & Quick Add Form
        sb.append("<div style='display:grid;grid-template-columns:2fr 1fr;gap:20px;margin-bottom:24px;'>");

        // Left: Prediction Table
        sb.append("<div style='background:#FFFFFF;border:1px solid #E2DFD6;border-radius:10px;padding:18px;'>");
        sb.append("<div style='font-weight:bold;font-size:16px;color:#101A2C;margin-bottom:12px;'>What-If Attendance Scenarios</div>");
        sb.append("<table style='width:100%;border-collapse:collapse;font-size:13px;'>");
        sb.append("<tr style='border-bottom:2px solid #E2DFD6;text-align:left;color:#64748B;'>")
          .append("<th style='padding:8px;'>Scenario</th><th>Projected %</th><th>Result</th></tr>");

        double missOne = ((double) present / (held + 1)) * 100.0;
        double next5 = ((double)(present + 5) / (held + 5)) * 100.0;
        double next10 = ((double)(present + 10) / (held + 10)) * 100.0;

        sb.append("<tr style='border-bottom:1px solid #f0f0f0;'><td style='padding:8px;'><b>If you miss 1 more class</b></td><td style='color:#D64545;font-weight:bold;'>").append(String.format(Locale.US, "%.2f%%", missOne)).append("</td><td>").append(missOne >= targetPercentage ? "<span style='color:#2E8B57;'>Still Safe</span>" : "<span style='color:#D64545;'>Below Target!</span>").append("</td></tr>");
        sb.append("<tr style='border-bottom:1px solid #f0f0f0;'><td style='padding:8px;'><b>If you attend next 5 classes</b></td><td style='color:#2E8B57;font-weight:bold;'>").append(String.format(Locale.US, "%.2f%%", next5)).append("</td><td><span style='color:#2E8B57;'>Gains +Buffer</span></td></tr>");
        sb.append("<tr style='border-bottom:1px solid #f0f0f0;'><td style='padding:8px;'><b>If you attend next 10 classes</b></td><td style='color:#2E8B57;font-weight:bold;'>").append(String.format(Locale.US, "%.2f%%", next10)).append("</td><td><span style='color:#2E8B57;'>Strong Buffer</span></td></tr>");
        sb.append("<tr><td style='padding:8px;'><b>Semester Max (100% future attendance)</b></td><td style='color:#D4AF37;font-weight:bold;'>").append(calcService.formatPercentage(maxPossible)).append("</td><td><span style='color:#D4AF37;'>Ceiling</span></td></tr>");
        sb.append("</table>");
        sb.append("</div>");

        // Right: Mark Attendance Form
        sb.append("<div style='background:#FFFFFF;border:1px solid #E2DFD6;border-radius:10px;padding:18px;'>");
        sb.append("<div style='font-weight:bold;font-size:16px;color:#101A2C;margin-bottom:12px;'>Mark Attendance Entry</div>");
        sb.append("<form action='/record' method='POST'>");
        sb.append("<label style='font-size:12px;font-weight:bold;display:block;margin-bottom:4px;'>Date</label>");
        sb.append("<input type='date' name='date' value='").append(LocalDate.now()).append("' style='width:100%;padding:8px;border:1px solid #ccc;border-radius:6px;margin-bottom:10px;' required>");
        sb.append("<label style='font-size:12px;font-weight:bold;display:block;margin-bottom:4px;'>Status</label>");
        sb.append("<select name='status' style='width:100%;padding:8px;border:1px solid #ccc;border-radius:6px;margin-bottom:10px;'>");
        sb.append("<option value='PRESENT'>Present (Attended)</option>");
        sb.append("<option value='ABSENT'>Absent (Missed)</option>");
        sb.append("<option value='HOLIDAY'>Holiday (College/Gazetted)</option>");
        sb.append("</select>");
        sb.append("<label style='font-size:12px;font-weight:bold;display:block;margin-bottom:4px;'>Holiday / Subject Note</label>");
        sb.append("<input type='text' name='notes' placeholder='e.g. Lab attended / Medical' style='width:100%;padding:8px;border:1px solid #ccc;border-radius:6px;margin-bottom:14px;'>");
        sb.append("<button type='submit' style='width:100%;background:#D4AF37;color:#101A2C;border:none;padding:10px;border-radius:6px;font-weight:bold;cursor:pointer;'>Save Attendance</button>");
        sb.append("</form>");
        sb.append("</div>");

        sb.append("</div>");

        // Daily Attendance Records Table
        sb.append("<div style='background:#FFFFFF;border:1px solid #E2DFD6;border-radius:10px;padding:18px;'>");
        sb.append("<div style='display:flex;justify-content:space-between;align-items:center;margin-bottom:14px;'>");
        sb.append("<div style='font-weight:bold;font-size:16px;color:#101A2C;'>Daily Semester Attendance Log (").append(records.size()).append(" Entries)</div>");
        sb.append("<div>");
        sb.append("<a href='/export/csv' style='background:#101A2C;color:#fff;padding:6px 12px;border-radius:6px;text-decoration:none;font-size:12px;margin-right:8px;'>Download CSV</a>");
        sb.append("<a href='/print' target='_blank' style='background:#D4AF37;color:#101A2C;padding:6px 12px;border-radius:6px;text-decoration:none;font-size:12px;font-weight:bold;'>Print Report</a>");
        sb.append("</div>");
        sb.append("</div>");

        sb.append("<table style='width:100%;border-collapse:collapse;font-size:13px;'>");
        sb.append("<tr style='background:#FAF9F6;border-bottom:2px solid #E2DFD6;text-align:left;color:#101A2C;'>")
          .append("<th style='padding:8px;'>Date</th><th>Day</th><th>Status</th><th>Holiday / Reason</th><th>Notes</th></tr>");

        for (Record r : records.values()) {
            String pill = r.status == Status.PRESENT ? "<span style='background:rgba(46,139,87,0.15);color:#2E8B57;padding:3px 8px;border-radius:4px;font-weight:bold;'>PRESENT</span>" :
                         (r.status == Status.ABSENT ? "<span style='background:rgba(214,69,69,0.15);color:#D64545;padding:3px 8px;border-radius:4px;font-weight:bold;'>ABSENT</span>" :
                         "<span style='background:rgba(212,175,55,0.15);color:#D4AF37;padding:3px 8px;border-radius:4px;font-weight:bold;'>HOLIDAY</span>");

            sb.append("<tr style='border-bottom:1px solid #F1EFEA;'>")
              .append("<td style='padding:8px;'><b>").append(r.date).append("</b></td>")
              .append("<td>").append(r.date.getDayOfWeek()).append("</td>")
              .append("<td>").append(pill).append("</td>")
              .append("<td>").append(r.holidayName != null ? r.holidayName : "-").append("</td>")
              .append("<td>").append(r.notes != null ? r.notes : "-").append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("</div>");

        return sb.toString();
    }

    private static String metricCard(String title, String val, String sub, String borderColor) {
        return "<div style='background:#FFFFFF;border:1px solid #E2DFD6;border-left:4px solid " + borderColor + ";border-radius:8px;padding:14px;box-shadow:0 1px 3px rgba(0,0,0,0.05);'>" +
               "<div style='font-size:11px;color:#8B95A5;text-transform:uppercase;font-weight:bold;'>" + title + "</div>" +
               "<div style='font-size:26px;font-weight:800;color:#101A2C;margin:4px 0;'>" + val + "</div>" +
               "<div style='font-size:12px;color:#64748B;'>" + sub + "</div>" +
               "</div>";
    }

    private static String renderPage(String title, String content) {
        return "<!DOCTYPE html>" +
               "<html><head><meta charset='UTF-8'><title>" + title + " — Pragati Attend</title>" +
               "<meta name='viewport' content='width=device-width,initial-scale=1.0'>" +
               "<style>" +
               "body{margin:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#F8F7F2;color:#101A2C;}" +
               ".header{background:#101A2C;color:#F8F7F2;padding:12px 24px;display:flex;justify-content:space-between;align-items:center;}" +
               ".brand{font-size:18px;font-weight:bold;color:#F8F7F2;display:flex;align-items:center;gap:8px;}" +
               ".brand-badge{background:#D4AF37;color:#101A2C;padding:4px 8px;border-radius:4px;font-size:12px;font-weight:bold;}" +
               ".content{max-width:1200px;margin:24px auto;padding:0 20px;}" +
               "</style></head><body>" +
               "<div class='header'>" +
               "<div class='brand'><span class='brand-badge'>PRAGATI</span> Pragati Attend</div>" +
               "<div style='font-size:13px;color:#CBD5E1;'>Pragati University Attendance Portal</div>" +
               "</div>" +
               "<div class='content'>" + content + "</div>" +
               "</body></html>";
    }

    private static void sendHtml(HttpExchange ex, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static Map<String, String> parseForm(String query) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name()), URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name()));
            }
        }
        return map;
    }
}
