package attendance;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exports attendance records to a PDF-ready HTML file,
 * then converts it to PDF using the system print dialog OR
 * writes a styled HTML report the user can print-to-PDF.
 *
 * Uses zero external libraries — pure Java file I/O.
 * The generated HTML is professional and print-ready.
 */
public class PdfExporter {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String REPORTS_DIR = "data/reports";

    public PdfExporter() {
        try {
            Files.createDirectories(Paths.get(REPORTS_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Cannot create reports directory: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a styled HTML attendance report.
     * The user can open it in a browser and Ctrl+P → Save as PDF.
     *
     * @param records  list of attendance records to include
     * @param subject  subject/class filter label (or "All Subjects")
     * @return path to the generated HTML file
     */
    public Path exportToHtml(List<AttendanceRecord> records, String subject) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName  = "attendance_report_" + timestamp + ".html";
        Path   outputPath = Paths.get(REPORTS_DIR, fileName);

        long presentCount = records.stream().filter(r -> "PRESENT".equals(r.getStatus())).count();
        long absentCount  = records.size() - presentCount;
        double rate = records.isEmpty() ? 0 : (presentCount * 100.0 / records.size());

        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8"/>
                <title>Attendance Report</title>
                <style>
                  * { margin: 0; padding: 0; box-sizing: border-box; }
                  body { font-family: 'Segoe UI', Arial, sans-serif; background: #f8fafc; color: #1e293b; }
                  .page { max-width: 900px; margin: 40px auto; background: white;
                          border-radius: 12px; box-shadow: 0 4px 24px rgba(0,0,0,0.08);
                          overflow: hidden; }
                  .header { background: linear-gradient(135deg, #0f3460 0%, #0ea5e9 100%);
                             color: white; padding: 32px 40px; }
                  .header h1 { font-size: 26px; font-weight: 700; margin-bottom: 4px; }
                  .header p  { font-size: 13px; opacity: 0.8; }
                  .meta { display: flex; gap: 12px; margin-top: 20px; flex-wrap: wrap; }
                  .meta-item { background: rgba(255,255,255,0.15); border-radius: 8px;
                               padding: 8px 16px; font-size: 13px; }
                  .stats { display: flex; gap: 0; border-bottom: 1px solid #e2e8f0; }
                  .stat { flex: 1; text-align: center; padding: 24px 16px; border-right: 1px solid #e2e8f0; }
                  .stat:last-child { border-right: none; }
                  .stat .value { font-size: 36px; font-weight: 800; }
                  .stat .label { font-size: 12px; color: #64748b; margin-top: 4px; text-transform: uppercase; letter-spacing: 0.05em; }
                  .present .value { color: #16a34a; }
                  .absent  .value { color: #dc2626; }
                  .rate    .value { color: #0ea5e9; }
                  .total   .value { color: #1e293b; }
                  .content { padding: 32px 40px; }
                  table { width: 100%; border-collapse: collapse; margin-top: 8px; }
                  thead tr { background: #f1f5f9; }
                  th { text-align: left; padding: 12px 16px; font-size: 12px;
                       text-transform: uppercase; letter-spacing: 0.05em; color: #64748b;
                       border-bottom: 2px solid #e2e8f0; }
                  td { padding: 12px 16px; font-size: 14px; border-bottom: 1px solid #f1f5f9; }
                  tr:last-child td { border-bottom: none; }
                  tr:hover td { background: #f8fafc; }
                  .badge { display: inline-block; padding: 3px 10px; border-radius: 999px;
                           font-size: 12px; font-weight: 600; }
                  .badge-present { background: #dcfce7; color: #16a34a; }
                  .badge-absent  { background: #fee2e2; color: #dc2626; }
                  .footer { background: #f8fafc; border-top: 1px solid #e2e8f0;
                            padding: 16px 40px; font-size: 12px; color: #94a3b8;
                            display: flex; justify-content: space-between; }
                  @media print {
                    body { background: white; }
                    .page { box-shadow: none; margin: 0; border-radius: 0; }
                  }
                </style>
                </head>
                <body>
                <div class="page">
                """);

        // Header
        html.append("<div class=\"header\">");
        html.append("<h1>Attendance Report</h1>");
        html.append("<p>COMP1402 - Object Oriented Programming</p>");
        html.append("<div class=\"meta\">");
        html.append("<div class=\"meta-item\">Subject: ").append(escHtml(subject)).append("</div>");
        html.append("<div class=\"meta-item\">Generated: ")
            .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")))
            .append("</div>");
        html.append("</div></div>");

        // Stats
        html.append("<div class=\"stats\">");
        html.append("<div class=\"stat total\"><div class=\"value\">").append(records.size()).append("</div><div class=\"label\">Total</div></div>");
        html.append("<div class=\"stat present\"><div class=\"value\">").append(presentCount).append("</div><div class=\"label\">Present</div></div>");
        html.append("<div class=\"stat absent\"><div class=\"value\">").append(absentCount).append("</div><div class=\"label\">Absent</div></div>");
        html.append(String.format("<div class=\"stat rate\"><div class=\"value\">%.1f%%</div><div class=\"label\">Attendance Rate</div></div>", rate));
        html.append("</div>");

        // Table
        html.append("<div class=\"content\">");
        html.append("<table><thead><tr>");
        html.append("<th>#</th><th>Student Name</th><th>Student ID</th><th>Status</th><th>Timestamp</th>");
        html.append("</tr></thead><tbody>");

        int i = 1;
        for (AttendanceRecord r : records) {
            String badgeClass = "PRESENT".equals(r.getStatus()) ? "badge-present" : "badge-absent";
            html.append("<tr>");
            html.append("<td>").append(i++).append("</td>");
            html.append("<td>").append(escHtml(r.getStudentName())).append("</td>");
            html.append("<td>").append(escHtml(r.getStudentId())).append("</td>");
            html.append("<td><span class=\"badge ").append(badgeClass).append("\">")
                .append(r.getStatus()).append("</span></td>");
            html.append("<td>").append(r.getFormattedTimestamp()).append("</td>");
            html.append("</tr>");
        }

        html.append("</tbody></table></div>");

        // Footer
        html.append("<div class=\"footer\">");
        html.append("<span>Smart Attendance System - COMP1402</span>");
        html.append("<span>To save as PDF: open in browser → Ctrl+P → Save as PDF</span>");
        html.append("</div></div></body></html>");

        Files.writeString(outputPath, html.toString());
        return outputPath.toAbsolutePath();
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public Path getReportsDir() {
        return Paths.get(REPORTS_DIR).toAbsolutePath();
    }
}
