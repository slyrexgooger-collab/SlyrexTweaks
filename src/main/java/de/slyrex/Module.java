package de.slyrex;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/** Beschreibung eines Moduls fuer das Slyrex-Menue. */
public final class Module {
	public final String id;
	public final String name;
	public final String description;
	public final ItemStack icon;
	private final BooleanSupplier enabled;
	private final Consumer<Boolean> setEnabled;
	private final Function<Screen, Screen> settings;

	private Module(String id, String name, String description, Item icon, BooleanSupplier enabled,
			Consumer<Boolean> setEnabled, Function<Screen, Screen> settings) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.icon = new ItemStack(icon);
		this.enabled = enabled;
		this.setEnabled = setEnabled;
		this.settings = settings;
	}

	/** Modul, dessen An/Aus in slyrex.json steht. */
	public static Module of(String id, String name, String description, Item icon, Function<Screen, Screen> settings) {
		return new Module(id, name, description, icon, () -> SlyrexConfig.get().isEnabled(id),
				on -> SlyrexConfig.get().setEnabled(id, on), settings);
	}

	/** Modul mit eigener An/Aus-Verwaltung. */
	public static Module custom(String id, String name, String description, Item icon, BooleanSupplier enabled,
			Consumer<Boolean> setEnabled, Function<Screen, Screen> settings) {
		return new Module(id, name, description, icon, enabled, setEnabled, settings);
	}

	public boolean enabled() {
		return enabled.getAsBoolean();
	}

	public void toggle() {
		setEnabled.accept(!enabled());
	}

	public boolean hasSettings() {
		return settings != null;
	}

	public Screen settings(Screen parent) {
		return settings == null ? null : settings.apply(parent);
	}

	public boolean favorite() {
		return SlyrexConfig.get().favorites.contains(id);
	}

	public void toggleFavorite() {
		if (!SlyrexConfig.get().favorites.remove(id)) SlyrexConfig.get().favorites.add(id);
		SlyrexConfig.save();
	}
}
