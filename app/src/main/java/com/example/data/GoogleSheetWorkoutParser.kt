package com.example.data

import com.example.model.ParsedExerciseLog
import com.example.model.ParsedSetLog
import com.example.model.ParsedWorkoutRant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoogleSheetWorkoutParser {

  private val DATE_FORMATS = listOf(
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT),
    SimpleDateFormat("MM/dd/yyyy", Locale.ROOT),
    SimpleDateFormat("dd/MM/yyyy", Locale.ROOT),
    SimpleDateFormat("yyyy/MM/dd", Locale.ROOT),
    SimpleDateFormat("MMMM d, yyyy", Locale.ROOT),
    SimpleDateFormat("MMM d, yyyy", Locale.ROOT),
    SimpleDateFormat("MMMM d yyyy", Locale.ROOT),
    SimpleDateFormat("MMM d yyyy", Locale.ROOT),
    SimpleDateFormat("d MMMM yyyy", Locale.ROOT),
    SimpleDateFormat("d MMM yyyy", Locale.ROOT)
  )

  fun parseSheetRows(
    rows: List<List<String>>,
    defaultUseLbs: Boolean = false
  ): List<ParsedWorkoutRant> {
    if (rows.isEmpty()) return emptyList()

    // Step 1: Detect if rows match structured tabular format
    val headerIdx = findHeaderRowIndex(rows)
    if (headerIdx != -1) {
      val parsedTable = parseTabularSheet(rows, headerIdx, defaultUseLbs)
      if (parsedTable.isNotEmpty()) {
        return parsedTable
      }
    }

    // Step 2: Fallback to unstructured narrative / block parsing
    val joinedText = rows.joinToString("\n") { row ->
      row.filter { it.isNotBlank() }.joinToString(" ")
    }
    return OfflineRantParser.parseMultiWorkoutRant(joinedText)
  }

  private fun findHeaderRowIndex(rows: List<List<String>>): Int {
    val maxCheck = minOf(rows.size, 5)
    for (i in 0 until maxCheck) {
      val row = rows[i].map { it.lowercase(Locale.ROOT).trim() }
      val hasExercise = row.any { it.contains("exercise") || it.contains("movement") || it.contains("lift") }
      val hasWeightOrReps = row.any { it.contains("weight") || it.contains("kg") || it.contains("lbs") || it.contains("rep") || it.contains("set") }
      val hasDate = row.any { it.contains("date") || it.contains("day") || it.contains("session") || it.contains("timestamp") }

      if ((hasExercise && hasWeightOrReps) || (hasExercise && hasDate)) {
        return i
      }
    }
    return -1
  }

  private fun parseTabularSheet(
    rows: List<List<String>>,
    headerIndex: Int,
    defaultUseLbs: Boolean
  ): List<ParsedWorkoutRant> {
    val headers = rows[headerIndex].map { it.lowercase(Locale.ROOT).trim() }

    var dateCol = -1
    var workoutCol = -1
    var exerciseCol = -1
    var weightCol = -1
    var repsCol = -1
    var setsCol = -1
    var notesCol = -1
    var unitCol = -1

    val setCols = mutableListOf<Int>()

    for ((idx, h) in headers.withIndex()) {
      when {
        dateCol == -1 && (h.contains("date") || h.contains("timestamp") || h == "day") -> dateCol = idx
        workoutCol == -1 && (h.contains("workout") || h.contains("routine") || h.contains("split") || h.contains("category") || h.contains("muscle")) -> workoutCol = idx
        exerciseCol == -1 && (h.contains("exercise") || h.contains("movement") || h.contains("lift") || h == "name") -> exerciseCol = idx
        weightCol == -1 && (h.contains("weight") || h == "kg" || h == "lbs" || h == "load") -> weightCol = idx
        repsCol == -1 && (h.contains("rep") || h == "count") -> repsCol = idx
        setsCol == -1 && (h == "set" || h == "sets" || h.contains("set order") || h.contains("set count")) -> setsCol = idx
        notesCol == -1 && (h.contains("note") || h.contains("comment") || h.contains("rpe")) -> notesCol = idx
        unitCol == -1 && (h.contains("unit") || h.contains("weight unit")) -> unitCol = idx
      }

      // Check for columns like "Set 1", "Set 2", "Set 3"
      if (Regex("""set\s*\d+""").matches(h)) {
        setCols.add(idx)
      }
    }

    if (exerciseCol == -1) {
      // Look for first column with text that isn't date or numbers
      for (c in headers.indices) {
        if (c != dateCol && c != weightCol && c != repsCol) {
          exerciseCol = c
          break
        }
      }
    }

    val isSheetLbs = headers.any { it.contains("lbs") || it.contains("pounds") } || defaultUseLbs

    // Group rows by Date + Workout Name
    // Key: Pair(DateString, WorkoutName)
    val groupedWorkouts = linkedMapOf<String, MutableMap<String, MutableList<ParsedSetLog>>>()
    val workoutDates = mutableMapOf<String, String>()
    val workoutNotes = mutableMapOf<String, MutableList<String>>()

    var lastSeenDate = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
    var lastSeenWorkout = "Gym Workout"

    for (r in (headerIndex + 1) until rows.size) {
      val row = rows[r]
      if (row.all { it.isBlank() }) continue

      val rawDate = if (dateCol in row.indices) row[dateCol].trim() else ""
      val rawWorkout = if (workoutCol in row.indices) row[workoutCol].trim() else ""
      val rawExercise = if (exerciseCol in row.indices) row[exerciseCol].trim() else ""
      val rawWeight = if (weightCol in row.indices) row[weightCol].trim() else ""
      val rawReps = if (repsCol in row.indices) row[repsCol].trim() else ""
      val rawSets = if (setsCol in row.indices) row[setsCol].trim() else ""
      val rawNotes = if (notesCol in row.indices) row[notesCol].trim() else ""
      val rawUnit = if (unitCol in row.indices) row[unitCol].trim().lowercase(Locale.ROOT) else ""

      if (rawDate.isNotBlank()) {
        val standardized = standardizeDate(rawDate)
        if (standardized != null) {
          lastSeenDate = standardized
        }
      }

      if (rawWorkout.isNotBlank()) {
        lastSeenWorkout = rawWorkout
      }

      if (rawExercise.isBlank()) continue

      val canonicalName = OfflineRantParser.resolveCanonicalExerciseName(rawExercise)
      val rowUsesLbs = if (rawUnit.isNotBlank()) rawUnit.contains("lb") else isSheetLbs

      val sessionKey = "$lastSeenDate|||$lastSeenWorkout"
      val exerciseMap = groupedWorkouts.getOrPut(sessionKey) { linkedMapOf() }
      workoutDates[sessionKey] = lastSeenDate

      if (rawNotes.isNotBlank()) {
        workoutNotes.getOrPut(sessionKey) { mutableListOf() }.add(rawNotes)
      }

      val setsList = exerciseMap.getOrPut(canonicalName) { mutableListOf() }

      // Check if row has multiple set columns ("Set 1", "Set 2", etc.)
      if (setCols.isNotEmpty()) {
        for (sc in setCols) {
          if (sc in row.indices && row[sc].isNotBlank()) {
            val cellVal = row[sc].trim()
            val parsedSet = parseSetFromCell(cellVal, rawWeight, rowUsesLbs)
            if (parsedSet != null) {
              setsList.add(parsedSet)
            }
          }
        }
      } else {
        // Single row represents a set or multiple sets
        val (parsedWeightKg, parsedReps) = parseWeightAndReps(rawWeight, rawReps, rowUsesLbs)
        val setCount = parseSetCount(rawSets)

        val finalReps = if (parsedReps > 0) parsedReps else 10.0
        val finalWeight = if (parsedWeightKg >= 0) parsedWeightKg else 0.0

        for (s in 0 until setCount) {
          setsList.add(ParsedSetLog(weightKg = finalWeight, reps = finalReps))
        }
      }
    }

    // Build the ParsedWorkoutRant list
    val results = mutableListOf<ParsedWorkoutRant>()
    for ((sessionKey, exercisesMap) in groupedWorkouts) {
      val parts = sessionKey.split("|||")
      val dateStr = parts.getOrNull(0) ?: lastSeenDate
      var title = parts.getOrNull(1) ?: "Gym Workout"

      val exerciseLogs = exercisesMap.map { (name, sets) ->
        ParsedExerciseLog(exerciseName = name, sets = sets)
      }

      if (title.isBlank() || title == "Gym Workout") {
        title = OfflineRantParser.suggestWorkoutTitle(exerciseLogs)
      }

      val notes = workoutNotes[sessionKey]?.distinct()?.joinToString("; ") ?: ""
      val dateMillis = parseDateToMillis(dateStr)

      results.add(
        ParsedWorkoutRant(
          workoutTitle = title,
          dateDisplay = dateStr,
          workoutDateMillis = dateMillis,
          exercises = exerciseLogs,
          notes = if (notes.isNotBlank()) "Imported from Google Sheets: $notes" else "Imported from Google Sheets",
          clarificationQuestions = emptyList(),
          detectedUnit = if (isSheetLbs) "LBS" else "KG"
        )
      )
    }

    return results
  }

  private fun parseSetFromCell(cell: String, rowWeight: String, isLbs: Boolean): ParsedSetLog? {
    // Cell might contain "10 reps", "10", "80x10", "80kg x 10"
    val clean = cell.trim()
    val matchX = Regex("""(\d+(?:\.\d+)?)\s*(?:kg|lb|lbs)?\s*[x×]\s*(\d+(?:\.\d+)?)""").find(clean)
    if (matchX != null) {
      val w = matchX.groupValues[1].toDoubleOrNull() ?: 0.0
      val r = matchX.groupValues[2].toDoubleOrNull() ?: 10.0
      val wKg = if (isLbs && w > 0) w / 2.20462 else w
      return ParsedSetLog(weightKg = wKg, reps = r)
    }

    val justReps = Regex("""\b(\d+)\b""").find(clean)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
    val weightNum = Regex("""(\d+(?:\.\d+)?)""").find(rowWeight)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    val wKg = if (isLbs && weightNum > 0) weightNum / 2.20462 else weightNum
    return ParsedSetLog(weightKg = wKg, reps = justReps)
  }

  private fun parseWeightAndReps(rawWeight: String, rawReps: String, isLbs: Boolean): Pair<Double, Double> {
    var weight = 0.0
    var reps = 10.0

    // Check reps
    val rMatch = Regex("""(\d+(?:\.\d+)?)""").find(rawReps)
    if (rMatch != null) {
      reps = rMatch.groupValues[1].toDoubleOrNull() ?: 10.0
    }

    // Check weight
    val wMatch = Regex("""(\d+(?:\.\d+)?)""").find(rawWeight)
    if (wMatch != null) {
      val rawW = wMatch.groupValues[1].toDoubleOrNull() ?: 0.0
      val cellHasLbs = rawWeight.contains("lb", ignoreCase = true) || isLbs
      weight = if (cellHasLbs && rawW > 0) rawW / 2.20462 else rawW
    }

    return Pair(weight, reps)
  }

  private fun parseSetCount(rawSets: String): Int {
    if (rawSets.isBlank()) return 1
    // e.g. "3", "3 sets"
    val num = Regex("""\b(\d+)\b""").find(rawSets)?.groupValues?.get(1)?.toIntOrNull()
    return (num ?: 1).coerceIn(1, 10)
  }

  private fun standardizeDate(raw: String): String? {
    val clean = raw.trim()
    for (fmt in DATE_FORMATS) {
      try {
        val parsed = fmt.parse(clean)
        if (parsed != null) {
          return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(parsed)
        }
      } catch (_: Exception) {}
    }
    return null
  }

  private fun parseDateToMillis(raw: String): Long {
    val clean = raw.trim()
    for (fmt in DATE_FORMATS) {
      try {
        val parsed = fmt.parse(clean)
        if (parsed != null) {
          return parsed.time
        }
      } catch (_: Exception) {}
    }
    return System.currentTimeMillis()
  }

  fun parseRawTextToRows(text: String): List<List<String>> {
    val cleanText = text.trim()
    if (cleanText.isBlank()) return emptyList()

    // If text contains tabs, it's copied directly from Google Sheets / Excel cells
    if (cleanText.contains("\t")) {
      return cleanText.lines()
        .filter { it.isNotBlank() }
        .map { line ->
          line.split("\t").map { it.trim().trim('"', '\'') }
        }
    }

    // Otherwise, parse as CSV
    return parseCsvString(cleanText)
  }

  fun parseCsvString(csv: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val currentRow = mutableListOf<String>()
    val currentCell = StringBuilder()
    var inQuotes = false
    var i = 0

    while (i < csv.length) {
      val ch = csv[i]
      if (ch == '"') {
        if (inQuotes && i + 1 < csv.length && csv[i + 1] == '"') {
          currentCell.append('"')
          i++
        } else {
          inQuotes = !inQuotes
        }
      } else if (ch == ',' && !inQuotes) {
        currentRow.add(currentCell.toString().trim())
        currentCell.clear()
      } else if ((ch == '\n' || ch == '\r') && !inQuotes) {
        if (ch == '\r' && i + 1 < csv.length && csv[i + 1] == '\n') {
          i++
        }
        currentRow.add(currentCell.toString().trim())
        currentCell.clear()
        if (currentRow.any { it.isNotBlank() }) {
          rows.add(currentRow.toList())
        }
        currentRow.clear()
      } else {
        currentCell.append(ch)
      }
      i++
    }

    if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
      currentRow.add(currentCell.toString().trim())
      if (currentRow.any { it.isNotBlank() }) {
        rows.add(currentRow.toList())
      }
    }

    return rows
  }
}
