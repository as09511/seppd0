/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */


package com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

import java.util.Arrays;
import java.util.HashSet;

//Yesod's information: gained by identifying items, finding secrets and killing new kinds of enemies,
// spent on the abilities of Yesod's scarf
public class InfoPower extends Buff {

	public static final int BASE_MAX = 20;

	public static final int IDENTIFY_INFO = 3;
	public static final int SECRET_TRAP_INFO = 1;
	public static final int SECRET_DOOR_INFO = 3;
	public static final int NEW_ENEMY_INFO = 1;

	public static final int GAIN_COLOR = 0xC090FF;

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	private int info = 0;
	//info held before the vault mirror's scarf filled it up, given back when leaving the vault
	private int vaultSavedInfo = -1;
	//kinds of enemies already killed this run
	private HashSet<String> slainTypes = new HashSet<>();


	public int info(){
		return info;
	}

	public static int max( Hero hero ){
		return BASE_MAX;
	}

	//the hero's info buff, or null if the hero isn't Yesod (or hasn't started collecting yet)
	public static InfoPower get(){
		Hero hero = Dungeon.hero;
		if (hero == null || hero.heroClass != HeroClass.YESOD) return null;
		return hero.buff(InfoPower.class);
	}

	public static int current(){
		InfoPower p = get();
		return p == null ? 0 : p.info;
	}

	public static void gain( int amount ){
		InfoPower p = get();
		if (p == null || amount <= 0) return;
		Hero hero = Dungeon.hero;
		int before = p.info;
		p.info = Math.min(max(hero), p.info + amount);
		int gained = p.info - before;
		if (gained > 0 && hero.sprite != null){
			hero.sprite.showStatus(GAIN_COLOR, Messages.get(InfoPower.class, "gain", gained));
		}
		BuffIndicator.refreshHero();
		Item.updateQuickslot();
	}

	public boolean spend( int amount ){
		if (info < amount) return false;
		info -= amount;
		BuffIndicator.refreshHero();
		Item.updateQuickslot();
		return true;
	}

	//the vault mirror's scarf comes with full info, but only inside the vault
	public void fillForVault(){
		if (vaultSavedInfo < 0) vaultSavedInfo = info;
		info = max((Hero)target);
		BuffIndicator.refreshHero();
		Item.updateQuickslot();
	}

	public void restoreAfterVault(){
		if (vaultSavedInfo >= 0){
			info = vaultSavedInfo;
			vaultSavedInfo = -1;
			BuffIndicator.refreshHero();
			Item.updateQuickslot();
		}
	}

	public static void onItemIdentified(){
		gain(IDENTIFY_INFO);
	}

	public static void onEnemySlain( Mob mob ){
		InfoPower p = get();
		if (p == null) return;
		if (p.slainTypes.add(mob.getClass().getName())){
			gain(NEW_ENEMY_INFO);
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.INFO_POWER;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(info);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", info, max((Hero)target));
	}

	private static final String INFO = "info";
	private static final String SLAIN = "slain_types";
	private static final String VAULT_SAVED = "vault_saved_info";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(INFO, info);
		bundle.put(SLAIN, slainTypes.toArray(new String[0]));
		bundle.put(VAULT_SAVED, vaultSavedInfo);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		info = bundle.getInt(INFO);
		vaultSavedInfo = bundle.contains(VAULT_SAVED) ? bundle.getInt(VAULT_SAVED) : -1;
		slainTypes.clear();
		if (bundle.contains(SLAIN)){
			slainTypes.addAll(Arrays.asList(bundle.getStringArray(SLAIN)));
		}
	}
}
