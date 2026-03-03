package net.axther.serverCore.pet.profiles;

import net.axther.serverCore.pet.PetAnimationType;
import net.axther.serverCore.pet.PetProfile;
import net.axther.serverCore.pet.PetSound;
import net.axther.serverCore.pet.util.HeadUtil;
import org.bukkit.Sound;

import java.util.Collections;
import java.util.List;

public class RatPetProfile extends PetProfile {

    private static final String TEXTURE = "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjE1Yzk1YjMyOTI5MDliZjg1NjRmYmZmMTg1M2Q2MzMyOGJhN2MyOWVmMTZiMGM2MmU0ZDViM2ZjMjQ4NDYwZSJ9fX0=";

    public RatPetProfile() {
        super("rat", "Rat", HeadUtil.fromTexture(TEXTURE),
                "<gradient:#8B4513:#D2691E><bold>Rat Pet</bold></gradient>",
                List.of(
                        "<gray>A loyal little rodent companion.",
                        "",
                        "<dark_gray>Attacks nearby hostile mobs",
                        "<dark_gray>and squeaks with excitement!",
                        "",
                        "<yellow>Right-click <gray>to summon/dismiss"
                ),
                0.12, 0.1, 1.0,
                0.28, 3.0, 2.0, 16.0,
                true, 5.0, 1.5, 25,
                600, 4, true,
                PetAnimationType.FLOAT,
                List.of(
                        new PetSound(Sound.ENTITY_SILVERFISH_AMBIENT, 0.5f, 1.8f, 200)
                ),
                null, Collections.emptyMap());
    }
}
