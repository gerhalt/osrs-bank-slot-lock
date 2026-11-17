package com.bankslotlock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/** Draws a small padlock in a corner of locked bank slots. */
class BankSlotLockOverlay extends WidgetItemOverlay
{
	/** Padlock size at 100% scale, in pixels. */
	private static final int ICON_WIDTH = 9;
	private static final int ICON_HEIGHT = 11;
	/** Gap between the padlock and the edge of the slot. */
	private static final int MARGIN = 1;
	/** How far the shadow sits below and to the right of the padlock, in screen pixels. */
	private static final int SHADOW_OFFSET = 1;

	private final BankSlotLockPlugin plugin;
	private final BankSlotLockConfig config;

	private final BasicStroke shackleStroke = new BasicStroke(1.5f);

	@Inject
	BankSlotLockOverlay(BankSlotLockPlugin plugin, BankSlotLockConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		showOnBank();
	}

	@Override
	public void renderItemOverlay(Graphics2D g, int itemId, WidgetItem widgetItem)
	{
		if (!config.showIcon() || !plugin.isLocked(itemId))
		{
			return;
		}

		double scale = config.iconScale() / 100.0;
		double w = ICON_WIDTH * scale;
		double h = ICON_HEIGHT * scale;

		Rectangle b = widgetItem.getCanvasBounds();
		double x;
		double y;
		switch (config.iconPosition())
		{
			case TOP_RIGHT:
				x = b.x + b.width - w - MARGIN;
				y = b.y + MARGIN;
				break;
			case BOTTOM_RIGHT:
				x = b.x + b.width - w - MARGIN;
				y = b.y + b.height - h - MARGIN;
				break;
			case BOTTOM_LEFT:
			default:
				x = b.x + MARGIN;
				y = b.y + b.height - h - MARGIN;
				break;
		}

		Color color = config.iconColor();
		Color shadow = new Color(0, 0, 0, color.getAlpha());

		Object savedAa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		try
		{
			// Shadow 1 screen pixel down and right, like RuneLite's overlay text.
			drawPadlock(g, x + SHADOW_OFFSET, y + SHADOW_OFFSET, scale, shadow);
			drawPadlock(g, x, y, scale, color);
		}
		finally
		{
			if (savedAa != null)
			{
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedAa);
			}
		}
	}

	private void drawPadlock(Graphics2D g, double x, double y, double scale, Color color)
	{
		AffineTransform saved = g.getTransform();
		try
		{
			g.translate(x, y);
			g.scale(scale, scale);
			g.setColor(color);

			// Shackle
			g.setStroke(shackleStroke);
			g.drawArc(1, 0, 6, 7, 0, 180);
			g.drawLine(1, 3, 1, 5);
			g.drawLine(7, 3, 7, 5);
			// Body
			g.fillRect(0, 5, ICON_WIDTH, 6);
		}
		finally
		{
			g.setTransform(saved);
		}
	}
}
