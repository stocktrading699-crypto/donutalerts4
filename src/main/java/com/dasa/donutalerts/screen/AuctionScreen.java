package com.dasa.donutalerts.screen;

import com.dasa.donutalerts.Alerts;
import com.dasa.donutalerts.Config;
import com.dasa.donutalerts.Fmt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Half-screen menu at the bottom. Page 1 = start an auction, page 2 = settings. */
public class AuctionScreen extends BaseScreen {
    private static final int COLS = 11;

    private static final class Entry {
        final Item item;
        final String lname;

        Entry(Item item) {
            this.item = item;
            this.lname = item.getName().getString().toLowerCase(Locale.ROOT);
        }
    }

    private static List<Entry> ALL;

    private static List<Entry> all() {
        if (ALL == null) {
            ALL = new ArrayList<>();
            for (Item i : Registries.ITEM) {
                if (i != Items.AIR) ALL.add(new Entry(i));
            }
        }
        return ALL;
    }

    private final List<Entry> view = new ArrayList<>();
    private final List<ClickableWidget> startW = new ArrayList<>();
    private final List<ClickableWidget> settingsW = new ArrayList<>();
    private String searchText = "";
    private int tab = 0; // 0 = start, 1 = settings
    private int page = 0;
    private int rows, per, panelTop, L, cy, RX;

    public AuctionScreen() {
        super(Text.literal("Auction"));
        // Opening the menu auto-picks the item (and stack size) you're holding.
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && !mc.player.getMainHandStack().isEmpty()) {
            ItemStack s = mc.player.getMainHandStack();
            Alerts.cfg.auctionItemId = Registries.ITEM.getId(s.getItem()).toString();
            Alerts.cfg.auctionQty = s.getCount();
        }
    }

    private void refreshView(String q) {
        view.clear();
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        for (Entry e : all()) {
            if (needle.isEmpty() || e.lname.contains(needle)) view.add(e);
        }
        page = 0;
    }

    private int pages() {
        return Math.max(1, (view.size() + per - 1) / per);
    }

    private static String sumLabel() {
        return "Add up bids: " + (Alerts.cfg.sumBids ? "ON" : "OFF");
    }

    private static String followLabel() {
        return "Alerts only during auctions: " + (Alerts.cfg.autoWithAuction ? "ON" : "OFF");
    }

    private void applyTab() {
        for (ClickableWidget w : startW) w.visible = tab == 0;
        for (ClickableWidget w : settingsW) w.visible = tab == 1;
        setFocused(null);
    }

    @Override
    protected void init() {
        startW.clear();
        settingsW.clear();
        Config c = Alerts.cfg;

        rows = Math.max(2, Math.min(4, c.itemRows));
        per = COLS * rows;
        int panelH = 72 + 20 * rows;
        panelTop = height - panelH;
        L = width / 2 - 230;
        cy = panelTop + 26;
        RX = L + 246;

        refreshView(searchText);

        // ---- tabs
        addDrawableChild(ButtonWidget.builder(Text.literal("Start auction"), b -> {
            tab = 0;
            applyTab();
        }).dimensions(L + 10, panelTop + 4, 100, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Settings"), b -> {
            tab = 1;
            applyTab();
        }).dimensions(L + 114, panelTop + 4, 100, 16).build());

        // =============== PAGE 1: start auction ===============
        TextFieldWidget search = new TextFieldWidget(textRenderer, L + 10, cy, 220, 16, Text.literal("search"));
        search.setPlaceholder(Text.literal("Search items..."));
        search.setText(searchText);
        search.setChangedListener(s -> {
            searchText = s;
            refreshView(s);
        });
        startW.add(addDrawableChild(search));

        for (int r = 0; r < rows; r++) {
            for (int col = 0; col < COLS; col++) {
                int idx = r * COLS + col;
                startW.add(addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
                    int i = page * per + idx;
                    if (i < view.size()) c.auctionItemId = Registries.ITEM.getId(view.get(i).item).toString();
                }).dimensions(L + 10 + col * 20, cy + 20 + r * 20, 20, 20).build()));
            }
        }

        int navY = cy + 20 + rows * 20 + 2;
        startW.add(addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            if (page > 0) page--;
        }).dimensions(L + 10, navY, 30, 16).build()));
        startW.add(addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            if (page + 1 < pages()) page++;
        }).dimensions(L + 200, navY, 30, 16).build()));

        TextFieldWidget qty = new TextFieldWidget(textRenderer, RX, cy + 30, 60, 16, Text.literal("amount"));
        qty.setMaxLength(5);
        qty.setText(String.valueOf(c.auctionQty));
        qty.setChangedListener(s -> {
            try {
                int v = Integer.parseInt(s.trim());
                if (v > 0) c.auctionQty = v;
            } catch (NumberFormatException ignored) {}
        });
        startW.add(addDrawableChild(qty));

        TextFieldWidget time = new TextFieldWidget(textRenderer, RX + 68, cy + 30, 60, 16, Text.literal("time"));
        time.setMaxLength(8);
        time.setText(c.auctionTime);
        time.setChangedListener(s -> {
            if (Fmt.parseDuration(s) > 0) c.auctionTime = s.trim();
        });
        startW.add(addDrawableChild(time));

        TextFieldWidget min = new TextFieldWidget(textRenderer, RX + 136, cy + 30, 68, 16, Text.literal("min"));
        min.setMaxLength(12);
        min.setText(Fmt.shortNum(c.auctionMin));
        min.setChangedListener(s -> {
            Double v = Fmt.parseLoose(s);
            if (v != null && v >= 0) c.auctionMin = v;
        });
        startW.add(addDrawableChild(min));

        startW.add(addDrawableChild(ButtonWidget.builder(Text.literal("START"), b -> {
            Alerts.startAuction();
            close();
        }).dimensions(RX, cy + 50, 100, 20).build()));
        startW.add(addDrawableChild(ButtonWidget.builder(Text.literal("STOP"), b -> Alerts.stopAuction())
                .dimensions(RX + 104, cy + 50, 100, 20).build()));

        // =============== PAGE 2: settings ===============
        settingsW.add(addDrawableChild(ButtonWidget.builder(Text.literal(sumLabel()), b -> {
            c.sumBids = !c.sumBids;
            b.setMessage(Text.literal(sumLabel()));
        }).dimensions(L + 10, cy, 220, 20).build()));

        settingsW.add(addDrawableChild(ButtonWidget.builder(Text.literal(followLabel()), b -> {
            c.autoWithAuction = !c.autoWithAuction;
            b.setMessage(Text.literal(followLabel()));
        }).dimensions(L + 10, cy + 24, 220, 20).build()));

        settingsW.add(addDrawableChild(ButtonWidget.builder(Text.literal("Item rows in picker: " + rows), b -> {
            c.itemRows = c.itemRows >= 4 ? 2 : c.itemRows + 1;
            clearAndInit();
        }).dimensions(L + 10, cy + 48, 220, 20).build()));

        TextFieldWidget late = new TextFieldWidget(textRenderer, RX, cy + 16, 60, 16, Text.literal("late"));
        late.setMaxLength(2);
        late.setText(String.valueOf(c.lateBidSeconds));
        late.setChangedListener(s -> {
            try {
                int v = Integer.parseInt(s.trim());
                if (v >= 0 && v <= 30) c.lateBidSeconds = v;
            } catch (NumberFormatException ignored) {}
        });
        settingsW.add(addDrawableChild(late));

        applyTab();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Config c = Alerts.cfg;

        // game stays visible above the panel; alerts/effects draw first so the panel stays clean
        Alerts.renderAll(ctx);

        ctx.fill(0, panelTop, width, height, 0xE60C0F1A);
        ctx.fill(0, panelTop, width, panelTop + 1, 0xFFFFAA00);

        for (Element e : children()) {
            if (e instanceof Drawable d) d.render(ctx, mouseX, mouseY, delta);
        }

        // active tab underline
        ctx.fill(L + 10 + tab * 104, panelTop + 20, L + 110 + tab * 104, panelTop + 22, 0xFFFFAA00);

        if (tab == 0) {
            // item picker icons
            for (int r = 0; r < rows; r++) {
                for (int col = 0; col < COLS; col++) {
                    int i = page * per + r * COLS + col;
                    if (i >= view.size()) continue;
                    ctx.drawItem(new ItemStack(view.get(i).item), L + 10 + col * 20 + 2, cy + 20 + r * 20 + 2);
                }
            }
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Page " + (page + 1) + " / " + pages()), L + 120, cy + 20 + rows * 20 + 6, 0xFFAAAAAA);

            ctx.drawItem(Alerts.itemStack(c.auctionItemId), RX, cy - 2);
            ctx.drawText(textRenderer, Alerts.itemName(c.auctionItemId), RX + 20, cy + 3, 0xFFFFFFFF, true);
            ctx.drawText(textRenderer, "Amount", RX, cy + 20, 0xFF5599FF, true);
            ctx.drawText(textRenderer, "Time (5m,1h)", RX + 68, cy + 20, 0xFF5599FF, true);
            ctx.drawText(textRenderer, "Min bid (1m)", RX + 136, cy + 20, 0xFF5599FF, true);
            ctx.drawText(textRenderer, Alerts.statusLine(), RX, cy + 74, 0xFFAAAAAA, false);
        } else {
            ctx.drawText(textRenderer, "Late bid window (seconds)", RX, cy + 4, 0xFF5599FF, true);
            ctx.drawText(textRenderer, "Bids this long after the timer ends still count", RX, cy + 38, 0xFF888888, false);
            ctx.drawText(textRenderer, "(stream delay). Alerts stay on for this window too.", RX, cy + 50, 0xFF888888, false);
        }
    }
}
