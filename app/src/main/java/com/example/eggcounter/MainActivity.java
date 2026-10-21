package com.example.eggcounter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(16, 21, 19);
    private static final int PANEL = Color.rgb(27, 35, 31);
    private static final int PANEL2 = Color.rgb(36, 46, 41);
    private static final int TEXT = Color.rgb(239, 246, 241);
    private static final int MUTED = Color.rgb(164, 181, 170);
    private static final int GREEN = Color.rgb(110, 231, 183);
    private static final int BORDER = Color.rgb(57, 73, 64);

    private static final int[] EGG_COLORS = {
            Color.parseColor("#FFFFFF"), // Pure white
            Color.parseColor("#F8F7F0"), // Off-white
            Color.parseColor("#F3E8CF"), // Ivory
            Color.parseColor("#E9D8B5"), // Cream
            Color.parseColor("#E5CDA3"), // Tinted beige
            Color.parseColor("#E8C99B"), // Pale tan
            Color.parseColor("#D8B078"), // Light brown
            Color.parseColor("#C99555"), // Golden brown
            Color.parseColor("#B97942"), // Medium brown
            Color.parseColor("#985B35"), // Dark brown
            Color.parseColor("#743F2B"), // Chocolate
            Color.parseColor("#572D24"), // Deep chocolate
            Color.parseColor("#854536"), // Terracotta
            Color.parseColor("#692D24")  // Mahogany
    };
    private static final String[] EGG_COLOR_NAMES = {
            "Pure white", "Off-white", "Ivory", "Cream", "Tinted beige",
            "Pale tan", "Light brown", "Golden brown", "Medium brown",
            "Dark brown", "Chocolate", "Deep chocolate", "Terracotta", "Mahogany"
    };
    private static final int LEGACY_COLOR = Color.parseColor("#E9D8B5");

    private EggDb db;
    private LinearLayout page;
    private LocalDate selectedDate = LocalDate.now();
    private LocalDate anchorDate = LocalDate.now();
    private String viewMode = "MONTH";
    private TextView totalValue, todayValue, selectedHeading;
    private LinearLayout calendarContainer;
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault());

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        db = new EggDb();
        selectedDate = LocalDate.now();
        anchorDate = selectedDate;
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        // Re-read the device's current local date whenever the app resumes.
        if (db != null && selectedDate.equals(anchorDate)) {
            selectedDate = LocalDate.now();
            anchorDate = selectedDate;
        }
        if (db != null) render();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(10), dp(16), dp(12)); return l;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }

    private void addSpace(LinearLayout parent, int h) {
        View v = new View(this); parent.addView(v, new LinearLayout.LayoutParams(1, dp(h)));
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setTextColor(TEXT);
        b.setAllCaps(false); b.setTextSize(13); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL2));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void addButton(LinearLayout row, String label, Runnable action, int weight) {
        Button b = button(label, action);
        row.addView(b, new LinearLayout.LayoutParams(0, dp(44), weight));
    }

    private LinearLayout card() {
        LinearLayout c = vertical(); c.setBackgroundTintList(null);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PANEL); bg.setCornerRadius(dp(16)); bg.setStroke(dp(1), BORDER);
        c.setBackground(bg); return c;
    }

    private Drawable eggShape(int color) {
        return new EggDrawable(color);
    }

    private static int nearestEggColor(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int best = EGG_COLORS[0];
        double bestDistance = Double.MAX_VALUE;
        for (int candidate : EGG_COLORS) {
            double dr = r - Color.red(candidate);
            double dg = g - Color.green(candidate);
            double db = b - Color.blue(candidate);
            double distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static class EggDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        EggDrawable(int color) {
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override public void draw(Canvas canvas) {
            Rect b = getBounds();
            float w = b.width();
            float h = b.height();
            float cx = b.exactCenterX();
            float top = b.top + h * 0.04f;
            float bottom = b.bottom - h * 0.02f;
            float left = b.left + w * 0.05f;
            float right = b.right - w * 0.05f;
            Path egg = new Path();
            egg.moveTo(cx, top);
            egg.cubicTo(cx + w * 0.20f, top, right, top + h * 0.34f, right, top + h * 0.62f);
            egg.cubicTo(right, bottom, left, bottom, left, top + h * 0.62f);
            egg.cubicTo(left, top + h * 0.34f, cx - w * 0.20f, top, cx, top);
            egg.close();
            canvas.drawPath(egg, paint);
        }

        @Override public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override public void setColorFilter(ColorFilter filter) {
            paint.setColorFilter(filter);
            invalidateSelf();
        }

        @Override public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }

    private void render() {
        page = vertical(); page.setBackgroundColor(BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page);
        setContentView(scroll);

        TextView title = text("🥚  Egg Counter", 26, TEXT, true);
        page.addView(title);
        page.addView(text("Your flock's collection log · stored on this device", 13, MUTED, false));
        addSpace(page, 12);

        addSummary(page);
        addSpace(page, 12);

        LinearLayout totals = horizontal();
        LinearLayout allCard = card();
        allCard.addView(text("ALL-TIME EGGS", 12, MUTED, true));
        totalValue = text(String.valueOf(db.total()), 34, GREEN, true);
        allCard.addView(totalValue);
        allCard.addView(text("Every collection, ever recorded", 12, MUTED, false));
        LinearLayout todayCard = card();
        todayCard.addView(text("TODAY", 12, MUTED, true));
        todayValue = text(String.valueOf(db.count(LocalDate.now())), 34, GREEN, true);
        todayCard.addView(todayValue);
        todayCard.addView(text(LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy")), 12, MUTED, false));
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, -2, 1);
        half.setMargins(0, 0, dp(6), 0); totals.addView(allCard, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, -2, 1);
        half2.setMargins(dp(6), 0, 0, 0); totals.addView(todayCard, half2);
        page.addView(totals);
        addSpace(page, 12);

        LinearLayout addRow = horizontal();
        addButton(addRow, "+ Log Collection", () -> logCollection(selectedDate), 1);
        page.addView(addRow);
        addSpace(page, 8);

        LinearLayout modeRow = horizontal();
        addButton(modeRow, "Week", () -> { viewMode="WEEK"; render(); }, 1);
        addButton(modeRow, "Month", () -> { viewMode="MONTH"; render(); }, 1);
        addButton(modeRow, "Year", () -> { viewMode="YEAR"; render(); }, 1);
        page.addView(modeRow);
        addSpace(page, 5);

        LinearLayout nav = horizontal();
        addButton(nav, "‹", () -> movePeriod(-1), 1);
        TextView period = text(periodTitle(), 17, TEXT, true);
        period.setGravity(Gravity.CENTER);
        nav.addView(period, new LinearLayout.LayoutParams(0, dp(44), 4));
        addButton(nav, "›", () -> movePeriod(1), 1);
        page.addView(nav);

        calendarContainer = vertical();
        calendarContainer.setPadding(0, dp(4), 0, dp(4));
        page.addView(calendarContainer);
        renderCalendar();

        addSpace(page, 10);
        LinearLayout selectedCard = card();
        selectedHeading = text("Selected day: " + selectedDate.format(dateFmt), 18, TEXT, true);
        selectedCard.addView(selectedHeading);
        selectedCard.addView(text("Daily total: " + db.count(selectedDate) + " eggs", 15, GREEN, true));
        selectedCard.addView(text("Collections · tap one to edit", 13, MUTED, true));
        List<CollectionGroup> collections = db.collectionsFor(selectedDate);
        if (collections.isEmpty()) {
            selectedCard.addView(text("No collections recorded for this day yet.", 14, TEXT, false));
        } else {
            for (CollectionGroup collection : collections) {
                addSpace(selectedCard, 6);
                selectedCard.addView(collectionRow(collection));
            }
        }
        addSpace(selectedCard, 8);
        LinearLayout actions = horizontal();
        addButton(actions, "+ Log Collection", () -> logCollection(selectedDate), 1);
        addButton(actions, "Edit date", () -> chooseDate(), 1);
        selectedCard.addView(actions);
        page.addView(selectedCard);
        addSpace(page, 12);
        page.addView(text("Offline by design. No account, network permission, or cloud sync.", 12, MUTED, false));
    }

    private void addSummary(LinearLayout parent) {
        LocalDate start = rangeStart();
        LocalDate end = rangeEnd();
        long total = db.countRange(start, end);
        List<TypeTotal> types = db.typeTotalsRange(start, end);

        LinearLayout summary = card();
        summary.addView(text(summaryLabel(), 12, MUTED, true));
        LinearLayout valueRow = horizontal();
        valueRow.addView(text(String.valueOf(total), 30, GREEN, true));
        valueRow.addView(text(" eggs collected", 15, MUTED, false));
        summary.addView(valueRow);
        summary.addView(text(rangeLabel(start, end), 12, MUTED, false));
        if (!types.isEmpty()) {
            TextView breakdown = text("", 13, TEXT, false);
            breakdown.setPadding(0, dp(4), 0, 0);
            breakdown.setText(typeBreakdown(types));
            summary.addView(breakdown);
        }
        parent.addView(summary);
    }

    private String summaryLabel() {
        if (viewMode.equals("WEEK")) return "THIS WEEK";
        if (viewMode.equals("YEAR")) return "THIS YEAR";
        return "THIS MONTH";
    }

    private LocalDate rangeStart() {
        if (viewMode.equals("WEEK")) return anchorDate.with(DayOfWeek.MONDAY);
        if (viewMode.equals("YEAR")) return LocalDate.of(anchorDate.getYear(), 1, 1);
        return YearMonth.from(anchorDate).atDay(1);
    }

    private LocalDate rangeEnd() {
        if (viewMode.equals("WEEK")) return rangeStart().plusDays(6);
        if (viewMode.equals("YEAR")) return LocalDate.of(anchorDate.getYear(), 12, 31);
        return YearMonth.from(anchorDate).atEndOfMonth();
    }

    private String rangeLabel(LocalDate start, LocalDate end) {
        if (viewMode.equals("WEEK")) {
            return start.format(DateTimeFormatter.ofPattern("d MMM")) + " – " + end.format(DateTimeFormatter.ofPattern("d MMM yyyy"));
        }
        if (viewMode.equals("YEAR")) return String.valueOf(anchorDate.getYear());
        return anchorDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()));
    }

    private CharSequence typeBreakdown(List<TypeTotal> types) {
        SpannableStringBuilder out = new SpannableStringBuilder();
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) out.append("   ");
            TypeTotal t = types.get(i);
            int start = out.length();
            out.append("●");
            out.setSpan(new ForegroundColorSpan(t.color), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            out.append(" ").append(String.valueOf(t.count)).append(" ").append(t.name);
        }
        return out;
    }

    private String periodTitle() {
        if (viewMode.equals("WEEK")) {
            LocalDate start = anchorDate.with(DayOfWeek.MONDAY);
            LocalDate end = start.plusDays(6);
            return start.format(DateTimeFormatter.ofPattern("d MMM")) + " – " + end.format(DateTimeFormatter.ofPattern("d MMM yyyy"));
        }
        if (viewMode.equals("YEAR")) return String.valueOf(anchorDate.getYear());
        return anchorDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()));
    }

    private void movePeriod(int amount) {
        if (viewMode.equals("WEEK")) anchorDate = anchorDate.plusWeeks(amount);
        else if (viewMode.equals("YEAR")) anchorDate = anchorDate.plusYears(amount);
        else anchorDate = anchorDate.plusMonths(amount);
        render();
    }

    private void renderCalendar() {
        calendarContainer.removeAllViews();
        if (viewMode.equals("YEAR")) {
            for (int m = 1; m <= 12; m++) {
                YearMonth ym = YearMonth.of(anchorDate.getYear(), m);
                TextView head = text(ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()), 16, GREEN, true);
                calendarContainer.addView(head);
                addMonthGrid(ym, true);
            }
        } else if (viewMode.equals("MONTH")) {
            addMonthGrid(YearMonth.from(anchorDate), false);
        } else {
            LocalDate start = anchorDate.with(DayOfWeek.MONDAY);
            LinearLayout days = vertical(); days.setPadding(0,0,0,0);
            for (int i=0; i<7; i++) {
                LocalDate d = start.plusDays(i);
                days.addView(dayTile(d, false));
            }
            calendarContainer.addView(days);
        }
    }

    private void addMonthGrid(YearMonth ym, boolean compact) {
        LinearLayout grid = vertical(); grid.setPadding(0, dp(3), 0, dp(compact ? 8 : 4));
        LinearLayout header = horizontal();
        String[] labels = {"M","T","W","T","F","S","S"};
        for (String s : labels) {
            TextView t = text(s, 11, MUTED, true); t.setGravity(Gravity.CENTER);
            header.addView(t, new LinearLayout.LayoutParams(0, dp(24), 1));
        }
        grid.addView(header);
        LocalDate first = ym.atDay(1);
        int offset = first.getDayOfWeek().getValue() - 1;
        int totalCells = ((offset + ym.lengthOfMonth() + 6) / 7) * 7;
        for (int base=0; base<totalCells; base+=7) {
            LinearLayout row = horizontal();
            for (int col=0; col<7; col++) {
                int n = base + col - offset + 1;
                if (n < 1 || n > ym.lengthOfMonth()) {
                    TextView blank = text("", 11, MUTED, false);
                    row.addView(blank, new LinearLayout.LayoutParams(0, dp(compact ? 27 : 52), 1));
                } else {
                    LocalDate d = ym.atDay(n);
                    View tile = dayTile(d, compact);
                    row.addView(tile, new LinearLayout.LayoutParams(0, dp(compact ? 27 : 52), 1));
                }
            }
            grid.addView(row);
        }
        calendarContainer.addView(grid);
    }

    private View dayTile(LocalDate d, boolean compact) {
        List<TypeTotal> types = db.typeTotalsForDay(d);
        long count = 0;
        for (TypeTotal t : types) count += t.count;

        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(1), dp(1), dp(1), dp(1));
        tile.setMinimumHeight(dp(compact ? 27 : 52));
        tile.setContentDescription(d.format(dateFmt) + ", " + count + " eggs");

        TextView dayNumber = text(String.valueOf(d.getDayOfMonth()), compact ? 8 : 12,
                count > 0 ? GREEN : TEXT, d.equals(selectedDate));
        dayNumber.setGravity(Gravity.CENTER);
        tile.addView(dayNumber);

        if (count > 0) {
            addEggDots(tile, types, compact);
            if (!compact) {
                TextView amount = text(count + (count == 1 ? " egg" : " eggs"), 8, MUTED, false);
                amount.setGravity(Gravity.CENTER);
                tile.addView(amount);
            }
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(d.equals(selectedDate) ? PANEL2 : BG);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), d.equals(LocalDate.now()) || d.equals(selectedDate) ? GREEN : BG);
        tile.setBackground(bg);

        tile.setOnClickListener(v -> { selectedDate = d; render(); });
        tile.setOnLongClickListener(v -> { selectedDate = d; logCollection(d); return true; });
        return tile;
    }

    private void addEggDots(LinearLayout parent, List<TypeTotal> types, boolean compact) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER);
        int max = compact ? 3 : 5;
        int shown = Math.min(types.size(), max);
        for (int i = 0; i < shown; i++) {
            View egg = new View(this);
            egg.setBackground(eggShape(types.get(i).color));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp(compact ? 4 : 6), dp(compact ? 5 : 8));
            lp.setMargins(dp(1), 0, dp(1), 0);
            row.addView(egg, lp);
        }
        if (types.size() > shown) {
            TextView more = text("+", compact ? 6 : 8, MUTED, false);
            more.setGravity(Gravity.CENTER);
            row.addView(more);
        }
        parent.addView(row);
    }

    private static class TypeRow {
        LinearLayout view;
        AutoCompleteTextView name;
        EditText qty;
        View swatch;
        Button removeButton;
        int color;
    }

    private static class NewEntry {
        final String name;
        final int color;
        int count;
        NewEntry(String name, int color, int count) {
            this.name = name; this.color = color; this.count = count;
        }
    }

    private static class TypeTotal {
        final String name;
        final int color;
        final long count;
        TypeTotal(String name, int color, long count) {
            this.name = name; this.color = color; this.count = count;
        }
    }

    private static class SessionItem {
        final long id;
        final String name;
        final int color;
        final long count;
        SessionItem(long id, String name, int color, long count) {
            this.id = id; this.name = name; this.color = color; this.count = count;
        }
    }

    private static class CollectionGroup {
        final long createdAt;
        final List<SessionItem> items = new ArrayList<>();
        CollectionGroup(long createdAt) { this.createdAt = createdAt; }
        long total() {
            long total = 0;
            for (SessionItem item : items) total += item.count;
            return total;
        }
    }

    private TypeRow createTypeRow(final List<TypeRow> rows, final LinearLayout entries,
                                  final Map<String, Integer> known, final String[] suggestions) {
        final TypeRow row = new TypeRow();
        row.color = EGG_COLORS[rows.size() % EGG_COLORS.length];

        LinearLayout line = horizontal();
        line.setPadding(0, dp(2), 0, 0);

        row.swatch = new View(this);
        row.swatch.setBackground(eggShape(row.color));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(26), dp(34));
        sp.setMargins(0, 0, dp(6), 0);
        line.addView(row.swatch, sp);
        row.swatch.setOnClickListener(v -> pickColor(row));

        row.name = new AutoCompleteTextView(this);
        row.name.setHint("Type name");
        row.name.setSingleLine(true);
        row.name.setThreshold(1);
        row.name.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, suggestions));
        row.name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        row.name.setTextColor(TEXT);
        row.name.setHintTextColor(MUTED);
        row.name.setTextSize(15);
        line.addView(row.name, new LinearLayout.LayoutParams(0, dp(44), 2.2f));

        row.qty = new EditText(this);
        row.qty.setHint("Qty");
        row.qty.setInputType(InputType.TYPE_CLASS_NUMBER);
        row.qty.setText("1");
        row.qty.setTextColor(TEXT);
        row.qty.setHintTextColor(MUTED);
        row.qty.setTextSize(15);
        row.qty.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(dp(60), dp(44));
        qp.setMargins(dp(4), 0, dp(2), 0);
        line.addView(row.qty, qp);

        row.removeButton = button("×", () -> {
            if (rows.size() <= 1) return;
            rows.remove(row);
            refreshTypeRows(entries, rows);
        });
        row.removeButton.setTextSize(20);
        row.removeButton.setPadding(0, 0, 0, 0);
        line.addView(row.removeButton, new LinearLayout.LayoutParams(dp(36), dp(44)));

        row.name.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                Integer saved = known.get(s.toString().trim());
                if (saved != null) {
                    row.color = saved;
                    row.swatch.setBackground(eggShape(saved));
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        row.view = line;
        return row;
    }

    private void refreshTypeRows(LinearLayout entries, List<TypeRow> rows) {
        entries.removeAllViews();
        for (TypeRow r : rows) {
            r.removeButton.setVisibility(rows.size() > 1 ? View.VISIBLE : View.GONE);
            entries.addView(r.view);
        }
    }

    private void pickColor(TypeRow row) {
        new AlertDialog.Builder(this)
                .setTitle("Egg colour")
                .setItems(EGG_COLOR_NAMES, (d, which) -> {
                    row.color = EGG_COLORS[which];
                    row.swatch.setBackground(eggShape(row.color));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logCollection(LocalDate date) {
        final LinearLayout entries = vertical();
        entries.setPadding(dp(8), 0, dp(8), 0);
        final List<TypeRow> rows = new ArrayList<>();
        final Map<String, Integer> known = db.typeColors();
        final String[] suggestions = known.keySet().toArray(new String[0]);

        LinearLayout box = vertical();
        box.addView(text("Date: " + date.format(dateFmt), 14, MUTED, true));
        box.addView(text("Type a type name and a quantity. Tap the colour to change it.", 12, MUTED, false));
        addSpace(box, 4);
        box.addView(entries);

        TypeRow first = createTypeRow(rows, entries, known, suggestions);
        rows.add(first);
        refreshTypeRows(entries, rows);

        Button addAnother = button("+ Add another type", () -> {
            TypeRow next = createTypeRow(rows, entries, known, suggestions);
            rows.add(next);
            refreshTypeRows(entries, rows);
        });
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(40));
        ap.setMargins(0, dp(6), 0, 0);
        box.addView(addAnother, ap);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Log collection")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v -> {
                Map<String, NewEntry> merged = new LinkedHashMap<>();
                for (TypeRow row : rows) {
                    String name = row.name.getText().toString().trim();
                    if (name.isEmpty()) continue;
                    int qty;
                    try {
                        qty = Integer.parseInt(row.qty.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Enter a quantity from 1 to 10000", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (qty <= 0 || qty > 10000) {
                        Toast.makeText(this, "Enter a quantity from 1 to 10000", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String key = name.toLowerCase(Locale.ROOT);
                    NewEntry existing = merged.get(key);
                    if (existing == null) {
                        merged.put(key, new NewEntry(name, row.color, qty));
                    } else {
                        existing.count += qty;
                    }
                }
                if (merged.isEmpty()) {
                    Toast.makeText(this, "Add at least one egg type", Toast.LENGTH_LONG).show();
                    return;
                }
                List<NewEntry> toSave = new ArrayList<>(merged.values());
                long total = 0;
                for (NewEntry e : toSave) total += e.count;
                db.logCollection(date, toSave);
                dialog.dismiss();
                selectedDate = date;
                anchorDate = date;
                render();
                Toast.makeText(this, total + (total == 1 ? " egg saved" : " eggs saved"), Toast.LENGTH_SHORT).show();
            });
        });
        dialog.show();
    }

    private void chooseDate() {
        EditText input = new EditText(this);
        input.setText(selectedDate.toString());
        input.setHint("YYYY-MM-DD");
        new AlertDialog.Builder(this).setTitle("Select date (YYYY-MM-DD)").setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Go", (d,w) -> {
                try {
                    selectedDate = LocalDate.parse(input.getText().toString().trim());
                    anchorDate = selectedDate;
                    render();
                } catch (Exception e) {
                    Toast.makeText(this, "Use date format YYYY-MM-DD", Toast.LENGTH_LONG).show();
                }
            }).show();
    }

    private String timeLabel(long createdAt) {
        return Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()));
    }

    private CharSequence sessionBreakdown(List<SessionItem> items) {
        SpannableStringBuilder out = new SpannableStringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) out.append("   ");
            SessionItem item = items.get(i);
            int start = out.length();
            out.append("●");
            out.setSpan(new ForegroundColorSpan(item.color), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            out.append(" ").append(String.valueOf(item.count)).append(" ").append(item.name);
        }
        return out;
    }

    private View collectionRow(final CollectionGroup collection) {
        LinearLayout row = horizontal();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PANEL2);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), BORDER);
        row.setBackground(bg);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(0, 0, 0, 0);
        long total = collection.total();
        info.addView(text(total + (total == 1 ? " egg" : " eggs") + " · " + timeLabel(collection.createdAt), 14, TEXT, true));
        TextView breakdown = text("", 13, MUTED, false);
        breakdown.setText(sessionBreakdown(collection.items));
        info.addView(breakdown);
        row.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView edit = text("EDIT", 11, GREEN, true);
        edit.setGravity(Gravity.CENTER);
        row.addView(edit, new LinearLayout.LayoutParams(dp(48), dp(36)));

        row.setOnClickListener(v -> editCollection(selectedDate, collection));
        return row;
    }

    private static class EditRow {
        final long id;
        final EditText qty;
        EditRow(long id, EditText qty) { this.id = id; this.qty = qty; }
    }

    private void editCollection(LocalDate date, CollectionGroup collection) {
        final LinearLayout entries = vertical();
        entries.setPadding(dp(8), 0, dp(8), 0);
        final List<EditRow> rows = new ArrayList<>();

        for (SessionItem item : collection.items) {
            LinearLayout line = horizontal();
            line.setPadding(0, dp(2), 0, 0);

            View swatch = new View(this);
            swatch.setBackground(eggShape(item.color));
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(24), dp(32));
            sp.setMargins(0, 0, dp(8), 0);
            line.addView(swatch, sp);

            line.addView(text(item.name, 15, TEXT, true),
                    new LinearLayout.LayoutParams(0, dp(44), 1));

            EditText qty = new EditText(this);
            qty.setInputType(InputType.TYPE_CLASS_NUMBER);
            qty.setText(String.valueOf(item.count));
            qty.setTextColor(TEXT);
            qty.setHintTextColor(MUTED);
            qty.setTextSize(15);
            qty.setGravity(Gravity.CENTER);
            qty.setSelectAllOnFocus(true);
            line.addView(qty, new LinearLayout.LayoutParams(dp(72), dp(44)));

            entries.addView(line);
            rows.add(new EditRow(item.id, qty));
        }

        LinearLayout box = vertical();
        box.addView(text("Date: " + date.format(dateFmt), 14, MUTED, true));
        box.addView(text("Collected at " + timeLabel(collection.createdAt), 13, MUTED, false));
        box.addView(text("Set a quantity to 0 to remove that type.", 12, MUTED, false));
        addSpace(box, 4);
        box.addView(entries);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit collection")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v -> {
                Map<Long, Integer> counts = new LinkedHashMap<>();
                int remaining = 0;
                for (EditRow row : rows) {
                    int qty;
                    try {
                        qty = Integer.parseInt(row.qty.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Enter a quantity from 0 to 10000", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (qty < 0 || qty > 10000) {
                        Toast.makeText(this, "Enter a quantity from 0 to 10000", Toast.LENGTH_LONG).show();
                        return;
                    }
                    counts.put(row.id, qty);
                    remaining += qty;
                }
                db.updateCollectionCounts(counts);
                dialog.dismiss();
                render();
                Toast.makeText(this, remaining == 0 ? "Collection removed" : "Collection updated", Toast.LENGTH_SHORT).show();
            });
        });
        dialog.show();
    }

    private class EggDb extends SQLiteOpenHelper {
        EggDb() { super(MainActivity.this, "egg_counter.db", null, 3); }

        @Override public void onCreate(SQLiteDatabase d) {
            d.execSQL("CREATE TABLE egg_types (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL COLLATE NOCASE UNIQUE, color INTEGER NOT NULL)");
            d.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, collection_date TEXT NOT NULL, egg_count INTEGER NOT NULL CHECK(egg_count > 0), created_at INTEGER NOT NULL, egg_type_id INTEGER)");
            d.execSQL("CREATE INDEX idx_sessions_date ON sessions(collection_date)");
        }

        @Override public void onUpgrade(SQLiteDatabase d, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                d.execSQL("CREATE TABLE IF NOT EXISTS egg_types (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL COLLATE NOCASE UNIQUE, color INTEGER NOT NULL)");
                d.execSQL("ALTER TABLE sessions ADD COLUMN egg_type_id INTEGER");
            }
            if (oldVersion < 3) {
                remapEggColors(d);
            }
        }

        private void remapEggColors(SQLiteDatabase d) {
            List<Long> ids = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            Cursor c = d.rawQuery("SELECT id, color FROM egg_types", null);
            try {
                while (c.moveToNext()) {
                    ids.add(c.getLong(0));
                    colors.add(c.getInt(1));
                }
            } finally { c.close(); }
            for (int i = 0; i < ids.size(); i++) {
                int mapped = nearestEggColor(colors.get(i));
                if (mapped != colors.get(i)) {
                    ContentValues cv = new ContentValues();
                    cv.put("color", mapped);
                    d.update("egg_types", cv, "id = ?", new String[]{String.valueOf(ids.get(i))});
                }
            }
        }

        Map<String, Integer> typeColors() {
            Map<String, Integer> colors = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            Cursor c = getReadableDatabase().rawQuery("SELECT name, color FROM egg_types ORDER BY name COLLATE NOCASE", null);
            try {
                while (c.moveToNext()) colors.put(c.getString(0), c.getInt(1));
            } finally { c.close(); }
            return colors;
        }

        void logCollection(LocalDate date, List<NewEntry> entries) {
            SQLiteDatabase d = getWritableDatabase();
            d.beginTransaction();
            try {
                long createdAt = System.currentTimeMillis();
                for (NewEntry entry : entries) {
                    long typeId = upsertType(d, entry.name, entry.color);
                    ContentValues v = new ContentValues();
                    v.put("collection_date", date.toString());
                    v.put("egg_count", entry.count);
                    v.put("created_at", createdAt);
                    v.put("egg_type_id", typeId);
                    d.insertOrThrow("sessions", null, v);
                }
                d.setTransactionSuccessful();
            } finally {
                d.endTransaction();
            }
        }

        private long upsertType(SQLiteDatabase d, String name, int color) {
            Cursor c = d.rawQuery("SELECT id, color FROM egg_types WHERE name = ?", new String[]{name});
            try {
                if (c.moveToFirst()) {
                    long id = c.getLong(0);
                    if (c.getInt(1) != color) {
                        ContentValues cv = new ContentValues();
                        cv.put("color", color);
                        d.update("egg_types", cv, "id = ?", new String[]{String.valueOf(id)});
                    }
                    return id;
                }
            } finally { c.close(); }
            ContentValues cv = new ContentValues();
            cv.put("name", name);
            cv.put("color", color);
            return d.insertOrThrow("egg_types", null, cv);
        }

        long total() {
            Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(egg_count),0) FROM sessions", null);
            try { c.moveToFirst(); return c.getLong(0); } finally { c.close(); }
        }

        long count(LocalDate date) {
            Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(egg_count),0) FROM sessions WHERE collection_date=?", new String[]{date.toString()});
            try { c.moveToFirst(); return c.getLong(0); } finally { c.close(); }
        }

        long countRange(LocalDate start, LocalDate end) {
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT COALESCE(SUM(egg_count),0) FROM sessions WHERE collection_date >= ? AND collection_date <= ?",
                    new String[]{start.toString(), end.toString()});
            try { c.moveToFirst(); return c.getLong(0); } finally { c.close(); }
        }

        List<TypeTotal> typeTotalsForDay(LocalDate date) {
            return typeTotals("WHERE s.collection_date = ?", new String[]{date.toString()});
        }

        List<TypeTotal> typeTotalsRange(LocalDate start, LocalDate end) {
            return typeTotals("WHERE s.collection_date >= ? AND s.collection_date <= ?",
                    new String[]{start.toString(), end.toString()});
        }

        private List<TypeTotal> typeTotals(String where, String[] args) {
            List<TypeTotal> out = new ArrayList<>();
            String sql = "SELECT COALESCE(t.name, 'Eggs'), COALESCE(t.color, " + LEGACY_COLOR + "), SUM(s.egg_count) "
                    + "FROM sessions s LEFT JOIN egg_types t ON t.id = s.egg_type_id "
                    + where
                    + " GROUP BY COALESCE(t.name, 'Eggs'), COALESCE(t.color, " + LEGACY_COLOR + ")"
                    + " ORDER BY SUM(s.egg_count) DESC";
            Cursor c = getReadableDatabase().rawQuery(sql, args);
            try {
                while (c.moveToNext()) {
                    out.add(new TypeTotal(c.getString(0), c.getInt(1), c.getLong(2)));
                }
            } finally { c.close(); }
            return out;
        }

        List<CollectionGroup> collectionsFor(LocalDate date) {
            String sql = "SELECT s.id, s.egg_count, s.created_at, COALESCE(t.name, 'Eggs'), COALESCE(t.color, " + LEGACY_COLOR + ") "
                    + "FROM sessions s LEFT JOIN egg_types t ON t.id = s.egg_type_id "
                    + "WHERE s.collection_date = ? ORDER BY s.created_at, s.id";
            LinkedHashMap<Long, CollectionGroup> groups = new LinkedHashMap<>();
            Cursor c = getReadableDatabase().rawQuery(sql, new String[]{date.toString()});
            try {
                while (c.moveToNext()) {
                    long createdAt = c.getLong(2);
                    CollectionGroup group = groups.get(createdAt);
                    if (group == null) {
                        group = new CollectionGroup(createdAt);
                        groups.put(createdAt, group);
                    }
                    group.items.add(new SessionItem(c.getLong(0), c.getString(3), c.getInt(4), c.getLong(1)));
                }
            } finally { c.close(); }
            return new ArrayList<>(groups.values());
        }

        void updateCollectionCounts(Map<Long, Integer> counts) {
            SQLiteDatabase d = getWritableDatabase();
            d.beginTransaction();
            try {
                for (Map.Entry<Long, Integer> entry : counts.entrySet()) {
                    String id = String.valueOf(entry.getKey());
                    if (entry.getValue() <= 0) {
                        d.delete("sessions", "id = ?", new String[]{id});
                    } else {
                        ContentValues cv = new ContentValues();
                        cv.put("egg_count", entry.getValue());
                        d.update("sessions", cv, "id = ?", new String[]{id});
                    }
                }
                d.setTransactionSuccessful();
            } finally {
                d.endTransaction();
            }
        }
    }
}
