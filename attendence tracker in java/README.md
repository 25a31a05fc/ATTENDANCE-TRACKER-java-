# Pragati Attend — Smart Semester Attendance Tracker (Java)

A complete, professional, standalone Java web application designed for students of **Pragati University** to manage daily semester attendance, predict required catch-up classes, calculate safe absence buffers, and export attendance audit reports.

---

## 📌 Project Overview
- **Language**: Java 17 LTS (Pure Java Standard Library — Zero External Dependencies).
- **Architecture**: Embedded Lightweight HTTP Server (`com.sun.net.httpserver`) with interactive web UI.
- **Design System**: 60-30-10 University Theme (60% Midnight Navy `#101A2C`, 30% Pearl White `#F8F7F2`, 10% Royal Gold `#D4AF37`).

---

## 🚀 How to Run the Program (In 1 Second)

### Method 1: Using the Command Line
Open your terminal inside this folder and run:

```bash
java AttendanceTracker.java
```
*(Or compile and run with standard javac)*:
```bash
javac AttendanceCalculationService.java AttendanceTracker.java
java AttendanceTracker
```

### Method 2: Double Click
Simply double-click the **`Run.bat`** file in this folder!

Then open your browser and navigate to:
👉 **`http://localhost:8080`**

---

## 🧮 Mathematical Formulas Implemented in Java

### 1. Attendance Percentage Formula
$$\text{Percentage} = \frac{P}{P + A} \times 100$$
*(Excludes official holidays and unrecorded dates from the denominator).*

### 2. Consecutive Catch-Up Classes Formula
When current attendance is below target $T$ ($0 < T < 1$):
$$x = \left\lceil \frac{T(P + A) - P}{1 - T} \right\rceil$$

### 3. Maximum Permitted Future Absences (Safe Buffer)
When current attendance is at or above target $T$:
$$\text{Max Absences} = \left\lfloor \frac{P}{T} - (P + A) \right\rfloor$$

### 4. Semester-End Feasibility Check
Calculates whether the required recovery classes $x$ can still be completed before final semester exams based on remaining instructional dates.

---

## ✨ Features
1. **Live Dashboard**: Displays classes held, attended, missed, holidays, current percentage, and safe absence buffer.
2. **Interactive Simulator**: Instant recalculation for targets (65%, 70%, 75%, 80%, 85%, 90%, 95%, 100%).
3. **What-If Scenario Matrix**: Shows projected percentage if you miss 1 class, or attend next 5, 10, or 15 classes.
4. **Daily Attendance Logger**: Mark Present, Absent, or Holiday with custom reasons.
5. **Report Generation**: One-click download of official CSV and printable university report with signature blocks.

---

&copy; Pragati University. All rights reserved.
