package com.example.okno.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.collectAsState
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

/*
    Plik jest odpowiedzialny za:
      • wyświetlenie wykresu ruchu sieciowego dla wybranego interfejsu,
      • możliwość przełączenia:
          - RX vs TX (odebrane / wysłane)
          - widok "Samples" (surowe próbki liczników) vs "Daily" (zużycie dzienne),
      • automatyczne skalowanie osi Y (B / KB / MB / GB),
      • estetyczne formatowanie osi X (czas lub dzień).

    Dane wejściowe:
      • GraphViewModel — dostarcza listę próbek (ts, rx, tx) jako StateFlow.

    Wewnątrz:
      • wykorzystujemy AndroidView, aby wkomponować LineChart (MPAndroidChart)
        w Compose,
      • próbki są przetwarzane na punkty wykresu:
          - w trybie "Samples": x = timestamp, y = licznik RX/TX
          - w trybie "Daily": x = indeks dnia (0,1,2,...), y = delta (last - first)
*/

@Composable
fun GraphCard(
    graphVM: GraphViewModel
) {
    // Pobieramy strumień próbek z ViewModelu
    val samples by graphVM.samples.collectAsState()

    // Lokalny stan UI:
    //  - showRx: true → pokazujemy RX, false → TX
    //  - aggregateDaily: false → pokazujemy próbki, true → agregacja dzienna
    var showRx by remember { mutableStateOf(true) }             // RX vs TX
    var aggregateDaily by remember { mutableStateOf(false) }    // próbki vs agregacja dzienna

    // Kolory z MaterialTheme, przekonwertowane na kolory Androida (ARGB)
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val rxColor = MaterialTheme.colorScheme.primary.toArgb()
    val txColor = MaterialTheme.colorScheme.secondary.toArgb()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Nagłówek: opis trybu + przyciski przełączające
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = buildString {
                    append(if (showRx) "Ruch: RX" else "Ruch: TX")
                    append(if (aggregateDaily) " (dziennie)" else " (sample)")
                },
                style = MaterialTheme.typography.titleMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Przełączenie RX ↔ TX
                TextButton(onClick = { showRx = !showRx }) {
                    Text(if (showRx) "Pokaż TX" else "Pokaż RX")
                }
                // Przełączenie Samples ↔ Daily
                TextButton(onClick = { aggregateDaily = !aggregateDaily }) {
                    Text(if (aggregateDaily) "Sample" else "Dziennie")
                }
            }
        }

        // Gdy jeszcze nie ma żadnych zapisanych próbek w bazie
        if (samples.isEmpty()) {
            Text(
                text = "Brak danych. Zapisz najpierw jakieś próbki.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            // Właściwy wykres (MPAndroidChart) osadzony w Compose przez AndroidView
            AndroidView(
                factory = { ctx ->
                    LineChart(ctx).apply {
                        description.isEnabled = false
                        axisRight.isEnabled = false

                        // Oś X (czas / indeks dnia)
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.setDrawGridLines(false)
                        xAxis.labelRotationAngle = 90f
                        xAxis.granularity = 1f              // krok co 1 jednostkę na osi X
                        xAxis.isGranularityEnabled = true

                        // Oś Y
                        axisLeft.setDrawGridLines(false)

                        legend.isEnabled = false

                        // Interakcja z wykresem (zoom, drag, pinch)
                        setTouchEnabled(true)
                        isDragEnabled = true
                        setScaleEnabled(true)
                        setPinchZoom(true)
                        isHighlightPerTapEnabled = false
                        isDoubleTapToZoomEnabled = true
                    }
                },
                update = { chart ->
                    // Zastosowanie kolorów z motywu do elementów wykresu
                    chart.xAxis.textColor = textColor
                    chart.axisLeft.textColor = textColor
                    chart.legend.textColor = textColor
                    chart.description.textColor = textColor

                    // 1) Wybierz źródło danych do wykresu: próbki czy agregacja dzienna
                    data class Point(val x: Float, val bytes: Long)

                    // Formatowanie dnia:
                    //  - dayKeyFormat: klucz grupujący (yyyyMMdd)
                    //  - dayLabelFormat: etykieta na osi X (MM/dd)
                    val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    val dayLabelFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                    val dailyLabels = mutableListOf<String>()   // etykiety dni dla osi X

                    val points: List<Point> = if (!aggregateDaily) {
                        // ---------------------------------------------------------
                        // "Samples": pokazujemy wszystkie próbki z bazy
                        // x = timestamp, y = licznik RX/TX
                        // ---------------------------------------------------------
                        samples.map { p ->
                            val bytes = if (showRx) p.rx else p.tx
                            Point(p.ts.toFloat(), bytes)
                        }
                    } else {
                        // ---------------------------------------------------------
                        // "Daily": zużycie dzienne
                        //
                        // Dla każdego dnia liczymy:
                        //   delta = (ostatni licznik - pierwszy licznik)
                        //
                        // x = indeks dnia (0f, 1f, 2f, ...)
                        // y = delta (w bajtach)
                        // ---------------------------------------------------------
                        val grouped = samples
                            .groupBy { p -> dayKeyFormat.format(Date(p.ts)) }

                        // Zapewniamy poprawną kolejność dni (rosnąco po dacie)
                        val sortedDays = grouped.keys.sorted()

                        sortedDays.mapIndexed { index, dayKey ->
                            val list = grouped[dayKey] ?: emptyList()
                            val sorted = list.sortedBy { it.ts }
                            val first = sorted.first()
                            val last = sorted.last()

                            val firstBytes = if (showRx) first.rx else first.tx
                            val lastBytes  = if (showRx) last.rx  else last.tx
                            val delta = (lastBytes - firstBytes).coerceAtLeast(0L)

                            // X = indeks dnia
                            val x = index.toFloat()
                            // Etykieta np. "11/18" dla tego indeksu
                            val label = dayLabelFormat.format(Date(last.ts))
                            dailyLabels.add(label)

                            Point(x, delta)
                        }
                    }

                    // Jeśli mimo wszystko brak punktów — czyścimy wykres i kończymy
                    if (points.isEmpty()) {
                        chart.clear()
                        return@AndroidView
                    }

                    // 2) Określ jednostkę (B / KB / MB / GB) na podstawie maksymalnej wartości
                    val maxBytes = points.maxOf { it.bytes }.coerceAtLeast(1L)
                    val (scale, unitLabel) = when {
                        maxBytes >= 1024L * 1024L * 1024L -> 1024f * 1024f * 1024f to "GB"
                        maxBytes >= 1024L * 1024L        -> 1024f * 1024f        to "MB"
                        maxBytes >= 1024L                -> 1024f                to "KB"
                        else                             -> 1f                   to "B"
                    }

                    // 3) Zamiana punktów na Entry (format MPAndroidChart)
                    val entries = points.map { p ->
                        Entry(p.x, p.bytes.toFloat() / scale)
                    }

                    val label = buildString {
                        append(if (showRx) "RX" else "TX")
                        append(" (")
                        append(unitLabel)
                        append(")")
                        if (aggregateDaily) append(" / dzień")
                    }

                    val singlePoint = entries.size == 1

                    // Ustawienia wyglądu serii danych na wykresie
                    val dataSet = LineDataSet(entries, label).apply {
                        // Jeśli mamy tylko jeden punkt, rysujemy kółko zamiast linii
                        setDrawCircles(singlePoint)
                        circleRadius = 4f
                        setDrawCircleHole(false)

                        lineWidth = if (singlePoint) 0f else 2f
                        setDrawValues(aggregateDaily)         // etykiety wartości tylko w trybie dziennym
                        valueTextColor = textColor
                        valueTextSize = 10f

                        if (aggregateDaily) {
                            // Etykiety punktów: "123.45 MB"
                            valueFormatter = object : ValueFormatter() {
                                override fun getPointLabel(entry: Entry?): String {
                                    if (entry == null) return ""
                                    return String.format(Locale.getDefault(), "%.2f %s", entry.y, unitLabel)
                                }
                            }
                        }

                        color = if (showRx) rxColor else txColor
                        setDrawFilled(!singlePoint)
                        if (!singlePoint) {
                            fillColor = color
                            fillAlpha = 60
                        }
                    }

                    chart.data = LineData(dataSet)

                    // Formatowanie osi X:
                    //   • w trybie "Samples" – pokazujemy pełną datę z godziną
                    //   • w trybie "Daily"   – korzystamy z etykiet z dailyLabels (MM/dd)
                    val xFmt = object : ValueFormatter() {
                        private val dfSample = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

                        override fun getAxisLabel(
                            value: Float,
                            axis: com.github.mikephil.charting.components.AxisBase?
                        ): String {
                            return if (aggregateDaily) {
                                // W trybie dziennym X to indeks (0,1,2,...)
                                // Labelujemy tylko całkowite indeksy (unikamy duplikatów)
                                val i = value.toInt()
                                val isExactInt = kotlin.math.abs(value - i.toFloat()) < 0.001f
                                if (isExactInt && i in dailyLabels.indices) dailyLabels[i] else ""
                            } else {
                                // W trybie próbek X to timestamp w ms
                                dfSample.format(Date(value.toLong()))
                            }
                        }
                    }
                    chart.xAxis.valueFormatter = xFmt


                    // Formatowanie osi Y: pokazujemy wartość + jednostkę (B/KB/MB/GB)
                    chart.axisLeft.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return String.format(Locale.getDefault(), "%.2f %s", value, unitLabel)
                        }
                    }

                    // Odświeżenie wykresu
                    chart.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }
    }
}
