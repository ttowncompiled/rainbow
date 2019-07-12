package org.sa.rainbow.stitch.gui.executor;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;

import org.apache.commons.lang.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.sa.rainbow.stitch.core.Tactic;
import org.sa.rainbow.stitch.gui.executor.StrategyCodeExecutionTracer.DurationCountdownTask;
import org.sa.rainbow.stitch.gui.executor.StrategyCodeExecutionTracer.StrategyDurationCountdownTask;
import org.sa.rainbow.stitch.gui.executor.StrategyExecutionPanel.StrategyInstanceData;
import org.sa.rainbow.stitch.gui.executor.StrategyExecutionPanel.TraceData;
import org.sa.rainbow.stitch.util.ExecutionHistoryData.ExecutionStateT;

public class StrategyCodeExecutionTracer extends RSyntaxTextArea {

	

	protected static final Map<String, String> PATH_TO_STITCHTEXT = new HashMap<>();
	private static String SETTLING_STRING = "\u2026";

	public static Color bleach(Color color, double amount) {
		int red = (int) ((color.getRed() * (1 - amount) / 255 + amount) * 255);
		int green = (int) ((color.getGreen() * (1 - amount) / 255 + amount) * 255);
		int blue = (int) ((color.getBlue() * (1 - amount) / 255 + amount) * 255);
		return new Color(red, green, blue);
	}

	public static final Color EXECUTING_COLOR = bleach(Color.GREEN, .5);
	public static final Color SETTLING_COLOR = bleach(Color.YELLOW, 0.5);
	public static final Color ERROR_COLOR = bleach(Color.RED, 0.5);

	protected static class DurationCountdownTask extends TimerTask {

		protected StrategyCodeExecutionTracer m_textArea;
		protected int m_startLocationInTextEditor;
		protected int m_endLocationInTextEditor;
		protected long m_totalDuration;
		protected String m_textToHighlight;
		protected int m_digits;
		protected long m_remainingTime;

		public DurationCountdownTask(StrategyCodeExecutionTracer textArea, int startLocationInTextEditor,
				int endLocationInTextEditor, long totalDuration) {
			m_textArea = textArea;
			m_startLocationInTextEditor = startLocationInTextEditor;
			m_endLocationInTextEditor = endLocationInTextEditor;
			m_totalDuration = totalDuration;
			m_textToHighlight = textArea.getText().substring(startLocationInTextEditor, endLocationInTextEditor);
			m_digits = (int) Math.round(Math.floor(Math.log10(m_totalDuration / 1000))) + 2;
//			textArea.add
		}

		protected void setUpInitialHighlight() {

			m_textArea
					.replaceRange(
							m_textToHighlight + SETTLING_STRING
									+ StringUtils.leftPad("" + m_totalDuration / 1000, m_digits - 1) + "s",
									m_startLocationInTextEditor, m_endLocationInTextEditor);
			m_textArea.requestFocusInWindow();
			m_textArea.setCaretPosition(m_startLocationInTextEditor);
			m_textArea.moveCaretPosition(m_endLocationInTextEditor);
			m_remainingTime = m_totalDuration/1000;
			
		}
		
		@Override
		public boolean cancel() {
			try {
				if (m_textArea.getText(m_startLocationInTextEditor,m_textToHighlight.length()+SETTLING_STRING.length()).equals(m_textToHighlight + SETTLING_STRING)) {
					

					m_textArea.replaceRange(m_textToHighlight, m_startLocationInTextEditor,
							m_endLocationInTextEditor + SETTLING_STRING.length() + m_digits);
				}
			} catch (BadLocationException e) {
			}
			return super.cancel();
		}

		@Override
		public void run() {
			countDownOneSecond();
		}

		protected void countDownOneSecond() {
			m_remainingTime -= 1;
			if (m_remainingTime <=0) {
				cancel();
			}
			else {
				m_textArea
				.replaceRange(
						m_textToHighlight + SETTLING_STRING
								+ StringUtils.leftPad("" + m_remainingTime,
										m_digits - 1)
								+ "s",
						m_startLocationInTextEditor, m_endLocationInTextEditor + 
								+ SETTLING_STRING.length() + m_digits);

				m_textArea.requestFocusInWindow();
				m_textArea.setCaretPosition(m_startLocationInTextEditor);
				m_textArea.moveCaretPosition(m_endLocationInTextEditor
				+ SETTLING_STRING.length() + m_digits);

			}
		}

	}
	
	public class StrategyDurationCountdownTask extends DurationCountdownTask {

		private int m_padSize;

		public StrategyDurationCountdownTask(StrategyCodeExecutionTracer strategyCodeExecutionTracer, int start, int end,
				long duration) {
			super(strategyCodeExecutionTracer, start, end, duration);
		}
		
		@Override
		protected void setUpInitialHighlight() {
			m_textArea.requestFocusInWindow();
			m_textArea.setCaretPosition(m_startLocationInTextEditor);
			m_textArea.moveCaretPosition(m_endLocationInTextEditor);
			m_remainingTime = m_totalDuration/1000;
			m_padSize = Long.toString(m_totalDuration).length();
		}
		
		@Override
		public boolean cancel() {
			m_textArea.replaceRange(Long.toString(m_totalDuration), m_startLocationInTextEditor, m_endLocationInTextEditor);
			return super.cancel();
		}
		
		@Override
		protected void countDownOneSecond() {
			m_remainingTime -= 1;
			if (m_remainingTime <= 0) cancel();
			else {
				m_textArea.replaceRange(StringUtils.leftPad("" + (m_remainingTime*1000), m_padSize), m_startLocationInTextEditor, m_endLocationInTextEditor);
				m_textArea.requestFocusInWindow();
				m_textArea.setCaretPosition(m_startLocationInTextEditor);
				m_textArea.moveCaretPosition(m_endLocationInTextEditor);
			}
		}

	}

	protected static String getStitchCodeText(String path) {
		String stitchText = PATH_TO_STITCHTEXT.get(path);
		if (stitchText == null) {
			try {
				stitchText = new String(Files.readAllBytes(new File(path).toPath()));
				PATH_TO_STITCHTEXT.put(path, stitchText);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return stitchText;
	}

	public static Font findAppropriateFont() {

		String text = SETTLING_STRING;
		String[] fontFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		for (String name : fontFamilies) {
			Font font = new Font(name, Font.PLAIN, 12);
			if (font.canDisplayUpTo(text) < 0) {
				return font;
			}
		}
		return null;
	}

	private Color m_defaultHighlightColor;
	private Timer m_settlingTimer;

	public StrategyCodeExecutionTracer() {
		super();
		setCodeFoldingEnabled(true);
		setSyntaxEditingStyle("text/stitch");
		setEditable(false);

		Font f = null; // findAppropriateFont();
		if (f != null)
			setFont(f);
		else
			SETTLING_STRING = "...";

		m_defaultHighlightColor = getSelectionColor();
	}

	public void showExecutionTrace(StrategyInstanceData sid) {
		String path = sid.strategyData.strategy.m_stitch.path;
		String stitchText = getStitchCodeText(path);

		// Find the location of the strategy
		Pattern p = Pattern.compile("strategy.*" + sid.strategyData.name);
		Matcher m = p.matcher(stitchText);
		// If the strategy is in there (which it should be)
		if (m.find()) {
			int loc = m.start();
			setCaretPosition(loc);
			setSelectionColor(m_defaultHighlightColor);
			try {
				scrollToLine(loc);
				removeAllLineHighlights();

				higlightLineOfLocation(loc, Color.LIGHT_GRAY);

				// Scroll through all exeuctions to highlight lines
				ListIterator<TraceData> li = sid.traces.listIterator(sid.traces.size());
				while (li.hasPrevious()) {
					TraceData trace = li.previous();
					String textToHighlight = trace.label;
					Pattern pa = Pattern.compile(textToHighlight + "\\s*:");
					if (trace.state == ExecutionStateT.TACTIC_EXECUTING
							|| trace.state == ExecutionStateT.TACTIC_SETTLING
							|| trace.state == ExecutionStateT.TACTIC_DONE) {
						pa = Pattern.compile(textToHighlight + "\\s*\\(");
					}
					Matcher ma = pa.matcher(stitchText);
					if (ma.find(loc)) {
						loc = ma.start();
						switch (trace.state) {
						case NODE_EXECUTING:
						case NODE_DONE:
							higlightLineOfLocation(loc, EXECUTING_COLOR);
							break;
						case STRATEGY_EXECUTING:
						case STRATEGY_DONE:
							higlightLineOfLocation(loc, EXECUTING_COLOR);
							break;
						case STRATEGY_SETTLING: {
							higlightLineOfLocation(loc, SETTLING_COLOR);
							setSelectionColor(SETTLING_COLOR);
							pa = Pattern.compile("@\\[[^\\d]*(\\d*)[^\\d]*\\]");
							ma = pa.matcher(stitchText);
							if (ma.find(loc)) {
								setUpStrategySettlingTimer(ma.start(1), ma.group(1), Long.parseLong(ma.group(1)));
							}
						}
							break;
						case TACTIC_EXECUTING:
							// Just select the call location
							requestFocusInWindow();
							setCaretPosition(loc);
							moveCaretPosition(loc + textToHighlight.length());
							break;
						case TACTIC_SETTLING:
							setSelectionColor(SETTLING_COLOR);
							Tactic tactic = sid.strategyData.script.m_stitch.findTactic(textToHighlight);
							// If there is a duration and we're not already counting the settling then
							// Set up the UI to display the settling dynamics
							long duration = tactic.getDuration();
							if (duration > 0 && m_settlingTimer == null) {
								setUpSettlingTimer(loc, textToHighlight, duration);
							}
							break;
						case TACTIC_DONE:
							pullDownSettlingTimer();
						}
					}

				}
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	private void pullDownSettlingTimer() {
		if (m_settlingTimer != null) {
			m_settlingTimer.cancel();
			m_settlingTimer = null;
		}
	}

	private void setUpSettlingTimer(int loc, String textToHighlight, long duration) {
		m_settlingTimer = new Timer();
		DurationCountdownTask task = new DurationCountdownTask(this, loc, loc + textToHighlight.length(), duration);
		task.setUpInitialHighlight();
		m_settlingTimer.scheduleAtFixedRate(task, 0, 1000);
	}
	
	private void setUpStrategySettlingTimer(int loc, String highlight, long duration) {
		m_settlingTimer = new Timer();
		StrategyDurationCountdownTask task = new StrategyDurationCountdownTask(this, loc, loc + highlight.length(), duration);
		task.setUpInitialHighlight();
		m_settlingTimer.scheduleAtFixedRate(task, 0, 1000);
	}

	protected void higlightLineOfLocation(final int location, Color highlightColor) throws BadLocationException {
		int lineOfOffset = getLineOfOffset(location);
		addLineHighlight(lineOfOffset, highlightColor);
	}

	protected void scrollToLine(final int location) throws BadLocationException {
		Rectangle vr = modelToView(location);
		int componentHeight = getVisibleRect().height;
		scrollRectToVisible(new Rectangle(vr.x, vr.y, vr.width, componentHeight));
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
	}

	public void showStrategy(StrategyInstanceData sid) {
		String path = sid.strategyData.strategy.m_stitch.path;
		String stitchText = getStitchCodeText(path);
		
		setText(stitchText);
	}

}