/*
 * Copyright 2012, 2013 Nicolas HERVE
 * 
 * This file is part of BASToD.
 * 
 * BASToD is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * BASToD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with BASToD. If not, see <http://www.gnu.org/licenses/>.
 */
package name.herve.bastod.gui.components;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import name.herve.bastod.engine.Engine;
import name.herve.bastod.engine.Player;
import name.herve.bastod.guifwk.AbstractComponent;
import name.herve.bastod.guifwk.GUIResources;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;

// TODO verifier problème quand speed != 1
/**
 * @author Nicolas HERVE - n.herve@laposte.net
 */
public class MetalBar extends AbstractComponent {
	private DecimalFormat df;
	private Player player;
	private String strDeltaA;
	private String strDeltaR;
	private String strMetal;
	private Engine engine;

	public MetalBar(Engine engine, Player player, int x, int y, int w, int h) {
		super("metalbar-" + player.getColor(), x, y, w, h);

		this.player = player;
		this.engine = engine;
		strMetal = null;
		strDeltaA = null;
		strDeltaR = null;

		df = new DecimalFormat("+0.00;-0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	}

	@Override
	public void drawText() {
		drawCentered(GUIResources.getInstance().getFont(player.getColor()), strMetal);
		
		BitmapFont font = GUIResources.getInstance().getFont(GUIResources.FONT_SMALL_WHITE);
		TextBounds b = font.getBounds(strDeltaA);
		draw(font, strDeltaA, getX() + getWidth() - b.width - 4, getY() + getHeight() - b.height - 4);
		b = font.getBounds(strDeltaR);
		draw(font, strDeltaR, getX() + getWidth() - b.width - 4, getY() +  4);
		
		//draw(, strMetal, getX() + getWidth() / 2 - 20, getY() + halfHeight + halfHeight / 2 + 7);
		//draw(GUIResources.getInstance().getFont(GUIResources.FONT_STANDARD_WHITE), strDelta, getX() + getWidth() / 2 - 10, getY() + halfHeight / 2 + 7);
	}

	@Override
	public void stop() {
		super.stop();
		
		disposeComponent();
	}

	@Override
	public Texture updateComponent() {
		Blending bck = Pixmap.getBlending();
		Pixmap.setBlending(Blending.None);

		Pixmap p = new Pixmap(getWidth(), getHeight(), Pixmap.Format.RGBA8888);
		Color c1 = GUIResources.getInstance().getColor(player.getColor()).cpy();
		c1.a = 1f;
		p.setColor(c1);

		p.drawRectangle(0, 0, getWidth(), getHeight());

		c1.a = 0.5f;
		p.setColor(c1);
		int w = player.getMetal() * (getWidth() - 2) / player.getMaxMetal();
		p.fillRectangle(1, 1, w, getHeight() - 2);

		strMetal = "$ " + player.getMetal();
		strDeltaA = df.format(player.getMetalAddedMean(engine.getNow()));
		strDeltaR = df.format(-1 * player.getMetalRemovedMean(engine.getNow()));

		Texture t = new Texture(p);
		p.dispose();
		Pixmap.setBlending(bck);

		return t;
	}
}
