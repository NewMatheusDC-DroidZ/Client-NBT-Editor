import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtElement;

public class DumpPageNbt {
    public static void main(String[] args) {
        try {
            File f = new File(System.getProperty("user.home"), "AppData/Roaming/ModrinthApp/profiles/1.21.11 True NBT Power/ClientChest/page0.nbt");
            if (!f.exists()) {
                System.out.println("Could not find ClientChest/page0.nbt at " + f.getAbsolutePath());
                return;
            }
            
            System.out.println("Found: " + f.getAbsolutePath());
            try (DataInputStream dis = new DataInputStream(new FileInputStream(f))) {
                NbtElement root = NbtIo.read(dis, net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
                System.out.println(root.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
