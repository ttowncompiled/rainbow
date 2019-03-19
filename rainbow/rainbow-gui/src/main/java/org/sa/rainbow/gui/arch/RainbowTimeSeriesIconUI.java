package org.sa.rainbow.gui.arch;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.LayoutManager;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicDesktopIconUI;

import org.sa.rainbow.gui.widgets.TimeSeriesPanel;

public class RainbowTimeSeriesIconUI extends BasicDesktopIconUI {
	
	private Object series;

	public RainbowTimeSeriesIconUI(TimeSeriesPanel panel) {
		this.series = panel;
		
		frame = desktopIcon.getInternalFrame();
		String title = frame.getTitle();
		JPanel p = new JPanel();
		desktopIcon.setBorder(null);
		desktopIcon.setOpaque(false);
		desktopIcon.setLayout(new BorderLayout());
		desktopIcon.add(panel, BorderLayout.CENTER);
		JLabel l = new JLabel(title, SwingConstants.CENTER);
		desktopIcon.add(l, BorderLayout.SOUTH);
	}
	
	@Override
	protected void uninstallComponents() {
		desktopIcon.setLayout(null);
		desktopIcon.removeAll();
		frame = null;
	}
	
	public java.awt.Dimension getMinimumSize(javax.swing.JComponent c) {
		LayoutManager layout = desktopIcon.getLayout();
		Dimension size = layout.minimumLayoutSize(desktopIcon);
		return new Dimension(size.width + 15, size.height + 15);
	}
	
	@Override
	public Dimension getPreferredSize(JComponent c) {
		return getMinimumSize(c);
	}

	@Override
	public Dimension getMaximumSize(JComponent c) {
		return getMinimumSize(c);
	}
}
