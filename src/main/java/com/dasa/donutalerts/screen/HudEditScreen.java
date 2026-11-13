package com.dasa.donutalerts.screen;

import com.dasa.donutalerts.Alerts;
import com.dasa.donutalerts.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class HudEditScreen extends BaseScreen {
    private int drag = -1; // 0 = toast, 1 = auction
    private int offX, offY;

    public HudEditScreen() {
        super(Text.literal("Move boxes"));
    }

    private PlayerListEntry mySkin() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return null;
        return mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
    }

    private boolean inToast(double mx, double my) {
        int[] t = Alerts.toastPos(width, height);
        return mx >= t[0] && mx <= t[0] + Alerts.toastW() && my >= t[1] && my <= t[1] + Alerts.toastH();
    }

    private boolean inAuction(double mx, double my) {
        int[] a = Alerts.auctionPos(width, height);
        return mx >= a[0] && mx <= a[0] + Alerts.aucW() && my >= a[1] && my <= a[1] + Alerts.aucH();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x55000000);
        PlayerListEntry skin = mySkin();
        Config c = Alerts.cfg;

        Alerts.drawBannerSample(ctx, width, height, skin);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Big center alerts (scroll anywhere else to resize)"), width / 2, (int) (height * 0.2f) - 12, 0xFFAAAAAA);

        int[] t = Alerts.toastPos(width, height);
        int[] a = Alerts.auctionPos(width, height);
        MatrixStack m = ctx.getMatrices();

        m.push();
        m.translate(t[0], t[1], 0f);
        m.scale(c.toastScale, c.toastScale, 1f);
        Alerts.drawToast(ctx, 0, 0, Alerts.sampleName(), 5_000_000, 0x55FF55, skin);
        m.pop();
        ctx.drawText(textRenderer, "Payment alerts", t[0], t[1] - 10, 0xFFFFFFFF, true);

        m.push();
        m.translate(a[0], a[1], 0f);
        m.scale(c.auctionScale, c.auctionScale, 1f);
        Alerts.drawAuctionBox(ctx, 0, 0, true, 30_000, 10_000_000, 1, "Elytra", "minecraft:elytra", Alerts.sampleName(), 12_000_000, skin);
        m.pop();
        ctx.drawText(textRenderer, "Auction box", a[0], a[1] - 10, 0xFFFFFFFF, true);

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Drag boxes to move. Scroll wheel over a box to resize it. Esc when done."), width / 2, 6, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int[] t = Alerts.toastPos(width, height);
            int[] a = Alerts.auctionPos(width, height);
            if (inAuction(mx, my)) {
                drag = 1;
                offX = (int) mx - a[0];
                offY = (int) my - a[1];
                return true;
            }
            if (inToast(mx, my)) {
                drag = 0;
                offX = (int) mx - t[0];
                offY = (int) my - t[1];
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (drag >= 0) {
            Config c = Alerts.cfg;
            double fx = Math.max(0, Math.min(1, (mx - offX) / (double) width));
            double fy = Math.max(0, Math.min(1, (my - offY) / (double) height));
            if (drag == 0) {
                c.toastX = fx;
                c.toastY = fy;
            } else {
                c.auctionX = fx;
                c.auctionY = fy;
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        drag = -1;
        Alerts.cfg.save();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        Config c = Alerts.cfg;
        float d = (float) (vertical * 0.1);
        if (inAuction(mx, my)) c.auctionScale = Alerts.clampScale(c.auctionScale + d);
        else if (inToast(mx, my)) c.toastScale = Alerts.clampScale(c.toastScale + d);
        else c.bannerScale = Alerts.clampScale(c.bannerScale + d);
        c.save();
        return true;
    }
}
