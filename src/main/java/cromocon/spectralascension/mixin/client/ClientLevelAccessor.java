package cromocon.spectralascension.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** ClientLevel.getBlockStatePredictionHandler è package-private: serve a WorkstaffAreaPrediction per sapere se c'è una previsione aperta. */
@Mixin(ClientLevel.class)
public interface ClientLevelAccessor {
    @Invoker("getBlockStatePredictionHandler")
    BlockStatePredictionHandler spectral_ascension$predictionHandler();
}
