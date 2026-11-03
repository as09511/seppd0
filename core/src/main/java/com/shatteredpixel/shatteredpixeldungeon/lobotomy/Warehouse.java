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


package com.shatteredpixel.shatteredpixeldungeon.lobotomy;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ego.EgoArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.EgoWeapon;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.IOException;

//the warehouse: resources kept outside of any one game and shared by all of them.
//For now it only holds enkephalin boxes, which Angela takes in exchange for her help.
public class Warehouse {

	public static final String WAREHOUSE_FILE = "warehouse.dat";

	private static final String ENKEPHALIN = "enkephalin";

	private static boolean loaded = false;
	private static int enkephalin = 0;

	private static void load(){
		if (loaded) return;
		try {
			Bundle bundle = FileUtils.bundleFromFile( WAREHOUSE_FILE );
			enkephalin = Math.max(0, bundle.getInt( ENKEPHALIN ));
		} catch (IOException e){
			enkephalin = 0;
		}
		loaded = true;
	}

	private static void save(){
		Bundle bundle = new Bundle();
		bundle.put( ENKEPHALIN, enkephalin );
		try {
			FileUtils.bundleToFile( WAREHOUSE_FILE, bundle );
		} catch (IOException e){
			ShatteredPixelDungeon.reportException(e);
		}
	}

	public static int enkephalin(){
		load();
		return enkephalin;
	}

	public static void addEnkephalin( int amount ){
		load();
		enkephalin = Math.max(0, enkephalin + amount);
		save();
	}

	//returns false and changes nothing if there are not enough boxes
	public static boolean spendEnkephalin( int amount ){
		load();
		if (enkephalin < amount) return false;
		enkephalin -= amount;
		save();
		return true;
	}

	public static RiskLevel risk( Item item ){
		if (item instanceof EgoWeapon) return ((EgoWeapon) item).risk();
		if (item instanceof EgoArmor) return ((EgoArmor) item).risk();
		return null;
	}

	//enkephalin boxes Angela asks to extract an E.G.O of this risk level
	public static int extractCost( RiskLevel risk ){
		switch (risk){
			case ZAYIN: default: return 20;
			case TETH:  return 20;
			case HE:    return 40;
			case WAW:   return 70;
			case ALEPH: return 100;
		}
	}

	//refining (one upgrade) costs half of the extraction
	public static int refineCost( RiskLevel risk ){
		return extractCost(risk) / 2;
	}
}
