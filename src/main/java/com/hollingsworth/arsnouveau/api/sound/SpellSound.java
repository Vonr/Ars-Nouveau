package com.hollingsworth.arsnouveau.api.sound;

import com.hollingsworth.arsnouveau.api.registry.SpellSoundRegistry;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import javax.annotation.Nullable;
import java.util.Objects;

public class SpellSound {

    public static MapCodec<SpellSound> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(s -> s.id),
            SoundEvent.CODEC.fieldOf("soundEvent").forGetter(s -> s.soundEvent),
            ComponentSerialization.CODEC.fieldOf("soundName").forGetter(s -> s.soundName)
    ).apply(instance, SpellSound::new));

    private ResourceLocation id;

    private Holder<SoundEvent> soundEvent;

    private Component soundName;

    public SpellSound(ResourceLocation id, Holder<SoundEvent> soundEvent, Component soundName) {
        this.id = id;
        this.soundEvent = soundEvent;
        this.soundName = soundName;
    }

    public SpellSound(Holder<SoundEvent> soundEvent, Component soundName) {
        this(soundEvent.unwrapKey().get().location(), soundEvent, soundName);
    }

    public ResourceLocation getId() {
        return id;
    }

    public void setId(ResourceLocation id) {
        this.id = id;
    }

    public Holder<SoundEvent> getSoundEvent() {
        return soundEvent;
    }

    public void setSoundEvent(Holder<SoundEvent> soundEvent) {
        this.soundEvent = soundEvent;
    }

    public Component getSoundName() {
        return soundName;
    }

    public void setSoundName(Component soundName) {
        this.soundName = soundName;
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id.toString());
        return tag;
    }

    public static @Nullable SpellSound fromTag(CompoundTag tag) {
        if (!tag.contains("id"))
            return null;

        ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
        return SpellSoundRegistry.getSpellSoundsRegistry().get(id);
    }

    public ResourceLocation getTexturePath() {
        return ResourceLocation.fromNamespaceAndPath(this.id.getNamespace(), "textures/sounds/" + this.id.getPath() + ".png");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpellSound that = (SpellSound) o;
        return Objects.equals(id, that.id) && Objects.equals(soundEvent, that.soundEvent) && Objects.equals(soundName, that.soundName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, soundEvent, soundName);
    }

    @Override
    public String toString() {
        return "SpellSound{" +
                "id=" + id +
                ", soundEvent=" + soundEvent +
                ", soundName=" + soundName +
                '}';
    }
}
