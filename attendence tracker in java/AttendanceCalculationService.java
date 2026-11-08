import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * AttendanceCalculationService
 *
 * Dedicated mathematical calculation engine for Pragati Attend.
 * Implements percentage calculations, target recovery predictions,
 * safe absence buffers, and semester-end feasibility checks.
 */
public class AttendanceCalculationService {

    private final double alertBuffer;

    public AttendanceCalculationService() {
        this.alertBuffer = 5.0; // 5% buffer above target is considered "At Risk"
    }

    public AttendanceCalculationService(double alertBuffer) {
        this.alertBuffer = alertBuffer;
    }

    /**
     * Calculates the attendance percentage from present and absent units.
     * Denominator is (P + A). Holidays and unrecorded dates are excluded from denominator.
     *
     * @param presentUnits present units
     * @param absentUnits absent units
     * @return attendance percentage (0.0 to 100.0) or null if zero denominator
     */
    public Double calculatePercentage(long presentUnits, long absentUnits) {
        long denominator = presentUnits + absentUnits;
        if (denominator <= 0) {
            return null; // Safe handling: no division by zero, NaN, or misleading 0%
        }
        double raw = ((double) presentUnits / (double) denominator) * 100.0;
        return roundToTwoDecimals(raw);
    }

    /**
     * Formats an attendance percentage for display.
     */
    public String formatPercentage(Double percentage) {
        if (percentage == null) {
            return "Not enough data";
        }
        return String.format(Locale.US, "%.2f%%", percentage);
    }

    /**
     * Calculates the minimum consecutive classes needed to reach or restore target attendance.
     * Formula: x = ceil( (T * (P + A) - P) / (1 - T) ) where 0 < T < 1
     *
     * @param presentUnits present units (P)
     * @param absentUnits absent units (A)
     * @param targetPercentage target attendance e.g. 75.0 (T = 0.75)
     * @return consecutive classes needed, 0 if already achieved, or -1 if target is 100% and absences exist
     */
    public long calculateClassesNeededToReachTarget(long presentUnits, long absentUnits, double targetPercentage) {
        long denominator = presentUnits + absentUnits;
        if (denominator <= 0) {
            return 0;
        }

        double targetDecimal = targetPercentage / 100.0;
        double currentRatio = (double) presentUnits / (double) denominator;

        // Already at or above target
        if (currentRatio >= targetDecimal) {
            return 0;
        }

        // Boundary case: Target is 100% and student has missed at least one unit
        if (targetPercentage >= 100.0) {
            if (absentUnits > 0) {
                return -1; // Mathematically impossible to reach 100% once an absence occurs
            }
            return 0;
        }

        // x = ceil( (T * (P + A) - P) / (1 - T) )
        double numerator = (targetDecimal * denominator) - presentUnits;
        double divisor = 1.0 - targetDecimal;

        if (divisor <= 0.0) {
            return -1;
        }

        double required = numerator / divisor;
        long ceilValue = (long) Math.ceil(required);
        return Math.max(1, ceilValue);
    }

    /**
     * Calculates maximum additional consecutive absences the student can sustain while remaining >= target.
     * Formula: max_absences = floor( P / T - (P + A) )
     *
     * @param presentUnits present units (P)
     * @param absentUnits absent units (A)
     * @param targetPercentage target attendance e.g. 75.0 (T = 0.75)
     * @return permitted absences, or 0 if already below target
     */
    public long calculatePermittedFutureAbsences(long presentUnits, long absentUnits, double targetPercentage) {
        long denominator = presentUnits + absentUnits;
        if (denominator <= 0 || targetPercentage <= 0.0) {
            return 0;
        }

        double targetDecimal = targetPercentage / 100.0;
        double currentRatio = (double) presentUnits / (double) denominator;

        if (currentRatio < targetDecimal) {
            return 0;
        }

        // max_absences = floor( P / T - (P + A) )
        double allowedTotal = (double) presentUnits / targetDecimal;
        double margin = allowedTotal - denominator;

        long floorValue = (long) Math.floor(margin);
        return Math.max(0, floorValue);
    }

    /**
     * Determines threshold standing.
     */
    public String determineThresholdStatus(Double currentPercentage, double targetPercentage) {
        if (currentPercentage == null) return "NOT ENOUGH DATA";
        if (currentPercentage < targetPercentage) return "BELOW TARGET";
        if (currentPercentage >= targetPercentage && currentPercentage < (targetPercentage + alertBuffer)) return "AT RISK";
        if (currentPercentage >= 100.0) return "PERFECT ATTENDANCE";
        return "SAFE";
    }

    /**
     * Calculates the maximum possible attendance percentage if the student attends all remaining classes.
     */
    public Double calculateMaxPossiblePercentage(long presentUnits, long absentUnits, long remainingUnits) {
        long totalRecorded = presentUnits + absentUnits;
        long endDenominator = totalRecorded + remainingUnits;
        if (endDenominator <= 0) return null;
        double maxPct = ((double) (presentUnits + remainingUnits) / (double) endDenominator) * 100.0;
        return roundToTwoDecimals(maxPct);
    }

    private Double roundToTwoDecimals(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
