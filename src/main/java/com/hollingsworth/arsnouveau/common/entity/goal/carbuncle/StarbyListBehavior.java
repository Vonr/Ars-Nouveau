package com.hollingsworth.arsnouveau.common.entity.goal.carbuncle;

import com.hollingsworth.arsnouveau.api.util.NBTUtil;
import com.hollingsworth.arsnouveau.client.particle.ColorPos;
import com.hollingsworth.arsnouveau.client.particle.ParticleColor;
import com.hollingsworth.arsnouveau.common.entity.Starbuncle;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.*;

public abstract class StarbyListBehavior extends StarbyBehavior {
    public record Data(List<DirectionalBlockPos> from, List<DirectionalBlockPos> to) {
        public static final MapCodec<Data> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.list(DirectionalBlockPos.CODEC).fieldOf("from").forGetter(Data::from),
                Codec.list(DirectionalBlockPos.CODEC).fieldOf("to").forGetter(Data::to)
        ).apply(instance, Data::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                DirectionalBlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), Data::from,
                DirectionalBlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), Data::to,
                Data::new
        );
    }

    public record DirectionalBlockPos(BlockPos pos, @Nullable Direction direction) {
        public static final Codec<DirectionalBlockPos> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(DirectionalBlockPos::pos),
                Direction.CODEC.optionalFieldOf("direction").forGetter(p -> Optional.ofNullable(p.direction))
        ).apply(instance, (pos, dir) -> new DirectionalBlockPos(pos, dir.orElse(null))));

        public static final StreamCodec<RegistryFriendlyByteBuf, DirectionalBlockPos> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, DirectionalBlockPos::pos,
                Direction.STREAM_CODEC.apply(ByteBufCodecs::optional), p -> Optional.ofNullable(p.direction),
                (pos, dir) -> new DirectionalBlockPos(pos, dir.orElse(null))
        );
    }

    public List<DirectionalBlockPos> from = null;
    public List<DirectionalBlockPos> to = null;

    /**
     * @deprecated Use {@link from}
     */
    @Deprecated
    public List<BlockPos> FROM_LIST = new ArrayList<>();

    /**
     * @deprecated Use {@link to}
     */
    @Deprecated
    public List<BlockPos> TO_LIST = new ArrayList<>();

    /**
     * @deprecated Use {@link from}
     */
    @Deprecated
    public Map<Integer, Direction> FROM_DIRECTION_MAP = new HashMap<>();
    /**
     * @deprecated Use {@link to}
     */
    @Deprecated
    public Map<Integer, Direction> TO_DIRECTION_MAP = new HashMap<>();

    public StarbyListBehavior(Starbuncle entity, CompoundTag tag) {
        super(entity, tag);
        int counter = 0;

        var decoded = Data.CODEC.decoder().decode(NbtOps.INSTANCE, tag);
        if (decoded.isSuccess()) {
            var data = decoded.getOrThrow().getFirst();
            this.setFromData(data);
            return;
        }

        while (NBTUtil.hasBlockPos(tag, "from_" + counter)) {
            BlockPos pos = NBTUtil.getBlockPos(tag, "from_" + counter);
            if (!this.FROM_LIST.contains(pos))
                this.FROM_LIST.add(pos);
            counter++;
        }

        counter = 0;
        while (NBTUtil.hasBlockPos(tag, "to_" + counter)) {
            BlockPos pos = NBTUtil.getBlockPos(tag, "to_" + counter);
            if (!this.TO_LIST.contains(pos))
                this.TO_LIST.add(pos);
            counter++;
        }

        for (String key : tag.getAllKeys()) {
            if (key.startsWith("from_direction_")) {
                int hash = Integer.parseInt(key.substring(15));
                FROM_DIRECTION_MAP.put(hash, Direction.from3DDataValue(tag.getInt(key)));
            }
            if (key.startsWith("to_direction_")) {
                int hash = Integer.parseInt(key.substring(13));
                TO_DIRECTION_MAP.put(hash, Direction.from3DDataValue(tag.getInt(key)));
            }
        }

        this.setFromData(this.getData());
    }

    @Override
    public boolean clearOrRemove() {
        return from.isEmpty() && to.isEmpty();
    }

    @Override
    public void onWanded(Player playerEntity) {
        super.onWanded(playerEntity);
        this.setFromData(new Data(new ArrayList<>(), new ArrayList<>()));
        PortUtil.sendMessage(playerEntity, Component.translatable("ars_nouveau.connections.cleared"));
        syncTag();
    }

    @Override
    public List<ColorPos> getWandHighlight(List<ColorPos> list) {
        for (var e : to) {
            list.add(ColorPos.centered(e.pos, ParticleColor.TO_HIGHLIGHT));
        }
        for (var e : from) {
            list.add(ColorPos.centered(e.pos, ParticleColor.FROM_HIGHLIGHT));
        }
        return list;
    }

    public void addFromPos(BlockPos fromPos) {
        var pd = new DirectionalBlockPos(fromPos.immutable(), null);
        if (!from.contains(pd)) {
            from.add(pd);
            FROM_LIST.add(pd.pos);
            syncTag();
        }
    }

    public void addToPos(BlockPos toPos) {
        var pd = new DirectionalBlockPos(toPos.immutable(), null);
        if (!to.contains(pd)) {
            to.add(pd);
            TO_LIST.add(toPos.immutable());
            syncTag();
        }
    }

    public void addFromPos(BlockPos fromPos, Direction direction) {
        var pd = new DirectionalBlockPos(fromPos.immutable(), direction);
        if (!from.contains(pd)) {
            from.add(pd);
            FROM_LIST.add(pd.pos);
            FROM_DIRECTION_MAP.put(pd.pos.hashCode(), direction);
            syncTag();
        }
    }

    public void addToPos(BlockPos toPos, Direction direction) {
        var pd = new DirectionalBlockPos(toPos.immutable(), direction);
        if (!to.contains(pd)) {
            to.add(pd);
            TO_LIST.add(pd.pos);
            TO_DIRECTION_MAP.put(pd.pos.hashCode(), direction);
            syncTag();
        }
    }

    public Data getData() {
        if (this.from != null && this.to != null) {
            return new Data(this.from, this.to);
        }

        List<DirectionalBlockPos> from = new ArrayList<>(this.FROM_LIST.size());
        for (int i = 0; i < this.FROM_LIST.size(); i++) {
            from.add(new DirectionalBlockPos(this.FROM_LIST.get(i), this.FROM_DIRECTION_MAP.get(i)));
        }
        
        List<DirectionalBlockPos> to = new ArrayList<>(this.TO_LIST.size());
        for (int i = 0; i < this.TO_LIST.size(); i++) {
            to.add(new DirectionalBlockPos(this.TO_LIST.get(i), this.TO_DIRECTION_MAP.get(i)));
        }
        
        return new Data(from, to);
    }

    public void setFromData(Data data) {
        this.from = data.from;
        this.to = data.to;

        this.FROM_LIST.clear();;
        this.TO_LIST.clear();;
        this.FROM_DIRECTION_MAP.clear();
        this.TO_DIRECTION_MAP.clear();

        for (int i = 0; i < data.from.size(); i++) {
            var pd = data.from.get(i);
            this.FROM_LIST.add(pd.pos);
            if (pd.direction != null) {
                this.FROM_DIRECTION_MAP.put(i, pd.direction);
            }
        }

        for (int i = 0; i < data.to.size(); i++) {
            var pd = data.to.get(i);
            this.TO_LIST.add(pd.pos);
            if (pd.direction != null) {
                this.TO_DIRECTION_MAP.put(i, pd.direction);
            }
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        Data.CODEC.encoder().encode(this.getData(), NbtOps.INSTANCE, tag);
        return super.toTag(tag);
    }
}
