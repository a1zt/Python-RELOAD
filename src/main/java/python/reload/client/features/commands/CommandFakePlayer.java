package python.reload.client.features.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import python.reload.api.command.Command;
import python.reload.api.command.CommandRegister;
import python.reload.api.system.client.FakePlayerManager;

@CommandRegister(name = "fakeplayer")
public class CommandFakePlayer extends Command {

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("delete").executes(c -> {
            String removedName = FakePlayerManager.getInstance().removeLast();

            if (removedName == null) {
                print("Список фейк-плееров пуст.");
            } else {
                print("Последний фейк-плеер (" + removedName + ") успешно удален!");
            }
            return 1;
        }));


        builder.then(literal("clear").executes(c -> {
            FakePlayerManager.getInstance().clear();
            print("Все фейк-плееры были удалены.");
            return 1;
        }));

        builder.then(literal("add").then(argument("name", StringArgumentType.word()).executes(c -> {
            String name = StringArgumentType.getString(c, "name");

            if (FakePlayerManager.getInstance().add(name)) {
                print("Фейк-плеер заспавнен: " + name);
            } else {
                print("Игрок с именем " + name + " уже существует или мир не загружен.");
            }

            return 1;
        })));
    }
}