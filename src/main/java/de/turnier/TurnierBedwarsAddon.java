package de.turnier;

import de.marcely.bedwars.api.BedwarsAddon;

/**
 * Registriert Turnier über den vorgesehenen MBedwars-Addon-Mechanismus.
 *
 * Die Klasse wird nur verwendet, wenn MBedwars auf dem Server installiert ist.
 */
public final class TurnierBedwarsAddon extends BedwarsAddon {

    public TurnierBedwarsAddon(TurnierPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "Turnier";
    }
}
