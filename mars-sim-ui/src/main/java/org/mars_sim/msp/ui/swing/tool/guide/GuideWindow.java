/*
 * Mars Simulation Project
 * GuideWindow.java
 * @date 2021-12-14
 * @author Lars Naesbye Christensen
 */

package org.mars_sim.msp.ui.swing.tool.guide;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.tool.JStatusBar;
import org.mars_sim.msp.ui.swing.toolwindow.ToolWindow;
import org.mars_sim.msp.ui.swing.utils.SwingHelper;

/**
 * The GuideWindow is a tool window that displays built-in html pages such as User Guide, Quick Tutorial, Keyboard Shortcuts, etc.
 */
@SuppressWarnings("serial")
public class GuideWindow extends ToolWindow implements ActionListener, HyperlinkListener {

	/** Tool name. */
	public static final String NAME = Msg.getString("GuideWindow.title"); //$NON-NLS-1$
	public static final String ICON = "action/help";
	
	public static final String WIKI_URL = Msg.getString("GuideWindow.githubwiki.url"); //$NON-NLS-1$
	public static final String WIKI_TEXT = Msg.getString("GuideWindow.githubwiki.title"); //$NON-NLS-1$
    
	private JButton link;
	
	private JLabel urlLabel;
	
	/** Data members. */
	/** our HTML content pane. */
	private HTMLContentPane htmlPane;
	/** The view port for the text pane. */
	private JViewport viewPort;
	/** The guide window URL. */
	private URL guideURL;
	
	private Icon homeIcon = ImageLoader.getIconByName("home");
	
	private JButton homeButton = new JButton(homeIcon);
	private JButton backButton = new JButton("<");
	private JButton forwardButton = new JButton(">");

	/**
	 * Constructor.
	 * 
	 * @param desktop
	 *            the desktop pane
	 */
	public GuideWindow(MainDesktopPane desktop) {
		super(NAME, desktop);

		guideURL = getClass().getResource(Msg.getString("doc.guide")); //$NON-NLS-1$
		
		// May use EventQueue.invokeLater()
       	init();            
	}
	
	public void init() {
			
		homeButton.setToolTipText(Msg.getString("GuideWindow.tooltip.home")); //$NON-NLS-1$
		homeButton.setSize(16, 16);
		homeButton.addActionListener(this);

		backButton.setToolTipText(Msg.getString("GuideWindow.tooltip.back")); //$NON-NLS-1$
		backButton.setSize(16, 16);
		backButton.addActionListener(this);

		forwardButton.setToolTipText(Msg.getString("GuideWindow.tooltip.forward")); //$NON-NLS-1$
		forwardButton.setSize(16, 16);
		forwardButton.addActionListener(this);

		// Create the main panel
		JPanel mainPane = new JPanel(new BorderLayout());
		setContentPane(mainPane);

		JPanel topPanel = new JPanel(new BorderLayout());
		mainPane.add(topPanel, BorderLayout.NORTH);
		
		// A toolbar to hold all our buttons
		JPanel homePanel = new JPanel(new FlowLayout(3, 3, FlowLayout.CENTER));
		topPanel.add(homePanel, BorderLayout.CENTER);
		
		homePanel.add(backButton);
		homePanel.add(homeButton);
		homePanel.add(forwardButton);
		
		JPanel linkPanel = new JPanel(new FlowLayout(2, 2, FlowLayout.LEFT));
		topPanel.add(linkPanel, BorderLayout.EAST);
		
		link = new JButton(WIKI_TEXT);
		link.setAlignmentY(1f);
		link.setToolTipText("Open mars-sim wiki in GitHub");
		linkPanel.add(link);
		link.addActionListener(e -> {
							SwingHelper.openBrowser(WIKI_URL);
							}
						);


		// Initialize the status bar
		mainPane.add(initializeStatusBar(),  BorderLayout.SOUTH);

		htmlPane = new HTMLContentPane();
		htmlPane.addHyperlinkListener(this);
		htmlPane.goToURL(guideURL);
		htmlPane.setContentType("text/html");
		htmlPane.setBackground(Color.lightGray);
		htmlPane.setBorder(new EmptyBorder(2, 2, 2, 2));

		JScrollPane scrollPane = new JScrollPane(htmlPane);
		viewPort = (JViewport) scrollPane.getViewport();
		viewPort.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
		mainPane.add(scrollPane,  BorderLayout.CENTER);
		
		updateButtons();
	
		setResizable(true);
		setMaximizable(true);
		setVisible(true);
	
		setSize(new Dimension(800, 600));		
		Dimension desktopSize = desktop.getMainWindow().getFrame().getSize();
		Dimension windowSize = getSize();

		int width = (desktopSize.width - windowSize.width) / 2;
		int height = (desktopSize.height - windowSize.height - 100) / 2;
		setLocation(width, height);
		
		// Pack window.
		// WARNING: using pack() here will shrink the window to one line tall in swing mode

		setURLString(Msg.getString("doc.whatsnew")); //$NON-NLS-1$
	}

    /**
     * Initializes status bar and its content.
     */
    private JStatusBar initializeStatusBar() {
    	JStatusBar statusBar = new JStatusBar(1, 1, 20);

        urlLabel = new JLabel();
        urlLabel.setFont(new Font("Times New Roman", Font.ITALIC, 10));
        urlLabel.setForeground(Color.DARK_GRAY);
		statusBar.addLeftComponent(urlLabel, false);

		statusBar.addRightCorner();

        return statusBar;
    }
    
    private void updateHoveringURL(String value) {
    	urlLabel.setText(value);
    }
    
	/**
	 * Set a URL String for display.
	 */
	public void setURLString(String fileloc) {
		if (htmlPane != null)
			htmlPane.goToURL(getClass().getResource(fileloc));
	}

	/** Implementing ActionListener method. */
	@Override
	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		if (source == this.homeButton) {
			htmlPane.goToURL(guideURL);
			updateButtons();
		} else if (source == this.backButton) {
			htmlPane.back();
			updateButtons();
		} else if (source == this.forwardButton) {
			htmlPane.forward();
			updateButtons();
		}
	}

	/**
	 * Handles a click on a link.
	 * @param event the HyperlinkEvent
	 */
	@Override
	public void hyperlinkUpdate(HyperlinkEvent event) {
		if (event.getEventType() == HyperlinkEvent.EventType.ENTERED) {
			updateHoveringURL(event.getURL().toString());
		}
		else if (event.getEventType() == HyperlinkEvent.EventType.EXITED) {
			updateHoveringURL("");
		}
		else if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			htmlPane.goToURL(event.getURL());
			updateButtons();
		}	
	}
	
	/**
	 * Updates navigation buttons.
	 */
	public void updateButtons() {
		homeButton.setEnabled(true);
		backButton.setEnabled(!htmlPane.isFirst());
		forwardButton.setEnabled(!htmlPane.isLast());
	}
	
	/** 
	 * Prepare tool window for deletion. 
	 */
	@Override
	public void destroy() {
		htmlPane = null;
		viewPort = null;
		guideURL = null;
		homeButton = null;
		backButton = null;
		forwardButton = null;
	}
}
