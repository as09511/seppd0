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

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ego.Suit;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.Baton;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.Beak;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.FragmentsFromSomewhere;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.FrostSplinter;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.Laetitia;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.LifeForADaredevil;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.Logging;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.MagicBullet;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.Penitence;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.RedEyes;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.Regret;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.SolemnLament;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.Hornet;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.Echo;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.SwordSharpenedWithTears;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.InTheNameOfLoveAndHate;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.Diffraction;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.Lamp;

//every E.G.O equipment in the mod, add new E.G.O here
public class EgoCatalog {

	@SuppressWarnings("unchecked")
	public static final Class<? extends Item>[] ALL = new Class[]{
			Baton.class,
			Suit.class,
			Penitence.class,
			LifeForADaredevil.class,
			MagicBullet.class,
			RedEyes.class,
			Beak.class,
			FragmentsFromSomewhere.class,
			Regret.class,
			FrostSplinter.class,
			Logging.class,
			Laetitia.class,
			SolemnLament.class,
			Hornet.class,
			Echo.class,
			SwordSharpenedWithTears.class,
			InTheNameOfLoveAndHate.class,
			Diffraction.class,
			Lamp.class
	};

}
