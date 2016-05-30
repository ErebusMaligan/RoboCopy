package app;

import gui.entry.CheckEntry;
import gui.entry.DirectoryEntry;
import gui.entry.Entry;
import gui.entry.ListPanel;
import gui.props.UIEntryProps;
import gui.props.variable.BooleanVariable;
import gui.props.variable.IntVariable;
import gui.props.variable.StringVariable;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;

import process.StandardProcess;
import statics.GU;
import statics.GUIUtils;
import ui.log.LogDialog;
import ui.log.LogFileSiphon;

/**
 * @author Daniel J. Rivers
 *         2013
 *
 * Created: Aug 24, 2013, 6:14:01 PM
 */
public class RoboCopy extends JFrame {

	private static final long serialVersionUID = 1L;

	private UIEntryProps props = new UIEntryProps();
	
	private ListPanel list = new ListPanel( "Excluded Dir" );

	public RoboCopy() {
		ImageIcon icon = new ImageIcon( getClass().getResource( "robocopy.png" ) );
		this.setTitle( "Advanced Robocopy" );
		this.setIconImage( icon.getImage() );
		this.setSize( new Dimension( 420, 220 ) );
		this.setDefaultCloseOperation( EXIT_ON_CLOSE );
		this.setLayout( new BorderLayout() );
		setupProps();
		this.add( getTabbedPane(), BorderLayout.CENTER );
		JButton b = new JButton( "Run Copy" );
		b.addActionListener( e -> execute() );
		JButton bat = new JButton( "Make Bat" );
		bat.addActionListener( e -> makeBat() );
		JPanel south = new JPanel();
		GU.hp( south, b, bat );
		this.add( south, BorderLayout.SOUTH );
		list.addItems( new String[] { "System Volume Information", "$RECYCLE.BIN" } );
		this.setVisible( true );
	}
	
	private String[] concat() {
		String sub = props.getString( "incSub" ).equals( "true" ) ? "/S" : "";
		String mir = props.getString( "mir" ).equals( "true" ) ? "/MIR" : "";
		String fft = props.getString( "fft" ).equals( "true" ) ? "/FFT" : "";
		String xd = list.getAllItems().isEmpty() ? "" : "/XD";
		String[] multiLaunch = new String[] { "robocopy", '\"' + props.getString( "sourceDir" ) + '\"', '\"' + props.getString( "destDir" ) + '\"', sub, "/W:" + props.getString( "wait" ),
				"/R:" + props.getString( "retry" ), mir, fft, xd };
		String[] finalLaunch = new String[ multiLaunch.length + list.getAllItems().size() ];
		int i = 0;
		while( i < multiLaunch.length ) {
			finalLaunch[ i ] = multiLaunch[ i++ ];
		}
		int x = 0;
		while( x < list.getAllItems().size() ) {
			finalLaunch[ i++ ] = list.getAllItems().get( x++ );
		}
		return finalLaunch;
	}

	private void makeBat() {
		String runName = props.getString( "logName" ) + ".log";
		String[] finalLaunch = concat();
		JFileChooser fc = new JFileChooser();
		String ext = ".bat";
		fc.setFileFilter( new FileNameExtensionFilter( "Windows Batch", ext ) );
		if ( fc.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION ) {
			File f = fc.getSelectedFile();
			if ( !f.getName().endsWith( ext ) ) {
				f = new File( f.getAbsolutePath() + ext );
			}
			String s = "";
			for ( String a : finalLaunch ) {
				s += a + " ";
			}
			s += "> " + runName;
			try ( BufferedWriter w = new BufferedWriter( new FileWriter( f ) ) ) {
				w.write( s );
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void execute() {
		String runName = props.getString( "logName" );
		String[] finalLaunch = concat();
		new LogFileSiphon( runName, props.getString( "destDir" ) + props.getString( "logName" ) + ".log" ) {
			public void skimMessage( String name, String s ) {
				try {
					if ( s.endsWith( "%" ) ) {
						fstream.write( s );
					} else {
						fstream.write( "[" + sdf.format( new Date( System.currentTimeMillis() ) ) + "]:  " + s );
						fstream.newLine();
					}
					fstream.flush();
				} catch ( IOException e ) {
					e.printStackTrace();
				}
			}
		};
		new LogDialog( this, runName, false );
		new StandardProcess( runName, finalLaunch );
	}

	private void setupProps() {
		props.addVariable( "sourceDir", new StringVariable( "D:/Source/" ) );
		props.addVariable( "destDir", new StringVariable( "D:/Dest/" ) );
		props.addVariable( "logName", new StringVariable( "output" ) );
		props.addVariable( "incSub", new BooleanVariable( true ) );
		props.addVariable( "mir", new BooleanVariable( true ) );
		props.addVariable( "wait", new IntVariable( 5 ) );
		props.addVariable( "retry", new IntVariable( 10000000 ) );
		props.addVariable( "fft", new BooleanVariable( true ) );
	}

	private JTabbedPane getTabbedPane() {
		JTabbedPane p = new JTabbedPane();
		p.add( "Directories", dirPanel() );
		p.add( "Exclude", list );
		p.add( "Options", optPanel() );
		return p;
	}

	private JPanel dirPanel() {
		JPanel p = new JPanel();
		p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
		p.add( new DirectoryEntry( "Source Dir:", props.getVariable( "sourceDir" ) ) );
		GUIUtils.spacer( p );
		p.add( new DirectoryEntry( "Dest Dir:", props.getVariable( "destDir" ) ) );
		GUIUtils.spacer( p );
		p.add( new Entry( "Log Name:", props.getVariable( "logName" ), new Dimension( GUIUtils.SHORT ) ) );
		return p;
	}

	private JPanel optPanel() {
		JPanel p = new JPanel();
		p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
		JPanel c = new JPanel();
		c.setLayout( new BoxLayout( c, BoxLayout.X_AXIS ) );
		c.add( new CheckEntry( "Include Subdirectories", props.getVariable( "incSub" ) ) );
		c.add( new CheckEntry( "Mirror", props.getVariable( "mir" ) ) );
		c.add( new CheckEntry( "Less Precise Timestamp", props.getVariable( "fft" ) ) );
		p.add( c );
		p.add( new Entry( "Time Between Retry( sec )", props.getVariable( "wait" ), new Dimension[] { GUIUtils.FIELD, GUIUtils.SHORT } ) );
		GUIUtils.spacer( p );
		p.add( new Entry( "Number of Retries", props.getVariable( "retry" ), new Dimension[] { GUIUtils.FIELD, GUIUtils.SHORT } ) );
		GUIUtils.spacer( p, new Dimension( 10, 30 ) );
		return p;
	}

	public static void main( String args[] ) {
		try {
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		} catch ( ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e ) {
			System.err.println( "Critical JVM Failure!" );
			e.printStackTrace();
		}
		new RoboCopy();
	}
}