package cromocon.spectralascension.gametest;

import com.mojang.authlib.GameProfile;
import cromocon.spectralascension.SpectralAscension;
import de.dafuqs.spectrum.api.item.Preenchanted;
import de.dafuqs.spectrum.registries.SpectrumAdvancements;
import de.dafuqs.spectrum.registries.SpectrumItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Attrezzi comuni ai GameTest delle classi nuove (SAFunctionTests, SAEdgeCaseTests): giocatore
 * finto, mira, armatura, Ink e pulizia. Gli stessi aiuti di SAGameTests, resi condivisi.
 */
public final class SATestUtil {
    private SATestUtil() {
    }

    /** Giocatore finto in sopravvivenza con una connessione che scarta i pacchetti (vedi SAGameTests.mockPlayer). */
    public static ServerPlayer mockPlayer(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "sa-test-player");
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, CommonListenerCookie.createInitial(profile, false)) {
            @Override
            public void send(Packet<?> packet) {
            }

            @Override
            public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
            }
        };
        // Un ServerPlayer appena creato è invulnerabile per 60 tick (spawnInvulnerableTime, campo privato): i test
        // che lo colpiscono lo vogliono vulnerabile subito.
        try {
            Field f = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            f.setAccessible(true);
            f.setInt(player, 0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return player;
    }

    /** Posizione e mira, testa compresa (getViewVector usa yHeadRot). */
    public static void aim(ServerPlayer player, BlockPos feet, float yaw, float pitch) {
        player.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yaw, pitch);
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
    }

    /** Mette o toglie un pezzo e avvisa i listener come farebbe il tick del giocatore. */
    public static void equip(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        ItemStack from = entity.getItemBySlot(slot).copy();
        entity.setItemSlot(slot, stack);
        NeoForge.EVENT_BUS.post(new LivingEquipmentChangeEvent(entity, slot, from, stack));
    }

    /** Lo stack con gli incantesimi con cui esce dalla ricetta (quelli preimpostati). */
    public static ItemStack enchanted(GameTestHelper helper, Item item) {
        ItemStack stack = new ItemStack(item);
        if (item instanceof Preenchanted p) {
            stack.set(DataComponents.ENCHANTMENTS, Preenchanted.buildDefaultEnchantments(helper.getLevel().registryAccess(), p));
        }
        return stack;
    }

    /** Concede spectrum:milestones/unlock_ink_use (serve a InkPowered.tryDrainEnergy in sopravvivenza). */
    public static void unlockInk(GameTestHelper helper, ServerPlayer player) {
        var inkUse = helper.getLevel().getServer().getAdvancements().get(SpectrumAdvancements.UNLOCK_INK_USE);
        helper.assertTrue(inkUse != null, "advancement mancante: " + SpectrumAdvancements.UNLOCK_INK_USE);
        for (String c : inkUse.value().criteria().keySet()) player.getAdvancements().award(inkUse, c);
    }

    /** Pigment Palette piena (409 600 Ink per colore). */
    public static ItemStack fullPalette() {
        return SpectrumItems.PIGMENT_PALETTE.get().getFullStack();
    }

    /** Barra d'attacco carica (come se il giocatore avesse aspettato): serve a sweep e colpi critici. */
    public static void chargeAttack(Player player) {
        try {
            Field f = LivingEntity.class.getDeclaredField("attackStrengthTicker");
            f.setAccessible(true);
            f.setInt(player, 1000);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Toglie le entità non giocatore attorno a un punto (drop, frecce, mob dei test). */
    public static void clearAround(GameTestHelper helper, BlockPos pos, double radius) {
        helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(pos).inflate(radius), e -> !(e instanceof Player)).forEach(Entity::discard);
    }

    public static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    /** Fallisce con tutti i problemi (uno per riga nel log) oppure riesce scrivendo il riassunto. */
    public static void finish(GameTestHelper helper, String name, List<String> problems, List<String> report) {
        report.forEach(r -> SpectralAscension.LOGGER.info("[GameTest] {}: {}", name, r));
        if (!problems.isEmpty()) {
            problems.forEach(p -> SpectralAscension.LOGGER.error("[GameTest] {}: {}", name, p));
            helper.fail(problems.size() + " problemi, il primo: " + problems.getFirst());
            return;
        }
        helper.succeed();
    }
}
