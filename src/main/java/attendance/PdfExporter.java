package attendance;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class PdfExporter {

    private static final String REPORTS_DIR = "data/reports";

    public PdfExporter() {
        try {
            Files.createDirectories(Paths.get(REPORTS_DIR));
        } catch (IOException e) {
            System.out.println("Could not create reports folder: " + e.getMessage());
        }
    }

    public Path exportToHtml(ArrayList<AttendanceRecord> records, String subject) throws IOException {

        int presentCount = 0;
        for (AttendanceRecord r : records) {
            if (r.getStatus().equals("PRESENT")) presentCount++;
        }
        int absentCount = records.size() - presentCount;
        double rate = records.isEmpty() ? 0 : (presentCount * 100.0 / records.size());

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName  = "attendance_report_" + timestamp + ".html";
        Path   outputPath = Paths.get(REPORTS_DIR, fileName);

        String html = buildHtml(records, subject, presentCount, absentCount, rate);
        Files.writeString(outputPath, html);
        return outputPath.toAbsolutePath();
    }

    private String buildHtml(ArrayList<AttendanceRecord> records, String subject,
                              int presentCount, int absentCount, double rate) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'/>");
        html.append("<title>Attendance Report</title>");
        html.append("<style>");
        html.append("body{font-family:Arial,sans-serif;background:#f8fafc;color:#1e293b;margin:0;}");
        html.append(".page{max-width:900px;margin:40px auto;background:#fff;border-radius:12px;");
        html.append("box-shadow:0 4px 24px rgba(0,0,0,.08);overflow:hidden;}");
        html.append(".header{background:linear-gradient(135deg,#0f3460,#0ea5e9);color:#fff;padding:32px 40px;}");
        html.append(".header h1{font-size:24px;margin-bottom:4px;}");
        html.append(".header p{font-size:13px;opacity:.8;}");
        html.append(".stats{display:flex;border-bottom:1px solid #e2e8f0;}");
        html.append(".stat{flex:1;text-align:center;padding:20px;border-right:1px solid #e2e8f0;}");
        html.append(".stat:last-child{border-right:none;}");
        html.append(".stat .val{font-size:32px;font-weight:800;}");
        html.append(".stat .lbl{font-size:12px;color:#64748b;text-transform:uppercase;}");
        html.append(".green{color:#16a34a;}.red{color:#dc2626;}.blue{color:#0ea5e9;}");
        html.append(".content{padding:32px 40px;}");
        html.append("table{width:100%;border-collapse:collapse;}");
        html.append("th{text-align:left;padding:10px 14px;font-size:12px;background:#f1f5f9;");
        html.append("text-transform:uppercase;border-bottom:2px solid #e2e8f0;}");
        html.append("td{padding:10px 14px;font-size:14px;border-bottom:1px solid #f1f5f9;}");
        html.append(".badge{display:inline-block;padding:2px 10px;border-radius:999px;font-size:12px;font-weight:600;}");
        html.append(".p-badge{background:#dcfce7;color:#16a34a;}");
        html.append(".a-badge{background:#fee2e2;color:#dc2626;}");
        html.append(".footer{background:#f8fafc;border-top:1px solid #e2e8f0;padding:14px 40px;");
        html.append("font-size:12px;color:#94a3b8;display:flex;justify-content:space-between;}");
        html.append("</style></head><body><div class='page'>");

        html.append("<div class='header'><h1>Attendance Report</h1>");
        html.append("<p>COMP1402 - Object Oriented Programming</p>");
        html.append("<p>Subject: ").append(subject).append("</p></div>");

        html.append("<div class='stats'>");
        html.append("<div class='stat'><div class='val'>").append(records.size()).append("</div><div class='lbl'>Total</div></div>");
        html.append("<div class='stat'><div class='val green'>").append(presentCount).append("</div><div class='lbl'>Present</div></div>");
        html.append("<div class='stat'><div class='val red'>").append(absentCount).append("</div><div class='lbl'>Absent</div></div>");
        html.append(String.format("<div class='stat'><div class='val blue'>%.1f%%</div><div class='lbl'>Rate</div></div>", rate));
        html.append("</div>");

        html.append("<div class='content'><table><thead><tr>");
        html.append("<th>#</th><th>Name</th><th>Student ID</th><th>Status</th><th>Timestamp</th>");
        html.append("</tr></thead><tbody>");

        for (int i = 0; i < records.size(); i++) {
            AttendanceRecord r = records.get(i);
            String badge = r.getStatus().equals("PRESENT") ? "p-badge" : "a-badge";
            html.append("<tr>");
            html.append("<td>").append(i + 1).append("</td>");
            html.append("<td>").append(r.getStudentName()).append("</td>");
            html.append("<td>").append(r.getStudentId()).append("</td>");
            html.append("<td><span class='badge ").append(badge).append("'>")
                .append(r.getStatus()).append("</span></td>");
            html.append("<td>").append(r.getFormattedTimestamp()).append("</td>");
            html.append("</tr>");
        }

        html.append("</tbody></table></div>");
        html.append("<div class='footer'><span>Smart Attendance System - COMP1402</span>");
        html.append("<span>Open in browser → Ctrl+P → Save as PDF</span></div>");
        html.append("</div></body></html>");

        return html.toString();
    }
}
