package python.reload.api.system;

import eu.donyka.discord.RPCHandler;
import eu.donyka.discord.discord.RichPresence;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import python.reload.api.system.backend.ClientInfo;
import python.reload.api.system.interfaces.QuickImports;

@UtilityClass
public class DiscordHook implements QuickImports {
    @SneakyThrows
    public void startRPC() {
        RPCHandler.setOnReady(user -> {
            RichPresence presence = RichPresence.builder()
                    .details("Version: " + ClientInfo.VERSION)
                    .details("user:dev")
                    .largeImageKey("https://media1.tenor.com/m/mXOYUoT4of4AAAAC/nextrix-extorted.gif")
                    .largeImageText("the best bypa$$")
                    .smallImageKey("https://media1.tenor.com/m/PLIr_VkF6ywAAAAd/ghostedvpn-hacker-cat.gif")
                    .smallImageText("$elfcode$")
                    
                    .button("Bio","https://en.wikipedia.org/wiki/Vibe_coding")
                    .startTimestamp(0)
                    .endTimestamp(0)
                    .build();
            RPCHandler.updatePresence(presence);
        });

        RPCHandler.setOnDisconnected(error -> {
            System.out.println("RPC Disconnected: " + error);
        });

        RPCHandler.setOnErrored(error -> {
            System.out.println("RPC Errored: " + error);
        });

        RPCHandler.startup("1475161710259077120", false);
    }

    public void stopRPC() {
        RPCHandler.shutdown();
    }
}
