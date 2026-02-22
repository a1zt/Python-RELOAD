package sweetie.evaware.client.ui.widget.overlay;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.world.GameMode;
import sweetie.evaware.api.system.configs.StaffManager;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.framelimiter.FrameLimiter;
import sweetie.evaware.api.utils.other.ReplaceUtil;
import sweetie.evaware.api.utils.player.PlayerUtil;
import sweetie.evaware.client.ui.widget.ContainerWidget;

import java.awt.*;
import java.util.*;
import java.util.List;

public class StaffsWidget extends ContainerWidget {
    private final FrameLimiter frameLimiter = new FrameLimiter(false);
    private List<Staff> cacheStaffs = new ArrayList<>();

    public record Staff(String name, Status status) {}

    @Getter
    @RequiredArgsConstructor
    public enum Status {
        ONLINE("Online"),
        NEAR("Near"),
        GM3("Gm3"),
        VANISH("Vanish");

        private final String label;
    }

    @Override
    public String getName() {
        return "Staffs";
    }

    public StaffsWidget() {
        super(100f, 100f);
    }

    @Override
    protected Map<String, ContainerElement.ColoredString> getCurrentData() {
        Map<String, ContainerElement.ColoredString> map = new HashMap<>();

        for (Staff staff : getStaffList()) {
            Color color = switch (staff.status()) {
                case ONLINE -> UIColors.positiveColor();
                case NEAR -> UIColors.middleColor();
                case GM3, VANISH -> UIColors.negativeColor();
            };

            String label = staff.status().getLabel();

            map.put(staff.name(), new ContainerElement.ColoredString(label, color));
        }
        return map;
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

                if (player.getGameMode() == GameMode.SPECTATOR) {
                    status = Status.GM3;
                } else if (mc.world.getPlayers().stream().anyMatch(p -> p.getGameProfile().getName().equals(name))) {
                    status = Status.NEAR;
                }

                staff.add(new Staff(prefix + " " + name, status));
            }
        }
        return staff;
    }

    private List<Staff> getVanishedPlayers() {
        List<Staff> vanished = new ArrayList<>();
        if (mc.world == null || mc.world.getScoreboard() == null || mc.getNetworkHandler() == null)
            return vanished;

        Set<String> onlineNames = new HashSet<>();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            onlineNames.add(entry.getProfile().getName());
        }

        for (Team team : mc.world.getScoreboard().getTeams()) {
            for (String name : team.getPlayerList()) {
                if (!PlayerUtil.isValidName(name)) continue;
                if (!onlineNames.contains(name)) {
                    vanished.add(new Staff(name, Status.VANISH));
                }
            }
        }

        return vanished;
    }

    private boolean isStaffPrefix(String prefix) {
        return (
                        prefix.contains("helper") ||
                        prefix.contains("moder") ||
                        prefix.contains("admin") ||
                        prefix.contains("owner") ||
                        prefix.contains("developer") ||
                        prefix.contains("staff") ||
                        prefix.contains("curator") ||

                        prefix.contains("куратор") ||
                        prefix.contains("разраб") ||
                        prefix.contains("модер") ||
                        prefix.contains("админ") ||
                        prefix.contains("стажер") ||
                        prefix.contains("стажёр") ||
                        prefix.contains("хелпер")
                );
    }
}
