package cromocon.spectralascension.item;

import com.mojang.serialization.Codec;
import cromocon.spectralascension.SpectralAscension;
import cromocon.spectralascension.registry.SAItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Scia colorata delle frecce del T9 (arco e balestra Prismatic): SOLO grafica, non tocca danni, traiettoria o fisica.
 * <p>
 * Come funziona:
 * <ol>
 *   <li><b>Marcatore sul server</b>: quando una freccia ({@link AbstractArrow}: normale, tipped, spettrale, di vetro di
 *       Spectrum, una per ogni freccia del Multishot) entra nel mondo e l'arma che l'ha tirata
 *       ({@link AbstractArrow#getWeaponItem()}) è l'arco o la balestra Prismatic, riceve il data attachment
 *       {@link #MARKER} (salvato con l'entità, quindi resta dopo un riavvio).</li>
 *   <li><b>Al client</b>: l'arma non arriva al client, il marcatore sì. {@code ServerEntityMixin} aggiunge ai pacchetti di
 *       comparsa della freccia (stesso bundle, dopo AddEntity) un {@code PrismaticTrailPayload}, che sul client mette lo
 *       stesso attachment sull'entità. Vale per ogni giocatore che comincia a vedere la freccia, anche su server dedicato.</li>
 *   <li><b>Particelle</b>: {@code client.PrismaticTrailParticles} a ogni tick del client lascia scintille lungo il tratto
 *       percorso dalla freccia, solo finché si muove (piantata a terra non emette più).</li>
 * </ol>
 * GameTest {@code SAFunctionTests.prismatic_trail_marker}; scenario client {@code scia_frecce}.
 */
public final class PrismaticTrail {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SpectralAscension.MOD_ID);

    /** true = la freccia lascia la scia. Salvato solo se vero. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> MARKER = ATTACHMENTS.register("prismatic_trail",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL, marked -> marked).build());

    private PrismaticTrail() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    /** L'arma che ha tirato la freccia è il nostro arco o la nostra balestra del T9? */
    public static boolean firedFromPrismatic(AbstractArrow arrow) {
        ItemStack weapon = arrow.getWeaponItem();
        return weapon != null && (weapon.is(SAItems.PRISMATIC_BOW.get()) || weapon.is(SAItems.PRISMATIC_CROSSBOW.get()));
    }

    public static boolean isMarked(Entity entity) {
        return entity instanceof AbstractArrow && entity.hasData(MARKER) && entity.getData(MARKER);
    }

    /** Game bus, solo server: la freccia entra nel mondo (prima dei pacchetti di comparsa ai giocatori). */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!arrow.hasData(MARKER) && firedFromPrismatic(arrow)) {
            arrow.setData(MARKER, true);
        }
    }
}
