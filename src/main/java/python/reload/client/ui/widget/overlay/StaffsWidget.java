package python.reload.client.ui.widget.overlay;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.GameMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.screen.ChatScreen;
import python.reload.api.system.configs.StaffManager;
import python.reload.api.utils.color.ColorUtil;
import python.reload.api.utils.color.UIColors;
import python.reload.api.utils.framelimiter.FrameLimiter;
import python.reload.api.utils.other.ReplaceUtil;
import python.reload.api.utils.player.PlayerUtil;
import python.reload.api.utils.render.RenderUtil;
import python.reload.api.utils.render.fonts.Font;
import python.reload.api.utils.render.fonts.Fonts;
import python.reload.client.ui.widget.Widget;
import python.reload.api.utils.animation.AnimationUtil;
import python.reload.api.utils.animation.Easing;

import java.awt.*;
import java.util.*;
import java.util.List;

public class StaffsWidget extends Widget {
    private final FrameLimiter frameLimiter = new FrameLimiter(false);
    private List<Staff> cacheStaffs = new ArrayList<>();
    private AnimationUtil animation = new AnimationUtil();
    private AnimationUtil widthAnimation = new AnimationUtil();
    private AnimationUtil heightAnimation = new AnimationUtil();

    private float cachedBgWidth = 0;
    private float cachedTotalHeight = 0;

    public record Staff(String name, Status status) {}

    @Getter
    @RequiredArgsConstructor
    public enum Status {
        ONLINE("Online", new Color(100, 200, 100)),
        NEAR("Near", new Color(200, 200, 100)),
        GM3("Gm3", new Color(200, 100, 100)),
        VANISH("Vanish", new Color(200, 100, 100));

        private final String label;
        private final Color color;
    }

    @Override
    public String getName() { return "Staffs"; }

    public StaffsWidget() {
        super(100f, 100f);
        widthAnimation.setValue(0);
        heightAnimation.setValue(0);
    }

    @Override
    public void render(MatrixStack matrixStack) {
        List<Staff> staffList = getStaffList();
        staffList.sort(Comparator.comparingInt(s -> s.name().length()));
        Collections.reverse(staffList);

        boolean shouldShow = !staffList.isEmpty() || mc.currentScreen instanceof ChatScreen;
        animation.update();
        widthAnimation.update();
        heightAnimation.update();

        float animValue = (float) animation.getValue();
        float widthAnimValue = (float) widthAnimation.getValue();
        float heightAnimValue = (float) heightAnimation.getValue();

        if (shouldShow && !animation.isAlive()) {
            animation.run(1f, 200L, Easing.SINE_OUT);
        } else if (!shouldShow && animation.isAlive() && animation.getToValue() != 0) {
            animation.run(0f, 200L, Easing.SINE_OUT);
        }

        if (animValue <= 0.01f) return;

        float x = getDraggable().getX();
        float y = getDraggable().getY();
        Font font = getSemiBoldFont();
        float fontSize = scaled(7f);
        float bgHeight = fontSize + scaled(5f);
        float padding = scaled(4f);
        float round = bgHeight * 0.3f;
        float lineSpacing = scaled(1f);
        float iconSize = fontSize + 1f;
        float iconWidth = 8f;
        float textIconSpacing = 2f;

        String title = "Staffs";
        float titleWidth = font.getWidth(title, fontSize);
        float iconTitleWidth = iconWidth + textIconSpacing + titleWidth;

        float maxNameWidth = 0;
        float maxStatusWidth = 0;
        for (Staff staff : staffList) {
            float nameWidth = font.getWidth(staff.name(), fontSize);
            float statusWidth = font.getWidth(staff.status().getLabel(), fontSize);
            if (nameWidth > maxNameWidth) maxNameWidth = nameWidth;
            if (statusWidth > maxStatusWidth) maxStatusWidth = statusWidth;
        }

        float totalLineWidth = maxNameWidth + maxStatusWidth + scaled(10f);
        float maxWidth = Math.max(iconTitleWidth, totalLineWidth);
        float targetBgWidth = maxWidth + padding * 2f;
        float targetTotalHeight = bgHeight * (staffList.size() + 1) + lineSpacing * staffList.size();

        if (targetBgWidth != cachedBgWidth || targetTotalHeight != cachedTotalHeight) {
            cachedBgWidth = targetBgWidth;
            cachedTotalHeight = targetTotalHeight;

            if (!widthAnimation.isAlive()) {
                widthAnimation.run(targetBgWidth, 150L, Easing.SINE_OUT);
            }
            if (!heightAnimation.isAlive()) {
                heightAnimation.run(targetTotalHeight, 150L, Easing.SINE_OUT);
            }
        }

        float bgWidth = (float) widthAnimation.getValue();
        float totalHeight = (float) heightAnimation.getValue();

        float currentY = y;

        float bg1X = x;
        float bg1Y = currentY;

        RenderUtil.BLUR_RECT.draw(matrixStack, bg1X, bg1Y, bgWidth, bgHeight, round - 1, new Color(18, 18, 18, (int)(240 * animValue)));

        float textY1 = bg1Y + (bgHeight - fontSize) / 2f + 0.5f;
        float iconY1 = bg1Y + (bgHeight - iconSize) / 2f;
        float currentX = bg1X + padding - 2;
        Fonts.ICON_DESHUX.drawGradientText(matrixStack, "f", currentX, iconY1, iconSize,
                ColorUtil.setAlpha(UIColors.primary(), (int)(UIColors.primary().getAlpha() * animValue)),
                ColorUtil.setAlpha(UIColors.secondary(), (int)(UIColors.secondary().getAlpha() * animValue)),
                iconWidth / 4f);
        currentX += iconWidth + textIconSpacing;
        font.drawText(matrixStack, title, currentX, textY1, fontSize, new Color(185, 185, 185, (int)(255 * animValue)));
        currentY += bgHeight + lineSpacing * 1.1f;

        for (int i = 0; i < staffList.size(); i++) {
            Staff staff = staffList.get(i);
            if (animValue <= 0.01f) continue;

            float lineAnim = Math.min(1.0f, animValue + (i * 0.05f));

            float bgX = x;
            float bgY = currentY;

            float bgAlpha = animValue * lineAnim;
            RenderUtil.BLUR_RECT.draw(matrixStack, bgX, bgY, bgWidth, bgHeight, round - 1, new Color(18, 18, 18, (int)(240 * bgAlpha)));

            float textY = bgY + (bgHeight - fontSize) / 2f + 0.5f;
            currentX = bgX + padding;

            font.drawText(matrixStack, staff.name(), currentX, textY, fontSize,
                    new Color(185, 185, 185, (int)(255 * bgAlpha)));

            Color statusColor = staff.status().getColor();
            float statusX = bgX + bgWidth - padding - font.getWidth(staff.status().getLabel(), fontSize);
            font.drawText(matrixStack, staff.status().getLabel(), statusX, textY, fontSize,
                    new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(),
                            (int)(statusColor.getAlpha() * bgAlpha)));

            currentY += bgHeight + lineSpacing;
        }

        getDraggable().setWidth(bgWidth);
        getDraggable().setHeight(totalHeight);
    }

    private List<Staff> getStaffList() {
        frameLimiter.execute(15, () -> {
            List<Staff> list = new ArrayList<>();
            if (!mc.isInSingleplayer()) {
                list.addAll(getOnlineStaff());
                list.addAll(getVanishedPlayers());
            }
            cacheStaffs = list;
        });
        return cacheStaffs;
    }

    private List<Staff> getOnlineStaff() {
        List<Staff> staff = new ArrayList<>();
        if (mc.player == null || mc.player.networkHandler == null || mc.world == null) return staff;
        for (PlayerListEntry player : mc.player.networkHandler.getPlayerList()) {
            Team team = player.getScoreboardTeam();
            if (team == null) continue;
            String name = player.getProfile().getName();
            if (!PlayerUtil.isValidName(name)) continue;
            String prefix = ReplaceUtil.replaceSymbols(team.getPrefix().getString());
            if (StaffManager.getInstance().contains(name) || isStaffPrefix(prefix.toLowerCase())) {
                Status status = Status.ONLINE;
                if (player.getGameMode() == GameMode.SPECTATOR) status = Status.GM3;
                else if (mc.world.getPlayers().stream().anyMatch(p -> p.getGameProfile().getName().equals(name))) status = Status.NEAR;
                staff.add(new Staff(prefix + " " + name, status));
            }
        }
        return staff;
    }

    private List<Staff> getVanishedPlayers() {
        List<Staff> vanished = new ArrayList<>();
        if (mc.world == null || mc.world.getScoreboard() == null || mc.getNetworkHandler() == null) return vanished;
        Set<String> onlineNames = new HashSet<>();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) onlineNames.add(entry.getProfile().getName());
        for (Team team : mc.world.getScoreboard().getTeams()) {
            for (String name : team.getPlayerList()) {
                if (!PlayerUtil.isValidName(name)) continue;
                if (!onlineNames.contains(name)) vanished.add(new Staff(name, Status.VANISH));
            }
        }
        return vanished;
    }

    private boolean isStaffPrefix(String prefix) {
        return (prefix.contains("helper") || prefix.contains("moder") || prefix.contains("admin") || prefix.contains("owner") || prefix.contains("developer") || prefix.contains("staff") || prefix.contains("curator") || prefix.contains("куратор") || prefix.contains("разраб") || prefix.contains("модер") || prefix.contains("админ") || prefix.contains("стажер") || prefix.contains("стажёр") || prefix.contains("хелпер"));
    }
}