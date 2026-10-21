package io.github.jmpe.cecil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(16, 21, 19);
    private static final int PANEL = Color.rgb(27, 35, 31);
    private static final int PANEL2 = Color.rgb(36, 46, 41);
    private static final int TEXT = Color.rgb(239, 246, 241);
    private static final int MUTED = Color.rgb(164, 181, 170);
    private static final int GREEN = Color.rgb(110, 231, 183);
    private static final int BORDER = Color.rgb(57, 73, 64);

    private static final int[] EGG_COLORS = {
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#F8F7F0"),
            Color.parseColor("#F3E8CF"),
            Color.parseColor("#E9D8B5"),
            Color.parseColor("#E5CDA3"),
            Color.parseColor("#E8C99B"),
            Color.parseColor("#D8B078"),
            Color.parseColor("#C99555"),
            Color.parseColor("#B97942"),
            Color.parseColor("#985B35"),
            Color.parseColor("#743F2B"),
            Color.parseColor("#572D24"),
            Color.parseColor("#854536"),
            Color.parseColor("#692D24")
    };
    private static final int LEGACY_COLOR = Color.parseColor("#E9D8B5");

    private static final String KIND_STARTED = "Incubation started";
    private static final String KIND_FAILED = "Incubation failed";
    private static final String KIND_SUCCESSFUL = "Incubation successful";
    private static final String KIND_ABORTED = "Incubation aborted";
    private static final String[] HATCH_KINDS = {
            KIND_STARTED, KIND_FAILED, KIND_SUCCESSFUL, KIND_ABORTED
    };
    private static final int KIND_STARTED_COLOR = Color.parseColor("#93C5FD");
    private static final int KIND_FAILED_COLOR = Color.parseColor("#FCA5A5");
    private static final int KIND_SUCCESS_COLOR = GREEN;
    private static final int KIND_ABORTED_COLOR = MUTED;

    private EggDb db;
    private SharedPreferences prefs;
    private LinearLayout page;
    private LocalDate selectedDate = LocalDate.now();
    private LocalDate anchorDate = LocalDate.now();
    private String viewMode = "MONTH";
    private String defaultViewMode = "MONTH";
    private String currentPage = "COLLECTIONS";
    private boolean menuOpen = false;
    private TextView totalValue, todayValue, selectedHeading;
    private LinearLayout calendarContainer;
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault());

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String savedDefault = prefs.getString("defaultViewMode", "MONTH");
        defaultViewMode = ("WEEK".equals(savedDefault) || "YEAR".equals(savedDefault)) ? savedDefault : "MONTH";
        db = new EggDb();
        if (state != null) {
            currentPage = state.getString("currentPage", "COLLECTIONS");
            menuOpen = state.getBoolean("menuOpen", false);
            viewMode = state.getString("viewMode", defaultViewMode);
        } else {
            viewMode = defaultViewMode;
        }
        selectedDate = LocalDate.now();
        anchorDate = selectedDate;
        render();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("currentPage", currentPage);
        out.putBoolean("menuOpen", menuOpen);
        out.putString("viewMode", viewMode);
    }

    @Override public void onBackPressed() {
        if (menuOpen) {
            menuOpen = false;
            render();
        } else if (!currentPage.equals("COLLECTIONS")) {
            currentPage = "COLLECTIONS";
            render();
        } else {
            super.onBackPressed();
        }
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
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);
        shell.setPadding(0, dp(10), 0, dp(10));
        if (Build.VERSION.SDK_INT >= 35) {
            shell.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(0, dp(10) + bars.top, 0, dp(10) + bars.bottom);
                return insets;
            });
        }
        shell.addView(buildTopBar());

        page = vertical();
        page.setBackgroundColor(BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page);
        shell.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(shell, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        if (currentPage.equals("HATCHING")) buildHatchingPage();
        else if (currentPage.equals("SETTINGS")) buildSettingsPage();
        else buildCollectionsPage();

        if (menuOpen) {
            root.addView(buildMenu(), new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }

        setContentView(root);
    }

    private View buildTopBar() {
        LinearLayout bar = horizontal();
        bar.setBackgroundColor(BG);
        bar.setPadding(dp(12), dp(8), dp(12), dp(8));

        Button menu = button("☰", () -> { menuOpen = true; render(); });
        menu.setTextSize(22);
        menu.setPadding(0, 0, 0, 0);
        bar.addView(menu, new LinearLayout.LayoutParams(dp(46), dp(44)));

        TextView title = text("Egg Counter", 20, TEXT, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, dp(44), 1);
        tp.setMargins(dp(10), 0, 0, 0);
        bar.addView(title, tp);

        TextView pageLabel = text(currentPage.equals("HATCHING") ? "HATCHING"
                : currentPage.equals("SETTINGS") ? "SETTINGS" : "COLLECTIONS", 11, GREEN, true);
        pageLabel.setGravity(Gravity.CENTER);
        bar.addView(pageLabel);

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(bar);
        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        wrap.addView(divider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        return wrap;
    }

    private View buildMenu() {
        LinearLayout menu = vertical();
        menu.setBackgroundColor(BG);
        menu.setPadding(dp(20), dp(18), dp(20), dp(20));
        if (Build.VERSION.SDK_INT >= 35) {
            menu.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(dp(20), dp(18) + bars.top, dp(20), dp(20) + bars.bottom);
                return insets;
            });
        }
        menu.setClickable(true);
        menu.setOnClickListener(v -> { });

        LinearLayout header = horizontal();
        Button close = button("✕", () -> { menuOpen = false; render(); });
        close.setTextSize(20);
        close.setPadding(0, 0, 0, 0);
        header.addView(close, new LinearLayout.LayoutParams(dp(46), dp(44)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1);
        lp.setMargins(dp(12), 0, 0, 0);
        header.addView(text("MENU", 12, MUTED, true), lp);
        menu.addView(header);

        addSpace(menu, 24);
        menu.addView(menuItem("Collections", "Egg collection log", "COLLECTIONS"));
        addSpace(menu, 8);
        menu.addView(menuItem("Hatching", "Incubation log", "HATCHING"));

        View spacer = new View(this);
        menu.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));
        menu.addView(text("All data stays on this device.", 12, MUTED, false));
        addSpace(menu, 12);
        menu.addView(menuItem("Settings", "Preferences and app info", "SETTINGS"));
        return menu;
    }

    private View menuItem(String title, String subtitle, final String pageId) {
        boolean active = currentPage.equals(pageId);
        LinearLayout item = horizontal();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(active ? PANEL2 : PANEL);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), active ? GREEN : BORDER);
        item.setBackground(bg);
        item.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(0, 0, 0, 0);
        labels.addView(text(title, 22, TEXT, true));
        labels.addView(text(subtitle, 12, MUTED, false));
        item.addView(labels, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        item.addView(text(active ? "●" : "›", 18, active ? GREEN : MUTED, true),
                new LinearLayout.LayoutParams(dp(28), dp(40)));

        item.setOnClickListener(v -> { currentPage = pageId; menuOpen = false; render(); });
        return item;
    }

    private void buildCollectionsPage() {
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

    private void buildHatchingPage() {
        page.addView(text("🐣  Hatching", 26, TEXT, true));
        page.addView(text("Incubation log · stored on this device", 13, MUTED, false));
        addSpace(page, 12);

        addHatchSummary(page);
        addSpace(page, 12);

        LinearLayout totals = horizontal();
        LinearLayout allCard = card();
        allCard.addView(text("ALL-TIME HATCHING", 12, MUTED, true));
        allCard.addView(text(String.valueOf(db.hatchTotal()), 34, GREEN, true));
        allCard.addView(text("Eggs in every incubation log", 12, MUTED, false));
        LinearLayout todayCard = card();
        todayCard.addView(text("TODAY", 12, MUTED, true));
        todayCard.addView(text(String.valueOf(db.hatchCount(LocalDate.now())), 34, GREEN, true));
        todayCard.addView(text(LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM yyyy")), 12, MUTED, false));
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, -2, 1);
        half.setMargins(0, 0, dp(6), 0); totals.addView(allCard, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, -2, 1);
        half2.setMargins(dp(6), 0, 0, 0); totals.addView(todayCard, half2);
        page.addView(totals);
        addSpace(page, 12);

        LinearLayout addRow = horizontal();
        addButton(addRow, "+ Log Incubation", () -> hatchLogDialog(selectedDate, null), 1);
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
        selectedCard.addView(text("Daily total: " + db.hatchCount(selectedDate) + " eggs", 15, GREEN, true));
        selectedCard.addView(text("Incubation logs · tap one to edit", 13, MUTED, true));
        List<HatchGroup> logs = db.hatchLogsFor(selectedDate);
        if (logs.isEmpty()) {
            selectedCard.addView(text("No incubation logs recorded for this day yet.", 14, TEXT, false));
        } else {
            for (HatchGroup log : logs) {
                addSpace(selectedCard, 6);
                selectedCard.addView(hatchLogRow(log));
            }
        }
        addSpace(selectedCard, 8);
        LinearLayout actions = horizontal();
        addButton(actions, "+ Log Incubation", () -> hatchLogDialog(selectedDate, null), 1);
        addButton(actions, "Edit date", () -> chooseDate(), 1);
        selectedCard.addView(actions);
        page.addView(selectedCard);
        addSpace(page, 12);
        page.addView(text("Incubation logs stay on this device. No account, network permission, or cloud sync.", 12, MUTED, false));
    }

    private void buildSettingsPage() {
        page.addView(text("⚙️  Settings", 26, TEXT, true));
        page.addView(text("Preferences · stored on this device", 13, MUTED, false));
        addSpace(page, 12);

        LinearLayout viewCard = card();
        viewCard.addView(text("DEFAULT CALENDAR VIEW", 12, MUTED, true));
        viewCard.addView(text("Used when the app opens.", 12, MUTED, false));
        viewCard.addView(text("Current: " + viewModeName(defaultViewMode), 13, GREEN, true));
        addSpace(viewCard, 6);
        LinearLayout modeRow = horizontal();
        addButton(modeRow, "Week", () -> setDefaultViewMode("WEEK"), 1);
        addButton(modeRow, "Month", () -> setDefaultViewMode("MONTH"), 1);
        addButton(modeRow, "Year", () -> setDefaultViewMode("YEAR"), 1);
        viewCard.addView(modeRow);
        page.addView(viewCard);
        addSpace(page, 12);

        addBreedSettings(page);
        addSpace(page, 12);

        LinearLayout dataCard = card();
        dataCard.addView(text("DATA", 12, MUTED, true));
        dataCard.addView(text(db.collectionLogCount() + " collections · " + db.hatchLogCountAll() + " incubation logs", 14, TEXT, true));
        long breedCount = db.typeCount();
        dataCard.addView(text(breedCount + (breedCount == 1 ? " breed saved" : " breeds saved"), 12, MUTED, false));
        Button clear = button("Clear all logs", () -> confirmClearAllLogs());
        clear.setTextColor(KIND_FAILED_COLOR);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        cp.setMargins(0, dp(8), 0, 0);
        dataCard.addView(clear, cp);
        page.addView(dataCard);
        addSpace(page, 12);

        LinearLayout about = card();
        about.addView(text("ABOUT", 12, MUTED, true));
        about.addView(text("CECIL · Egg Counter", 16, TEXT, true));
        String version = "1.0.1";
        try {
            String installed = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            if (installed != null) version = installed;
        } catch (Exception ignored) { }
        about.addView(text("Version " + version, 12, MUTED, false));
        about.addView(text("Offline by design: no permissions, network access, account, or cloud sync. Licensed GPL-3.0-or-later.", 12, MUTED, false));
        page.addView(about);
        addSpace(page, 12);
        page.addView(text("Settings stay on this device.", 12, MUTED, false));
    }

    private String viewModeName(String mode) {
        if ("WEEK".equals(mode)) return "Week";
        if ("YEAR".equals(mode)) return "Year";
        return "Month";
    }

    private void setDefaultViewMode(String mode) {
        defaultViewMode = mode;
        viewMode = mode;
        prefs.edit().putString("defaultViewMode", mode).apply();
        render();
    }

    private void confirmClearAllLogs() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Clear all logs?")
                .setMessage("This permanently deletes every collection and incubation log. Chicken breeds and their colours are kept.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear all", (d, which) -> {
                    db.clearAllLogs();
                    selectedDate = LocalDate.now();
                    anchorDate = selectedDate;
                    render();
                    Toast.makeText(this, "All logs cleared", Toast.LENGTH_SHORT).show();
                });
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(KIND_FAILED_COLOR));
        dialog.show();
    }

    private void addBreedSettings(LinearLayout parent) {
        LinearLayout breedsCard = card();
        breedsCard.addView(text("CHICKEN BREEDS", 12, MUTED, true));
        breedsCard.addView(text("Set egg colours; tap a breed to edit or delete. Only these breeds appear when logging.", 12, MUTED, false));

        List<EggType> breeds = db.eggTypes();
        if (breeds.isEmpty()) {
            breedsCard.addView(text("No breeds yet. Add one below.", 13, TEXT, false));
        } else {
            for (EggType breed : breeds) {
                addSpace(breedsCard, 6);
                breedsCard.addView(breedRow(breed));
            }
        }

        Button add = button("+ Add breed", () -> breedDialog(null));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
        ap.setMargins(0, dp(8), 0, 0);
        breedsCard.addView(add, ap);
        parent.addView(breedsCard);
    }

    private View breedRow(final EggType breed) {
        LinearLayout row = horizontal();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PANEL2);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), BORDER);
        row.setBackground(bg);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));

        View egg = new View(this);
        egg.setBackground(eggShape(breed.color));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(dp(20), dp(26));
        ep.setMargins(0, 0, dp(8), 0);
        row.addView(egg, ep);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(0, 0, 0, 0);
        info.addView(text(breed.name, 15, TEXT, true));
        info.addView(text(hexColor(breed.color), 11, MUTED, false));
        row.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView edit = text("EDIT", 11, GREEN, true);
        edit.setGravity(Gravity.CENTER);
        row.addView(edit, new LinearLayout.LayoutParams(dp(48), dp(36)));
        row.setOnClickListener(v -> breedDialog(breed));
        return row;
    }

    private void breedDialog(final EggType existing) {
        final int[] chosenColor = { existing != null ? existing.color : EGG_COLORS[7] };

        LinearLayout box = vertical();
        box.addView(text("Breed name", 12, MUTED, true));
        final EditText nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setTextColor(TEXT);
        nameInput.setHintTextColor(MUTED);
        nameInput.setTextSize(15);
        nameInput.setHint("e.g. Australorp");
        if (existing != null) nameInput.setText(existing.name);
        box.addView(nameInput);
        addSpace(box, 8);

        box.addView(text("EGG COLOUR", 12, MUTED, true));
        final View swatch = new View(this);
        swatch.setBackground(eggShape(chosenColor[0]));
        final TextView hex = text(hexColor(chosenColor[0]), 15, TEXT, true);

        LinearLayout colorRow = horizontal();
        GradientDrawable colorBg = new GradientDrawable();
        colorBg.setColor(PANEL2);
        colorBg.setCornerRadius(dp(10));
        colorBg.setStroke(dp(1), BORDER);
        colorRow.setBackground(colorBg);
        colorRow.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(34), dp(44));
        sp.setMargins(0, 0, dp(10), 0);
        colorRow.addView(swatch, sp);
        colorRow.addView(hex, new LinearLayout.LayoutParams(0, dp(44), 1));
        colorRow.setOnClickListener(v -> showHsvColorPicker(chosenColor[0], color -> {
            chosenColor[0] = color;
            swatch.setBackground(eggShape(color));
            hex.setText(hexColor(color));
        }));
        box.addView(colorRow);
        addSpace(box, 4);
        box.addView(text("Tap the egg to open the full HSV colour picker.", 12, MUTED, false));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add breed" : "Edit breed")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null);
        if (existing != null) builder.setNeutralButton("Delete", null);
        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(ignored -> {
            if (existing != null) {
                Button delete = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                delete.setTextColor(KIND_FAILED_COLOR);
                delete.setOnClickListener(v -> confirmDeleteBreed(dialog, existing));
            }
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v -> {
                String name = nameInput.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(this, "Enter a breed name", Toast.LENGTH_LONG).show();
                    return;
                }
                long excludeId = existing != null ? existing.id : -1;
                if (db.eggTypeNameTaken(name, excludeId)) {
                    Toast.makeText(this, "A breed with that name already exists", Toast.LENGTH_LONG).show();
                    return;
                }
                if (existing == null) {
                    db.addEggType(name, chosenColor[0]);
                } else {
                    db.updateEggType(existing.id, name, chosenColor[0]);
                }
                dialog.dismiss();
                render();
                Toast.makeText(this, existing == null ? "Breed added" : "Breed updated", Toast.LENGTH_SHORT).show();
            });
        });
        dialog.show();
    }

    private void confirmDeleteBreed(final AlertDialog parent, final EggType breed) {
        long[] usage = db.breedUsage(breed.id);
        String message;
        if (usage[0] == 0 && usage[1] == 0) {
            message = "This breed is not used by any logs. This cannot be undone.";
        } else {
            message = "This breed is used in " + usage[0] + " collection" + (usage[0] == 1 ? "" : "s")
                    + " and " + usage[1] + " incubation log" + (usage[1] == 1 ? "" : "s")
                    + ". Deleting it will remove its entries from those logs. This cannot be undone.";
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete " + breed.name + "?")
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, which) -> {
                    db.deleteBreed(breed.id);
                    parent.dismiss();
                    render();
                    Toast.makeText(this, "Breed deleted", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private String hexColor(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    private interface ColorPicked {
        void onColor(int color);
    }

    private void showHsvColorPicker(int initialColor, final ColorPicked callback) {
        final HsvPickerView picker = new HsvPickerView(this, initialColor);

        LinearLayout box = vertical();
        LinearLayout previewRow = horizontal();
        final View preview = new View(this);
        preview.setBackground(eggShape(initialColor));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(32), dp(42));
        pp.setMargins(0, 0, dp(10), 0);
        previewRow.addView(preview, pp);
        final TextView hex = text(hexColor(initialColor), 16, TEXT, true);
        previewRow.addView(hex, new LinearLayout.LayoutParams(0, dp(42), 1));
        box.addView(previewRow);
        addSpace(box, 6);

        picker.setColorListener(color -> {
            preview.setBackground(eggShape(color));
            hex.setText(hexColor(color));
        });
        box.addView(picker, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(280)));
        addSpace(box, 6);
        box.addView(text("Drag inside the square for saturation and brightness; drag the bar for hue.", 12, MUTED, false));

        new AlertDialog.Builder(this)
                .setTitle("Egg colour")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Use colour", (d, which) -> callback.onColor(picker.getColor()))
                .show();
    }

    private class HsvPickerView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float hueBarHeight = dp(26);
        private final float gap = dp(14);
        private float hue;
        private float sat;
        private float val;
        private ColorPicked listener;

        HsvPickerView(Context context, int color) {
            super(context);
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hue = hsv[0];
            sat = hsv[1];
            val = hsv[2];
            setFocusable(true);
        }

        void setColorListener(ColorPicked listener) {
            this.listener = listener;
        }

        int getColor() {
            return Color.HSVToColor(new float[]{hue, sat, val});
        }

        @Override protected void onDraw(Canvas canvas) {
            int w = getWidth();
            int h = getHeight();
            float hueTop = h - hueBarHeight;
            float svBottom = hueTop - gap;
            if (w <= 0 || svBottom <= 0) return;

            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(0, 0, w, 0, Color.WHITE,
                    Color.HSVToColor(new float[]{hue, 1f, 1f}), Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, svBottom, paint);
            paint.setShader(new LinearGradient(0, 0, 0, svBottom,
                    Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, svBottom, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(BORDER);
            canvas.drawRect(0, 0, w, svBottom, paint);

            int[] hueColors = {Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN,
                    Color.BLUE, Color.MAGENTA, Color.RED};
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(0, 0, w, 0, hueColors, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, hueTop, w, h, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(BORDER);
            canvas.drawRect(0, hueTop, w, h, paint);

            float cx = sat * w;
            float cy = (1f - val) * svBottom;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx, cy, dp(7), paint);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.BLACK);
            canvas.drawCircle(cx, cy, dp(9), paint);

            float hx = Math.max(dp(3), Math.min(w - dp(3), (hue / 360f) * w));
            RectF marker = new RectF(hx - dp(3), hueTop - dp(2), hx + dp(3), h + dp(2));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawRoundRect(marker, dp(3), dp(3), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(Color.BLACK);
            canvas.drawRoundRect(marker, dp(3), dp(3), paint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    handleTouch(event.getX(), event.getY());
                    return true;
                case MotionEvent.ACTION_MOVE:
                    handleTouch(event.getX(), event.getY());
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                default:
                    return super.onTouchEvent(event);
            }
        }

        private void handleTouch(float x, float y) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;
            float hueTop = h - hueBarHeight;
            float svBottom = hueTop - gap;
            if (y <= svBottom) {
                sat = clamp01(x / w);
                val = clamp01(1f - y / svBottom);
            } else if (y >= hueTop) {
                hue = clamp01(x / w) * 359.999f;
            } else {
                return;
            }
            invalidate();
            if (listener != null) listener.onColor(getColor());
        }

        private float clamp01(float value) {
            return Math.max(0f, Math.min(1f, value));
        }
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

    private void addHatchSummary(LinearLayout parent) {
        LocalDate start = rangeStart();
        LocalDate end = rangeEnd();
        long total = db.hatchCountRange(start, end);
        long logCount = db.hatchLogCountRange(start, end);
        Map<String, Integer> kindCounts = db.hatchKindCountsRange(start, end);
        List<TypeTotal> types = db.hatchTypeTotalsRange(start, end);

        LinearLayout summary = card();
        summary.addView(text(summaryLabel(), 12, MUTED, true));
        LinearLayout valueRow = horizontal();
        valueRow.addView(text(String.valueOf(total), 30, GREEN, true));
        valueRow.addView(text(" eggs in incubation logs", 15, MUTED, false));
        summary.addView(valueRow);
        summary.addView(text(logCount + (logCount == 1 ? " log" : " logs") + " · " + rangeLabel(start, end), 12, MUTED, false));

        LinearLayout kinds = new LinearLayout(this);
        kinds.setOrientation(LinearLayout.VERTICAL);
        kinds.setPadding(0, dp(4), 0, 0);
        for (int i = 0; i < HATCH_KINDS.length; i += 2) {
            LinearLayout row = horizontal();
            for (int j = i; j < i + 2 && j < HATCH_KINDS.length; j++) {
                String kind = HATCH_KINDS[j];
                Integer stored = kindCounts.get(kind);
                int count = stored == null ? 0 : stored;
                LinearLayout cell = horizontal();
                View dot = new View(this);
                dot.setBackground(dotShape(hatchKindColor(kind)));
                LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(8), dp(8));
                dotParams.setMargins(0, 0, dp(6), 0);
                cell.addView(dot, dotParams);
                cell.addView(text(hatchKindShort(kind) + " " + count, 12, MUTED, false));
                row.addView(cell, new LinearLayout.LayoutParams(0, dp(24), 1));
            }
            kinds.addView(row);
        }
        summary.addView(kinds);

        if (!types.isEmpty()) {
            TextView breakdown = text("", 13, TEXT, false);
            breakdown.setPadding(0, dp(4), 0, 0);
            breakdown.setText(typeBreakdown(types));
            summary.addView(breakdown);
        }
        parent.addView(summary);
    }

    private int hatchKindColor(String kind) {
        if (KIND_STARTED.equals(kind)) return KIND_STARTED_COLOR;
        if (KIND_FAILED.equals(kind)) return KIND_FAILED_COLOR;
        if (KIND_SUCCESSFUL.equals(kind)) return KIND_SUCCESS_COLOR;
        return KIND_ABORTED_COLOR;
    }

    private String hatchKindShort(String kind) {
        if (KIND_STARTED.equals(kind)) return "Started";
        if (KIND_FAILED.equals(kind)) return "Failed";
        if (KIND_SUCCESSFUL.equals(kind)) return "Successful";
        if (KIND_ABORTED.equals(kind)) return "Aborted";
        return kind;
    }

    private Drawable dotShape(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
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
        boolean hatching = currentPage.equals("HATCHING");
        List<TypeTotal> types = hatching ? db.hatchTypeTotalsForDay(d) : db.typeTotalsForDay(d);
        long count = hatching ? db.hatchCount(d) : db.count(d);

        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(1), dp(1), dp(1), dp(1));
        tile.setMinimumHeight(dp(compact ? 27 : 52));
        tile.setContentDescription(d.format(dateFmt) + ", " + (hatching ? "hatching, " : "") + count + " eggs");

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
        tile.setOnLongClickListener(v -> {
            selectedDate = d;
            if (hatching) hatchLogDialog(d, null);
            else logCollection(d);
            return true;
        });
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

    private static class BreedRow {
        LinearLayout view;
        Button breedButton;
        EditText qty;
        View swatch;
        Button removeButton;
        EggType breed;
    }

    private static class EggType {
        final long id;
        final String name;
        final int color;
        EggType(long id, String name, int color) {
            this.id = id; this.name = name; this.color = color;
        }
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

    private static class HatchGroup {
        final long createdAt;
        final String logDate;
        final String kind;
        final List<SessionItem> items = new ArrayList<>();
        HatchGroup(long createdAt, String logDate, String kind) {
            this.createdAt = createdAt;
            this.logDate = logDate;
            this.kind = kind;
        }
        long total() {
            long total = 0;
            for (SessionItem item : items) total += item.count;
            return total;
        }
    }

    private boolean requireBreeds(List<EggType> breeds) {
        if (!breeds.isEmpty()) return true;
        new AlertDialog.Builder(this)
                .setTitle("No breeds yet")
                .setMessage("Add chicken breeds in Settings before logging.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open Settings", (d, which) -> {
                    currentPage = "SETTINGS";
                    menuOpen = false;
                    render();
                })
                .show();
        return false;
    }

    private EggType unusedBreed(List<BreedRow> rows, List<EggType> breeds) {
        for (EggType breed : breeds) {
            boolean used = false;
            for (BreedRow row : rows) {
                if (row.breed != null && row.breed.name.equalsIgnoreCase(breed.name)) {
                    used = true;
                    break;
                }
            }
            if (!used) return breed;
        }
        return breeds.get(0);
    }

    private BreedRow createBreedRow(final List<BreedRow> rows, final LinearLayout entries,
                                    final List<EggType> breeds) {
        final BreedRow row = new BreedRow();
        row.breed = unusedBreed(rows, breeds);

        LinearLayout line = horizontal();
        line.setPadding(0, dp(2), 0, 0);

        row.swatch = new View(this);
        row.swatch.setBackground(eggShape(row.breed.color));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(26), dp(34));
        sp.setMargins(0, 0, dp(6), 0);
        line.addView(row.swatch, sp);

        row.breedButton = button(row.breed.name, () -> chooseBreed(row, breeds));
        row.breedButton.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        line.addView(row.breedButton, new LinearLayout.LayoutParams(0, dp(44), 2.2f));

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
            refreshBreedRows(entries, rows);
        });
        row.removeButton.setTextSize(20);
        row.removeButton.setPadding(0, 0, 0, 0);
        line.addView(row.removeButton, new LinearLayout.LayoutParams(dp(36), dp(44)));

        row.view = line;
        return row;
    }

    private void chooseBreed(final BreedRow row, final List<EggType> breeds) {
        String[] names = new String[breeds.size()];
        for (int i = 0; i < breeds.size(); i++) names[i] = breeds.get(i).name;
        new AlertDialog.Builder(this)
                .setTitle("Select breed")
                .setItems(names, (d, which) -> {
                    row.breed = breeds.get(which);
                    updateBreedRow(row);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateBreedRow(BreedRow row) {
        row.breedButton.setText(row.breed.name);
        row.swatch.setBackground(eggShape(row.breed.color));
    }

    private void refreshBreedRows(LinearLayout entries, List<BreedRow> rows) {
        entries.removeAllViews();
        for (BreedRow row : rows) {
            row.removeButton.setVisibility(rows.size() > 1 ? View.VISIBLE : View.GONE);
            entries.addView(row.view);
        }
    }

    private EggType findBreed(List<EggType> breeds, String name) {
        for (EggType breed : breeds) {
            if (breed.name.equalsIgnoreCase(name)) return breed;
        }
        return null;
    }

    private void logCollection(LocalDate date) {
        final List<EggType> breeds = db.eggTypes();
        if (!requireBreeds(breeds)) return;

        final LinearLayout entries = vertical();
        entries.setPadding(dp(8), 0, dp(8), 0);
        final List<BreedRow> rows = new ArrayList<>();

        LinearLayout box = vertical();
        box.addView(text("Date: " + date.format(dateFmt), 14, MUTED, true));
        box.addView(text("Choose a breed and quantity. Manage breeds in Settings.", 12, MUTED, false));
        addSpace(box, 4);
        box.addView(entries);

        BreedRow first = createBreedRow(rows, entries, breeds);
        rows.add(first);
        refreshBreedRows(entries, rows);

        Button addAnother = button("+ Add another breed", () -> {
            BreedRow next = createBreedRow(rows, entries, breeds);
            rows.add(next);
            refreshBreedRows(entries, rows);
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
                for (BreedRow row : rows) {
                    if (row.breed == null) continue;
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
                    String key = row.breed.name.toLowerCase(Locale.ROOT);
                    NewEntry existing = merged.get(key);
                    if (existing == null) {
                        merged.put(key, new NewEntry(row.breed.name, row.breed.color, qty));
                    } else {
                        existing.count += qty;
                    }
                }
                if (merged.isEmpty()) {
                    Toast.makeText(this, "Add at least one breed", Toast.LENGTH_LONG).show();
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

    private View sessionBreakdownView(List<SessionItem> items) {
        LinearLayout out = new LinearLayout(this);
        out.setOrientation(LinearLayout.VERTICAL);
        out.setPadding(0, dp(2), 0, 0);
        for (SessionItem item : items) {
            LinearLayout line = horizontal();
            line.setPadding(0, dp(1), 0, dp(1));
            View egg = new View(this);
            egg.setBackground(eggShape(item.color));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(9), dp(12));
            lp.setMargins(0, 0, dp(6), 0);
            line.addView(egg, lp);
            line.addView(text(item.count + " " + item.name, 13, MUTED, false));
            out.addView(line);
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
        info.addView(sessionBreakdownView(collection.items));
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Edit collection")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .setNeutralButton("Delete", null);
        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(ignored -> {
            Button delete = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            delete.setTextColor(KIND_FAILED_COLOR);
            delete.setOnClickListener(v -> confirmDeleteCollection(dialog, collection));
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

    private void confirmDeleteCollection(final AlertDialog parent, final CollectionGroup collection) {
        new AlertDialog.Builder(this)
                .setTitle("Delete collection?")
                .setMessage("This removes the collection and its egg counts. This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, which) -> {
                    db.deleteCollection(collection);
                    parent.dismiss();
                    render();
                    Toast.makeText(this, "Collection deleted", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void confirmDeleteHatchLog(final AlertDialog parent, final HatchGroup log) {
        new AlertDialog.Builder(this)
                .setTitle("Delete incubation log?")
                .setMessage("This removes the log and its egg counts. This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, which) -> {
                    db.deleteHatchLog(log);
                    parent.dismiss();
                    render();
                    Toast.makeText(this, "Incubation log deleted", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private View hatchLogRow(final HatchGroup group) {
        LinearLayout row = horizontal();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PANEL2);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), BORDER);
        row.setBackground(bg);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));

        View statusDot = new View(this);
        statusDot.setBackground(dotShape(hatchKindColor(group.kind)));
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(10), dp(10));
        dotParams.setMargins(0, 0, dp(8), 0);
        row.addView(statusDot, dotParams);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(0, 0, 0, 0);
        info.addView(text(group.kind, 14, hatchKindColor(group.kind), true));
        LocalDate logDate = LocalDate.parse(group.logDate);
        String dateLabel = logDate.equals(LocalDate.now())
                ? "Today" : logDate.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()));
        long total = group.total();
        info.addView(text(total + (total == 1 ? " egg" : " eggs") + " · " + dateLabel + " · " + timeLabel(group.createdAt), 12, MUTED, false));
        info.addView(sessionBreakdownView(group.items));
        row.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView edit = text("EDIT", 11, GREEN, true);
        edit.setGravity(Gravity.CENTER);
        row.addView(edit, new LinearLayout.LayoutParams(dp(48), dp(36)));

        row.setOnClickListener(v -> hatchLogDialog(logDate, group));
        return row;
    }

    private void pickHatchKind(final String[] selectedKind, final Button kindButton) {
        new AlertDialog.Builder(this)
                .setTitle("Incubation status")
                .setItems(HATCH_KINDS, (d, which) -> {
                    selectedKind[0] = HATCH_KINDS[which];
                    kindButton.setText(selectedKind[0]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void hatchLogDialog(LocalDate defaultDate, final HatchGroup existing) {
        final List<EggType> breeds = new ArrayList<>(db.eggTypes());
        if (existing == null && !requireBreeds(breeds)) return;

        final LinearLayout entries = vertical();
        entries.setPadding(dp(8), 0, dp(8), 0);
        final List<BreedRow> rows = new ArrayList<>();

        LinearLayout box = vertical();
        box.addView(text("Log date (YYYY-MM-DD)", 12, MUTED, true));
        final EditText dateInput = new EditText(this);
        dateInput.setSingleLine(true);
        dateInput.setText(existing != null ? existing.logDate : defaultDate.toString());
        dateInput.setInputType(InputType.TYPE_CLASS_TEXT);
        dateInput.setTextColor(TEXT);
        dateInput.setHintTextColor(MUTED);
        dateInput.setTextSize(15);
        box.addView(dateInput);
        box.addView(text("Defaults to the selected calendar date. Edit it for a past date; the time uses the device clock when you save.", 12, MUTED, false));
        addSpace(box, 6);

        final String[] selectedKind = { existing != null ? existing.kind : KIND_STARTED };
        box.addView(text("Status", 12, MUTED, true));
        final Button kindButton = button(selectedKind[0], () -> { });
        kindButton.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        kindButton.setOnClickListener(v -> pickHatchKind(selectedKind, kindButton));
        box.addView(kindButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));
        addSpace(box, 6);

        box.addView(text("Breeds and quantities. Manage breeds in Settings.", 12, MUTED, false));
        addSpace(box, 4);
        box.addView(entries);

        if (existing != null) {
            for (SessionItem item : existing.items) {
                EggType breed = findBreed(breeds, item.name);
                if (breed == null) {
                    breed = new EggType(0, item.name, item.color);
                    breeds.add(breed);
                }
                BreedRow row = createBreedRow(rows, entries, breeds);
                row.breed = breed;
                updateBreedRow(row);
                row.qty.setText(String.valueOf(item.count));
                rows.add(row);
            }
        } else {
            BreedRow first = createBreedRow(rows, entries, breeds);
            rows.add(first);
        }
        refreshBreedRows(entries, rows);

        Button addAnother = button("+ Add another breed", () -> {
            BreedRow next = createBreedRow(rows, entries, breeds);
            rows.add(next);
            refreshBreedRows(entries, rows);
        });
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(40));
        ap.setMargins(0, dp(6), 0, 0);
        box.addView(addAnother, ap);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Log incubation" : "Edit incubation log")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null);
        if (existing != null) builder.setNeutralButton("Delete", null);
        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(ignored -> {
            if (existing != null) {
                Button delete = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                delete.setTextColor(KIND_FAILED_COLOR);
                delete.setOnClickListener(v -> confirmDeleteHatchLog(dialog, existing));
            }
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v -> {
                LocalDate logDate;
                try {
                    logDate = LocalDate.parse(dateInput.getText().toString().trim());
                } catch (Exception e) {
                    Toast.makeText(this, "Use date format YYYY-MM-DD", Toast.LENGTH_LONG).show();
                    return;
                }
                if (logDate.isAfter(LocalDate.now())) {
                    Toast.makeText(this, "Choose today or a past date", Toast.LENGTH_LONG).show();
                    return;
                }

                Map<String, NewEntry> merged = new LinkedHashMap<>();
                for (BreedRow row : rows) {
                    if (row.breed == null) continue;
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
                    if (qty == 0) continue;
                    String key = row.breed.name.toLowerCase(Locale.ROOT);
                    NewEntry found = merged.get(key);
                    if (found == null) {
                        merged.put(key, new NewEntry(row.breed.name, row.breed.color, qty));
                    } else {
                        found.count += qty;
                    }
                }

                if (merged.isEmpty() && existing == null) {
                    Toast.makeText(this, "Add at least one breed", Toast.LENGTH_LONG).show();
                    return;
                }

                List<NewEntry> toSave = new ArrayList<>(merged.values());
                long createdAt = existing != null ? existing.createdAt : System.currentTimeMillis();
                db.saveHatchLog(createdAt, logDate, selectedKind[0], toSave, existing);
                dialog.dismiss();
                selectedDate = logDate;
                anchorDate = logDate;
                render();
                if (toSave.isEmpty()) {
                    Toast.makeText(this, "Log removed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, existing == null ? "Incubation log saved" : "Incubation log updated", Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialog.show();
    }

    private class EggDb extends SQLiteOpenHelper {
        EggDb() { super(MainActivity.this, "egg_counter.db", null, 4); }

        @Override public void onCreate(SQLiteDatabase d) {
            d.execSQL("CREATE TABLE egg_types (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL COLLATE NOCASE UNIQUE, color INTEGER NOT NULL)");
            d.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, collection_date TEXT NOT NULL, egg_count INTEGER NOT NULL CHECK(egg_count > 0), created_at INTEGER NOT NULL, egg_type_id INTEGER)");
            d.execSQL("CREATE INDEX idx_sessions_date ON sessions(collection_date)");
            createHatchTable(d);
        }

        @Override public void onUpgrade(SQLiteDatabase d, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                d.execSQL("CREATE TABLE IF NOT EXISTS egg_types (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL COLLATE NOCASE UNIQUE, color INTEGER NOT NULL)");
                d.execSQL("ALTER TABLE sessions ADD COLUMN egg_type_id INTEGER");
            }
            if (oldVersion < 4) {
                createHatchTable(d);
            }
        }

        private void createHatchTable(SQLiteDatabase d) {
            d.execSQL("CREATE TABLE IF NOT EXISTS hatch_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, log_date TEXT NOT NULL, created_at INTEGER NOT NULL, kind TEXT NOT NULL, egg_type_id INTEGER, egg_count INTEGER NOT NULL CHECK(egg_count > 0))");
            d.execSQL("CREATE INDEX IF NOT EXISTS idx_hatch_logs_date ON hatch_logs(log_date)");
        }

        List<EggType> eggTypes() {
            List<EggType> out = new ArrayList<>();
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT id, name, color FROM egg_types ORDER BY name COLLATE NOCASE", null);
            try {
                while (c.moveToNext()) {
                    out.add(new EggType(c.getLong(0), c.getString(1), c.getInt(2)));
                }
            } finally { c.close(); }
            return out;
        }

        boolean eggTypeNameTaken(String name, long excludeId) {
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT 1 FROM egg_types WHERE name = ? COLLATE NOCASE AND id != ? LIMIT 1",
                    new String[]{name, String.valueOf(excludeId)});
            try { return c.moveToFirst(); } finally { c.close(); }
        }

        long addEggType(String name, int color) {
            ContentValues cv = new ContentValues();
            cv.put("name", name);
            cv.put("color", color);
            return getWritableDatabase().insertOrThrow("egg_types", null, cv);
        }

        void updateEggType(long id, String name, int color) {
            ContentValues cv = new ContentValues();
            cv.put("name", name);
            cv.put("color", color);
            getWritableDatabase().update("egg_types", cv, "id = ?", new String[]{String.valueOf(id)});
        }

        long[] breedUsage(long id) {
            long[] out = new long[2];
            String[] args = new String[]{String.valueOf(id)};
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT COUNT(DISTINCT created_at) FROM sessions WHERE egg_type_id = ?", args);
            try { c.moveToFirst(); out[0] = c.getLong(0); } finally { c.close(); }
            c = getReadableDatabase().rawQuery(
                    "SELECT COUNT(DISTINCT created_at) FROM hatch_logs WHERE egg_type_id = ?", args);
            try { c.moveToFirst(); out[1] = c.getLong(0); } finally { c.close(); }
            return out;
        }

        void deleteBreed(long id) {
            SQLiteDatabase d = getWritableDatabase();
            d.beginTransaction();
            try {
                String[] args = new String[]{String.valueOf(id)};
                d.delete("sessions", "egg_type_id = ?", args);
                d.delete("hatch_logs", "egg_type_id = ?", args);
                d.delete("egg_types", "id = ?", args);
                d.setTransactionSuccessful();
            } finally {
                d.endTransaction();
            }
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

        void deleteCollection(CollectionGroup group) {
            SQLiteDatabase d = getWritableDatabase();
            d.beginTransaction();
            try {
                for (SessionItem item : group.items) {
                    d.delete("sessions", "id = ?", new String[]{String.valueOf(item.id)});
                }
                d.setTransactionSuccessful();
            } finally {
                d.endTransaction();
            }
        }

        long collectionLogCount() {
            Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(DISTINCT created_at) FROM sessions", null);
            try { c.moveToFirst(); return c.getLong(0); } finally { c.close(); }
        }

        long hatchLogCountAll() {
            Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(DISTINCT created_at) FROM hatch_logs", null);
            try { c.moveToFirst(); return c.getLong(0); } finally { c.close(); }
        }

        long typeCount() {
            Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM egg_types", null);
            try { c.moveToFirst(); return c.getLong(0); } finally { c.close(); }
        }

        void clearAllLogs() {
            SQLiteDatabase d = getWritableDatabase();
            d.beginTransaction();
            try {
                d.delete("sessions", null, null);
                d.delete("hatch_logs", null, null);
                d.setTransactionSuccessful();
            } finally {
                d.endTransaction();
            }
        }

        void saveHatchLog(long createdAt, LocalDate logDate, String kind,
                          List<NewEntry> entries, HatchGroup existing) {
            SQLiteDatabase d = getWritableDatabase();
            d.beginTransaction();
            try {
                if (existing != null) {
                    for (SessionItem item : existing.items) {
                        d.delete("hatch_logs", "id = ?", new String[]{String.valueOf(item.id)});
                    }
                }
                for (NewEntry entry : entries) {
                    long typeId = upsertType(d, entry.name, entry.color);
                    ContentValues v = new ContentValues();
                    v.put("log_date", logDate.toString());
                    v.put("created_at", createdAt);
                    v.put("kind", kind);
                    v.put("egg_type_id", typeId);
                    v.put("egg_count", entry.count);
                    d.insertOrThrow("hatch_logs", null, v);
                }
                d.setTransactionSuccessful();
            } finally {
                d.endTransaction();
            }
        }

        void deleteHatchLog(HatchGroup group) {
            SQLiteDatabase d = getWritableDatabase();
            d.beginTransaction();
            try {
                for (SessionItem item : group.items) {
                    d.delete("hatch_logs", "id = ?", new String[]{String.valueOf(item.id)});
                }
                d.setTransactionSuccessful();
            } finally {
                d.endTransaction();
            }
        }

        long hatchCount(LocalDate date) {
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT COALESCE(SUM(egg_count),0) FROM hatch_logs WHERE log_date = ?",
                    new String[]{date.toString()});
            try { c.moveToFirst(); return c.getLong(0); } finally { c.close(); }
        }

        long hatchTotal() {
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT COALESCE(SUM(egg_count),0) FROM hatch_logs", null);
            try { c.moveToFirst(); return c.getLong(0); } finally { c.close(); }
        }

        long hatchCountRange(LocalDate start, LocalDate end) {
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT COALESCE(SUM(egg_count),0) FROM hatch_logs WHERE log_date >= ? AND log_date <= ?",
                    new String[]{start.toString(), end.toString()});
            try { c.moveToFirst(); return c.getLong(0); } finally { c.close(); }
        }

        long hatchLogCountRange(LocalDate start, LocalDate end) {
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT COUNT(DISTINCT created_at) FROM hatch_logs WHERE log_date >= ? AND log_date <= ?",
                    new String[]{start.toString(), end.toString()});
            try { c.moveToFirst(); return c.getLong(0); } finally { c.close(); }
        }

        Map<String, Integer> hatchKindCountsRange(LocalDate start, LocalDate end) {
            Map<String, Integer> out = new LinkedHashMap<>();
            Cursor c = getReadableDatabase().rawQuery(
                    "SELECT kind, COUNT(DISTINCT created_at) FROM hatch_logs WHERE log_date >= ? AND log_date <= ? GROUP BY kind",
                    new String[]{start.toString(), end.toString()});
            try {
                while (c.moveToNext()) out.put(c.getString(0), c.getInt(1));
            } finally { c.close(); }
            return out;
        }

        List<TypeTotal> hatchTypeTotalsForDay(LocalDate date) {
            return hatchTypeTotals("WHERE h.log_date = ?", new String[]{date.toString()});
        }

        List<TypeTotal> hatchTypeTotalsRange(LocalDate start, LocalDate end) {
            return hatchTypeTotals("WHERE h.log_date >= ? AND h.log_date <= ?",
                    new String[]{start.toString(), end.toString()});
        }

        private List<TypeTotal> hatchTypeTotals(String where, String[] args) {
            List<TypeTotal> out = new ArrayList<>();
            String sql = "SELECT COALESCE(t.name, 'Eggs'), COALESCE(t.color, " + LEGACY_COLOR + "), SUM(h.egg_count) "
                    + "FROM hatch_logs h LEFT JOIN egg_types t ON t.id = h.egg_type_id "
                    + where
                    + " GROUP BY COALESCE(t.name, 'Eggs'), COALESCE(t.color, " + LEGACY_COLOR + ")"
                    + " ORDER BY SUM(h.egg_count) DESC";
            Cursor c = getReadableDatabase().rawQuery(sql, args);
            try {
                while (c.moveToNext()) {
                    out.add(new TypeTotal(c.getString(0), c.getInt(1), c.getLong(2)));
                }
            } finally { c.close(); }
            return out;
        }

        List<HatchGroup> hatchLogsFor(LocalDate date) {
            String sql = "SELECT h.id, h.egg_count, h.created_at, h.log_date, h.kind, "
                    + "COALESCE(t.name, 'Eggs'), COALESCE(t.color, " + LEGACY_COLOR + ") "
                    + "FROM hatch_logs h LEFT JOIN egg_types t ON t.id = h.egg_type_id "
                    + "WHERE h.log_date = ? ORDER BY h.created_at, h.id";
            LinkedHashMap<Long, HatchGroup> groups = new LinkedHashMap<>();
            Cursor c = getReadableDatabase().rawQuery(sql, new String[]{date.toString()});
            try {
                while (c.moveToNext()) {
                    long createdAt = c.getLong(2);
                    HatchGroup group = groups.get(createdAt);
                    if (group == null) {
                        group = new HatchGroup(createdAt, c.getString(3), c.getString(4));
                        groups.put(createdAt, group);
                    }
                    group.items.add(new SessionItem(c.getLong(0), c.getString(5), c.getInt(6), c.getLong(1)));
                }
            } finally { c.close(); }
            return new ArrayList<>(groups.values());
        }
    }
}
