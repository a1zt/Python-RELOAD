package python.reload.client.ui.widget.overlay;

import lombok.Getter;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import python.reload.api.event.events.render.Render2DEvent;
import python.reload.api.utils.animation.*;
import python.reload.api.utils.render.RenderUtil;
import python.reload.api.utils.render.fonts.Fonts;
import python.reload.client.ui.widget.Widget;
import java.awt.Color;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationWidget extends Widget {
    @Getter private static NotificationWidget instance;
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();
    private Notification tN;
    private float centerX = 0;

    public NotificationWidget() {
        super(150f, 50f);
        setInstance(this);
        setEnabled(true);
        getDraggable().setHorizontalDragEnabled(false);
    }

    public static void setInstance(NotificationWidget i) {
        instance = i;
    }

    @Override public String getName() {
        return "Notifications";
    }

    @Override public void render(Render2DEvent.Render2DEventData e) {
        if (mc.player == null) return;

        if (mc.currentScreen instanceof ChatScreen) {
            if (tN == null || !notifications.contains(tN)) {
                tN = new Notification("Пример Уведомления", NotificationType.INFO);
                tN.keep = true;
                notifications.add(tN);
            }
        } else if (tN != null) {
            tN.keep = false;
            tN = null;
        }

        MatrixStack ms = e.matrixStack();
        notifications.removeIf(Notification::rm);

        float screenWidth = mc.getWindow().getScaledWidth();
        float fontSize = scaled(7f);
        float h = fontSize + scaled(5f);
        float gap = scaled(2f);
        float round = h * 0.3f;
        float padding = scaled(4f);

        float maxWidth = 0;
        for (Notification n : notifications) {
            n.a.update();
            if (n.ex && n.a.getValue() < 0.05) continue;

            float tw = Fonts.SF_MEDIUM.getWidth(n.text, fontSize);
            float w = padding * 2 + tw - 1;
            if (w > maxWidth) maxWidth = w;
        }

        centerX = (screenWidth - maxWidth) / 2f;
        float cy = getDraggable().getY();

        getDraggable().setX(centerX);
        getDraggable().setWidth(maxWidth);

        for (Notification n : notifications) {
            if (n.ex && n.a.getValue() < 0.05) continue;

            float v = (float) n.a.getValue();
            float fs = fontSize;
            int a = MathHelper.clamp((int)(240 * v), 0, 255);
            int ta = MathHelper.clamp((int)(255 * v), 5, 255);

            float tw = Fonts.SF_MEDIUM.getWidth(n.text, fs);
            float w = padding * 2 + tw - 1;
            float x = centerX + (maxWidth - w) / 2f;

            float cx = x + w / 2f;
            float centY = cy + h / 2f;

            ms.push();
            ms.translate(cx, centY, 0);
            ms.scale(v, v, 1f);
            ms.translate(-cx, -centY, 0);

            RenderUtil.BLUR_RECT.draw(ms, x, cy, w, h, round - 1, new Color(18, 18, 18, a));

            Color c = n.keep ? Color.getHSBColor((System.currentTimeMillis() % 2000) / 2000f, 0.7f, 1f) : n.type.c;

            float progressWidth = (w - 5) * n.pct();
            float progressX = x + 2.5f;
            float progressY = cy + h - 2f;
            float progressRound = 1f;

            if (progressWidth > progressRound * 2) {
                RenderUtil.RECT.draw(ms, progressX, progressY, progressWidth, 2f, progressRound,
                        new Color(c.getRed(), c.getGreen(), c.getBlue(), ta));
            } else {
                RenderUtil.RECT.draw(ms, progressX, progressY, progressWidth, 2f, 0f,
                        new Color(c.getRed(), c.getGreen(), c.getBlue(), ta));
            }

            float textX = x + (w - tw) / 2f;
            Fonts.SF_MEDIUM.drawText(ms, n.text, textX, cy + h / 2f - fs / 2f + 0.5f, fs,
                    new Color(255, 255, 255, ta));

            ms.pop();
            cy += (h + gap) * v;
        }

        float widgetHeight = Math.max(20f, cy - getDraggable().getY());
        getDraggable().setHeight(widgetHeight);
    }

    public static void notify(String m, NotificationType t) {
        if (instance != null) instance.notifications.add(new Notification(m, t));
    }

    @Override public void render(MatrixStack m) {}

    private static class Notification {
        String text;
        NotificationType type;
        AnimationUtil a = new AnimationUtil();
        long s = System.currentTimeMillis();
        long m = 2000;
        boolean ex, keep;

        Notification(String t, NotificationType tp) {
            text = t;
            type = tp;
            a.setValue(0);
            a.run(1.0, 300, Easing.BACK_OUT);
        }

        float pct() {
            if (keep) return (float)(0.5f + 0.5f * Math.sin(System.currentTimeMillis() / 300.0));
            long e = System.currentTimeMillis() - s;
            return Math.max(0, Math.min(1, (float)(m - e) / m));
        }

        boolean rm() {
            if (keep) return false;
            if (!ex && System.currentTimeMillis() - s > m) {
                ex = true;
                a.run(0.0, 300, Easing.SINE_IN);
            }
            return ex && a.isFinished() && a.getValue() <= 0.05;
        }
    }

    public enum NotificationType {
        SUCCESS(new Color(80, 255, 140)),
        INFO(new Color(80, 180, 255)),
        WARNING(new Color(255, 215, 80)),
        ERROR(new Color(255, 60, 60));

        Color c;

        NotificationType(Color co) {
            c = co;
        }
    }
}