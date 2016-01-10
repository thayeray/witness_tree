/*
 * Copyright (c) 2015 Thayer A. Young, Cicada Systems GIS Consulting
 *   for questions or comments about this code please contact thayer.young@cicadagis.com
 *  
 *  Portions of this code that pertain to the progress bar are modified from 
 *    ProgressBarDemo.java, see copyright below:
 * 
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * This class contains all aspects of the GUI for the Witness Tree program.
 * @author thayer young Cicada Systems GIS Consulting Copyright 2015, the progress bar is modified from ProgressBarDemo.java Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 */
@SuppressWarnings("serial")
public class WitnessGUI extends JFrame implements ActionListener, PropertyChangeListener
{
	// Constants
	public final static String MBL_DEFAULT = "Western_Wicomico_ed.mbl";
	public final static String KML_DEFAULT = "Western_Wicomico_TAY.kml";
	public final static String NO_FILE_CHOSEN = "No File Chosen", NO_TO_OVERWRITE = "Chose not to overwrite";
	public final static String NO_FOLDER_FILE_CHOSEN = "No Folder / File Name Chosen";
	public final static String[] SINGLE_LINE_CUSTOM_FIELDS = new String[] {"! NOTE=","! ANNR=","! ANNS=","! ASG=","! resurvey","! improvements"};
	public final static String[] MULTIPLE_LINE_CUSTOM_FIELDS = new String[] {"! RR:"};
	public final static String[] GEOCOMMENT_SEARCH_TERMS = new String[] {"ash","bark","bay","beech","birch","bush","cedar","cherry","chestnut","currant","cypress","dogwood","elm","gum","haw","hickory","holly","laurel","locust","maple","mulberry","myrtle","oak","peach","persimmon","pignut","pine","poplar","sassafras","scrub","spice","tree","walnut","willow","wood"};
	public final static String RUN_DATA_DIAGNOSTIC = "Run data diagnostic";
	public final static String CONVERT_TO_GIS_FILES = "Convert to GIS files";
	
	// GUI Elements
	private JFrame frame;		// The frame that holds the panels.
	private JPanel mainPanel;	// The panel that holds both the customFieldsPanel and input panels.
	private JPanel filePanel;	// The panel that holds the file IO parameters 
	private JPanel customFieldsPanel;		// Where the cut and sort fields are designated.
	private JLabel mblLabel = new JLabel ("Input: path to Deed Mapper data file:");
	private JTextField mblPathTF = new JTextField(NO_FILE_CHOSEN, 40);
	private JButton mblButton = new JButton ("MBL File");
	private JLabel kmlLabel = new JLabel ("Input: path to Deed Mapper geometry file:");
	private JTextField kmlPathTF = new JTextField(NO_FILE_CHOSEN, 40);
	private JButton kmlButton = new JButton ("KML File");	
	private JLabel outLabel = new JLabel("Output: file name and path:");
	private JTextField outPathTF = new JTextField(NO_FOLDER_FILE_CHOSEN, 40);
	private JButton outButton =     new JButton("Output Folder");
	private JTextArea customFieldsPanelSingleJTextArea = new JTextArea(20,0); 	// The GUI list of unique values from the first cut field
	private JTextArea customFieldsPanelMultipleJTextArea = new JTextArea(20,0); //  "   "    "   "   "       "     "   "  second "    "
	private JTextArea searchTermsJTextArea = new JTextArea(20,0); 				// List of terms to be searched for in the geo comments
	private JButton   diagnosticButton = new JButton(RUN_DATA_DIAGNOSTIC);
	private JButton   convertButton = new JButton(CONVERT_TO_GIS_FILES);
	private JProgressBar progress = new JProgressBar(0,100);
	
	// Class Variables
	/** the default file path to the mbl file */
	private File dataDefault = new File(MBL_DEFAULT);
	/** the default file path to the kml file */
	private File geomDefault = new File(KML_DEFAULT);
	/** the default file path to where the output will be written */
	private File outDefault = new File(System.getProperty("user.dir"),getDefaultOutFileName());
	private String[] singleLineCustomFields;
	private String[] multipleLineCustomFields;
	private String[] geoCommentSearchTerms;
	private DataTableW<String> mblTable = null;
	private DataTableW<String> kmlTable = null;
	
	/**
	 * Constructor for the GUI
	 */
	public WitnessGUI()
	{
		frame = new JFrame("Deed Mapper to GIS Converter");		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	
		mainPanel = new JPanel();				// The panel that holds both the input and customFieldsPanel panels		
		filePanel = new JPanel();
		filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
		Container labelContainer = new Container();
		labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.Y_AXIS));
		labelContainer.add(mblLabel);
		mblLabel.setAlignmentX(RIGHT_ALIGNMENT);
		labelContainer.add(Box.createRigidArea(new Dimension(0,13)));
		labelContainer.add(kmlLabel);
		kmlLabel.setAlignmentX(RIGHT_ALIGNMENT);
		labelContainer.add(Box.createRigidArea(new Dimension(0,13)));
		labelContainer.add(outLabel);
		outLabel.setAlignmentX(RIGHT_ALIGNMENT);
		Container textContainer = new Container();
		textContainer.setLayout(new BoxLayout(textContainer, BoxLayout.Y_AXIS));
		textContainer.add(mblPathTF);
		textContainer.add(kmlPathTF);
		textContainer.add(outPathTF);
		Container buttonContainer = new Container();
		buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
		buttonContainer.add(mblButton);
		buttonContainer.add(kmlButton);
		buttonContainer.add(outButton);
		filePanel.add(labelContainer);
		filePanel.add(textContainer);
		filePanel.add(buttonContainer);		
		customFieldsPanel = new JPanel();					// Where the cut fields are selected, and the results are output 
		customFieldsPanel.setLayout(new BoxLayout(customFieldsPanel, BoxLayout.Y_AXIS));	
		Container customLabelContainer = new Container();
		customLabelContainer.setLayout(new BoxLayout(customLabelContainer, BoxLayout.X_AXIS));
		customLabelContainer.add(Box.createRigidArea(new Dimension(200,0)));
		customLabelContainer.add(new JLabel("Single Line Custom Fields"));
		customLabelContainer.add(Box.createRigidArea(new Dimension(200,0)));
		customLabelContainer.add(new JLabel("Multiple Line Custom Fields"));
		customLabelContainer.add(Box.createRigidArea(new Dimension(200,0)));
		customLabelContainer.add(new JLabel("Geometry Comments Search Terms"));
		customLabelContainer.add(Box.createRigidArea(new Dimension(200,0)));
		Container customTextContainer = new Container();// The custom fields are defined here
		customTextContainer.setLayout(new BoxLayout(customTextContainer, BoxLayout.X_AXIS));
		customTextContainer.add(Box.createRigidArea(new Dimension(20,0)));
		customTextContainer.add(new JScrollPane(customFieldsPanelSingleJTextArea), BorderLayout.CENTER); // Format the output for the unique field values.
		customFieldsPanelSingleJTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
		singleLineCustomFields = Arrays.copyOf(SINGLE_LINE_CUSTOM_FIELDS,SINGLE_LINE_CUSTOM_FIELDS.length);
		customFieldsPanelSingleJTextArea.setText(arrayToString(singleLineCustomFields));
		customTextContainer.add(Box.createRigidArea(new Dimension(20,0)));
		customTextContainer.add(new JScrollPane(customFieldsPanelMultipleJTextArea), BorderLayout.CENTER);
		customFieldsPanelMultipleJTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
		multipleLineCustomFields = Arrays.copyOf(MULTIPLE_LINE_CUSTOM_FIELDS,MULTIPLE_LINE_CUSTOM_FIELDS.length);
		customFieldsPanelMultipleJTextArea.setText(arrayToString(multipleLineCustomFields));
		customTextContainer.add(Box.createRigidArea(new Dimension(20,0)));
		
		customTextContainer.add(new JScrollPane(searchTermsJTextArea), BorderLayout.CENTER);
		searchTermsJTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
		geoCommentSearchTerms = Arrays.copyOf(GEOCOMMENT_SEARCH_TERMS,GEOCOMMENT_SEARCH_TERMS.length);
		searchTermsJTextArea.setText(arrayToString(geoCommentSearchTerms));
		customTextContainer.add(Box.createRigidArea(new Dimension(20,0)));
		
		Container convertButtonContainer = new Container();
		convertButtonContainer.setLayout(new BoxLayout(convertButtonContainer, BoxLayout.X_AXIS));
		convertButtonContainer.add(diagnosticButton);
		diagnosticButton.setAlignmentX(CENTER_ALIGNMENT);
		diagnosticButton.addActionListener(this);	// <-- for progress bar, ties actionPerformed() to diagnosticButton
		convertButtonContainer.add(convertButton);
		convertButton.setAlignmentX(CENTER_ALIGNMENT);
		convertButton.addActionListener(this);	// <-- for progress bar, ties actionPerformed() to convertButton
		convertButtonContainer.add(progress);
		progress.setAlignmentX(CENTER_ALIGNMENT);
		progress.setEnabled(true);
		progress.setStringPainted(true);
		progress.setValue(0);
		progress.setString("0%");
		customFieldsPanel.add(convertButtonContainer);	
		
		customFieldsPanel.add(Box.createRigidArea(new Dimension(0,20)));
		customFieldsPanel.add(customLabelContainer);
		customFieldsPanel.add(customTextContainer);
		customFieldsPanel.add(Box.createRigidArea(new Dimension(0,20)));	
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(filePanel);
        mainPanel.add(customFieldsPanel);
        establishActions();
        
		frame.add(mainPanel);							//   is added to mainPanel by the pressGoButton() method
		frame.getContentPane();
		frame.pack();
		frame.setVisible (true);	
	}
	
	/**
	 * This class runs the logic in Witness to convert the DeedMapper files to GIS readable files.
	 * @author thayer young, the progress bar code is modified from ProgressBarDemo.java Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved. 
	 */
	class Convert extends SwingWorker<Void, Void>
	{	
		/*
         * Main task. Executed in background thread.
         * The setProgress() calls trigger propertyChange() to advance the progress bar.
         */
		public Void doInBackground() 
		{	File dataFile = getCorrectFile(dataDefault, mblPathTF, true);
			File geomFile = getCorrectFile(geomDefault, kmlPathTF, true);
			File outFile = getCorrectFile(outDefault, outPathTF, false);	// default, null, false
			DataTableW<String> mbl = null;
			DataTableW<String> kml = null;
			if(isReady(dataFile, geomFile, outFile))
			{	super.setProgress(0);
				mbl = getOrOverwriteTable(dataFile, mblTable, true);
				super.setProgress(20);
				kml = getOrOverwriteTable(geomFile, kmlTable, false);
				super.setProgress(40);
			//	System.out.println("==================== writeGeoFile ==================== writeGeoFile ==================== writeGeoFile ====================");
				Witness.writeOutputFiles(mbl, kml, geoCommentSearchTerms, getOutFile(), DEFAULT_FILE_EXTENSION);
			//	System.out.println("==================== writeGeoFile ==================== writeGeoFile ==================== writeGeoFile ====================");
				super.setProgress(100);
			}
			return null;
		}// end doInBackground() 
	
		public void done()
		{	Toolkit.getDefaultToolkit().beep();
			diagnosticButton.setEnabled(true);
        	convertButton.setEnabled(true);
        	mblButton.setEnabled(true);
    		kmlButton.setEnabled(true);
    		outButton.setEnabled(true);
        	setCursor(null); //turn off the wait cursor
		}
		
	}
	
	class Diagnostic extends SwingWorker<Void, Void>
	{
		@Override
		protected Void doInBackground() throws Exception 
		{	File dataFile = getCorrectFile(dataDefault, mblPathTF, true);
			File geomFile = getCorrectFile(geomDefault, kmlPathTF, true);
			File outFile = getCorrectFile(outDefault, outPathTF, false);	// default, null, false		
			DataTableW<String> mbl = null;
			DataTableW<String> kml = null;
			DataTableW<String> mblDuplicates = null;
			DataTableW<String> kmlDuplicates = null;
			int mblSize = 0, kmlSize = 0;
			if(isReady(dataFile, geomFile, outFile))
			{	  				
				super.setProgress(0);
				mbl = getOrOverwriteTable(dataFile, mblTable, true);				
				super.setProgress(20);
				kml = getOrOverwriteTable(geomFile, kmlTable, true);
				super.setProgress(40);
				mblDuplicates = Witness.countComparatorDuplicates(mbl, true);
				mblSize = mblDuplicates.size();
				super.setProgress(60);
				kmlDuplicates = Witness.countComparatorDuplicates(kml, true);
				kmlSize = kmlDuplicates.size();
				super.setProgress(80);
				Witness.writeDuplicateFiles(mblDuplicates, kmlDuplicates, getOutFile(), DEFAULT_FILE_EXTENSION);
				super.setProgress(100);
				if(mblSize == 0 && kmlSize == 0)
				{	JOptionPane.showMessageDialog(null, "No duplicate IDs were found in either the MBL or KML file.", 
							"Diagnostic Results", JOptionPane.INFORMATION_MESSAGE);}
				else if(mblSize > 0 && kmlSize == 0)
				{	JOptionPane.showMessageDialog(null, "The MBL file contains " + mblSize 
							+ " duplicate IDs.\nSee the:\n" + Witness.appendSuffix(dataFile, false, true, DEFAULT_FILE_EXTENSION) + " file."
							+ "\n\nAll parcels should have unique IDs, or data will be lost!", "Diagnostic Results", 
							JOptionPane.ERROR_MESSAGE);}
				else if(mblSize == 0 && kmlSize > 0)
				{	JOptionPane.showMessageDialog(null, "The KML file contains " + kmlSize
							+ " duplicate IDs.\nSee the:\n" + Witness.appendSuffix(dataFile, true, true, DEFAULT_FILE_EXTENSION) + " file." 
							+ "\n\nAll parcels should have unique IDs, or data will be lost!", "Diagnostic Results", 
							JOptionPane.ERROR_MESSAGE);}
				else if(mblSize > 0 && kmlSize > 0)
				{	JOptionPane.showMessageDialog(null, "The MBL file contains " + mblSize
							+ " duplicate IDs. And the KML file contains "+ kmlSize +".\nSee the\n" 
							 + Witness.appendSuffix(dataFile, false, true, DEFAULT_FILE_EXTENSION) + "\nand\n" 
							 + Witness.appendSuffix(dataFile, true, true, DEFAULT_FILE_EXTENSION) + " files."
							 + "\n\nAll parcels should have unique IDs, or data will be lost!", 
							 "Diagnostic Results", JOptionPane.ERROR_MESSAGE);}
			}
			return null;
		}
		
		@Override
		protected void done()
		{	Toolkit.getDefaultToolkit().beep();
			diagnosticButton.setEnabled(true);
			convertButton.setEnabled(true);
			mblButton.setEnabled(true);
			kmlButton.setEnabled(true);
			outButton.setEnabled(true);
			setCursor(null);
		}
	}
	
	/**
	 * Sets up the behavior of the GUI elements, and refers to individual methods.
	 */
	private void establishActions()
	{
		AbstractAction mblButtonPressed = new AbstractAction()  
		{	@Override
			public void actionPerformed(ActionEvent event) 
			{	mblPress();
			}
		};
		mblButton.addActionListener(mblButtonPressed);

		AbstractAction kmlButtonPressed = new AbstractAction()  
		{	@Override
			public void actionPerformed(ActionEvent event) 
			{	kmlPress();
			}
		};
		kmlButton.addActionListener(kmlButtonPressed);
		
		AbstractAction outButtonPressed = new AbstractAction()  
		{	@Override
			public void actionPerformed(ActionEvent event) 
			{	outPress();
			}
		};
		outButton.addActionListener(outButtonPressed);

	/*	AbstractAction convertButtonPressed = new AbstractAction()  
		{	@Override
			public void actionPerformed(ActionEvent event) 
			{	
				pressConvertButton();	// Runs the conversion code
			}
		};
		convertButton.addActionListener(convertButtonPressed);
	*/	
	}
	
	/**
	 * This is an intermediate method in the chain that leads to the mbl input button behavior.
	 */
	private void mblPress()
	{	try
		{	dataDefault = pressInButton("Deed Mapper Data File", "mbl", dataDefault, mblPathTF);	// Opens a file browser dialog for the input file path
		}catch (IOException exception)
		{	mblPathTF.setText(exception.toString());
		}
		
	}
	
	/**
	 * This is an intermediate method in the chain that leads to the kml input button behavior.
	 */
	private void kmlPress()
	{	try
		{	geomDefault = pressInButton("Deed Mapper Geometry File", "kml", geomDefault, kmlPathTF);	// Opens a file browser dialog for the input file path
		}catch (IOException exception)
		{	kmlPathTF.setText(exception.toString());
		}
	}
	
	/**
	 * This is an intermediate method in the chain that leads to the output file button behavior.
	 */
	private void outPress()
	{	try
		{	outDefault = pressOutButton();	// Opens a file browser dialog for the output file path
		}catch (IOException exception)
		{	outPathTF.setText(exception.toString());
		}
	}
	
	private DataTableW<String> getOrOverwriteTable(File sourceFile, DataTableW<String> table, boolean isMBL)
	{	String type;
		int overwrite = JOptionPane.NO_OPTION;
		DataTableW<String> returnTable = null;
		LinkedList<String> initial = null;
		// Get the values in the JTextAreas and use them to update the arrays (internalize the user's input)
		if(isMBL)
		{	singleLineCustomFields = customFieldsPanelSingleJTextArea.getText().split("\n");
			multipleLineCustomFields = customFieldsPanelMultipleJTextArea.getText().split("\n");
			geoCommentSearchTerms = searchTermsJTextArea.getText().split("\n");
			type = "MBL";
		}
		else type = "KML";
		if(table != null)
			overwrite = JOptionPane.showConfirmDialog(null, "There is already a "+type+" table, would you like to overwrite?");
		if(table == null || overwrite == JOptionPane.YES_OPTION)
		{	initial  = Witness.readInLines(sourceFile);
			if(isMBL) 
				table = Witness.textToTable(initial,singleLineCustomFields,multipleLineCustomFields);
			else
				table = Witness.kmlToTable(initial);
		}
		returnTable = table.clone();
		return returnTable;
	}

	/**
	 * Opens a dialog for the user to select the file path to the Deed Mapper data File described by the parameters.
	 * @param description The file type that the user will see in the file filter dialog
	 * @param extension The file extension to be filtered
	 * @param file The class variable for the current default file for the data file type
	 * @param jTextField The JTextField where the file path is listed in the GUI
	 * @throws IOException If there is a problem selecting the input file.
	 */
	private File pressInButton(String description, String extension, File file, JTextField jTextField) throws IOException
	{	String messageText;
		JFileChooser openChooser = new JFileChooser(getUserDir(file));	// creates the open file dialog, defaulting to the directory of 'file' or the user directory
		FileNameExtensionFilter filter = new FileNameExtensionFilter(description, extension);
		openChooser.setFileFilter(filter);
		int status = openChooser.showOpenDialog(null);				// displays the dialog to the user
		if (status == JFileChooser.APPROVE_OPTION)	// user has selected a file
		{	file = openChooser.getSelectedFile();			// gets the path of the selected file
			messageText = file.toString();	// sets file path as the message to be displayed
		}
		else									// no file selected
			messageText = NO_FILE_CHOSEN;		// sets error message as the message to be displayed 
		jTextField.setText(messageText);			// displays message in the in path text field 
		return file;
	}
	
	protected boolean isReady(File dataFile, File geomFile, File outFile)
	{	boolean ready = true;
		String message = "Before the conversion can be begin you must: ";
		LinkedList<String> messageList = new LinkedList<String>();
					
		// Set data and geometry files			
		if (dataFile == null)
		{	ready = false;
			messageList.add("set the mbl file");
		}		
		if (geomFile == null)
		{	ready = false;
			messageList.add("set the kml file");
		}
		if (outFile == null)
		{	ready = false;
			messageList.add("set the output file");
		}
		if (!ready)
		{	if(!messageList.isEmpty())
				message += messageList.removeFirst();
			while(!messageList.isEmpty())
				message += ", " + messageList.removeFirst();
			message += ".";
			JOptionPane.showMessageDialog(null, message,"Missing Files Error",JOptionPane.ERROR_MESSAGE);
		}
		return ready;
	}


	/**
	 * Gets the directory from the file, substitutes the user directory if neither the file nor its directory exist.
	 * @param file Checks if the File exists. For example the class variable defaults dataDefault, geomDefault, outDefault.
	 * @return
	 */
	private String getUserDir(File file)
	{	String userDir, path = "";
		String directory = "";
		if (file != null)
		{	if (file.exists())
			{	if (file.isDirectory())
					directory = file.getAbsolutePath();
				else
				{	path = file.getAbsolutePath();
					if (path.contains(File.separator))
						directory = path.substring(0,path.lastIndexOf(File.separator));
				}
			}
			else
			{	
				try
				{	path = file.getAbsolutePath();
					if (path.contains(File.separator))
					{	directory = path.substring(0,path.lastIndexOf(File.separator));		
					}
					if(!(new File(directory).exists()))
					{	directory = "";
					}
				}catch (Exception e)
				{	// do nothing					
				}
			}
		}
		if (directory.equals(""))								// if no file has been opened or closed yet
		{	userDir = System.getProperty("user.dir");			// gets user's working directory 
		}
		else 
		{	userDir = directory;								// session working directory
		}
		return userDir;
	}
	
	private final String DEFAULT_FILE_NAME = "DMtoGIS";			// default file name part 1
	private final String DEFAULT_FILE_EXTENSION = ".txt";			// default file name part 2		
	private final String FILTER_TEXT = "Tab Delimited Text Tables";
	
	/**
	 * Returns the previously chosen or default output file stub.
	 * @param True if you want to ignore the stored output file, and generate a default
	 * @return The output file stub.
	 */
	private File getOutFile()
	{	String path = "", name = "";
		File outDefaultCopy1 = null;
		if(outDefault != null)
		{	path = outDefault.getAbsolutePath();
			outDefaultCopy1 = new File(path);
			if (path.contains(File.separator))
			{	name = path.substring(path.lastIndexOf(File.separator));
			}
		}
		String userDir = getUserDir(outDefaultCopy1);	
		if(name != "")
			return new File(userDir, name);
		else
			return new File(userDir, getDefaultOutFileName());	// combine default file name parts
	}

	/**
	 * Gets a string with the default output file name stub, without the geo or data suffix.
	 * @return The default output file name stub
	 */
	private String getDefaultOutFileName()
	{	long timeStamp = System.currentTimeMillis() / 1000;		// default file name part 3
		return DEFAULT_FILE_NAME + "_" + timeStamp + DEFAULT_FILE_EXTENSION;
	}
	
	/**
	 * Opens a dialog for the user to select an output file path and output root file name.  Should be an 
	 *   empty folder. 
	 * @throws IOException If there is a problem setting the output file path
	 */
	private File pressOutButton() throws IOException
	{	File saveFile = null;
		String messageText = "";									 
		JFileChooser saveChooser = new JFileChooser();			// save file dialog	
		saveChooser.setSelectedFile(getOutFile());				// set default file name
		FileNameExtensionFilter filter = new FileNameExtensionFilter(FILTER_TEXT, DEFAULT_FILE_EXTENSION);
		saveChooser.setFileFilter(filter);
		int status = saveChooser.showSaveDialog(null);
		if (status != JFileChooser.APPROVE_OPTION)
			messageText = NO_FOLDER_FILE_CHOSEN;					// sets message to error message
		else
		{	saveFile = saveChooser.getSelectedFile();
			File saveFileGeo = null;
			File saveFileDat = null;
			if (saveFile != null)
			{	saveFileGeo = Witness.appendSuffix(saveFile, true, false, DEFAULT_FILE_EXTENSION);
				saveFileDat = Witness.appendSuffix(saveFile, false, false, DEFAULT_FILE_EXTENSION);
				int overwrite;
				if (saveFileGeo.exists() || saveFileDat.exists())
				{			
					overwrite = JOptionPane.showConfirmDialog(null, "That geo and data files for that file stub already exists.  Do you want to overwrite?");
					if (overwrite == JOptionPane.YES_OPTION)
					{	messageText = saveFile.toString();		// sets message to be the path of the selected file
						outDefault = saveFile;					// saves the user's choice
					}
					else // do not overwrite: default file name at user's selected path
					{	String path = saveFile.getAbsolutePath();
						if(path.contains(File.separator))
							path = path.substring(0,path.lastIndexOf(File.separator)); 
						outDefault = new File(path, getDefaultOutFileName());	// saves the default file name with the user's path
						messageText = outDefault.getAbsolutePath();// sets message to the default file name
					}
				}
				else 
				{	messageText = saveFile.toString();			// sets message to be the path of the selected file	
					outDefault = saveFile;					// saves the user's choice
				}
			}
		}
		outPathTF.setText(messageText);					// displays message in the out path text field
		return saveFile;
	}
	
	/**
	 * The method checks the possible file/directories to see if there is a match with the desired outcome. If a match
	 *   is found default is set to correspond to that match and the correct file is returned.
	 * @param defaultFile The class variable holding the default file.
	 * @param textFieldToCheck The JTextField that would have the file information.
	 * @param existsIsGood True if you want the file to exist, false if not.
	 * @return The corresponding File, either from the parameters or null if further user input is required.
	 */
	private static File getCorrectFile(File defaultFile, JTextField textFieldToCheck, boolean existsIsGood)
	{	
		File dataFile = null;
		String textToCheck = textFieldToCheck.getText();
		boolean good = false;
		if(textToCheck != null)
		{	if(!textToCheck.equals(NO_FILE_CHOSEN) && !textToCheck.equals(NO_FOLDER_FILE_CHOSEN) && textToCheck.contains(File.separator))	// JTextField has been altered
			{	dataFile = new File(textToCheck);					// Check match for text in JTextField		
				good = correctFile(dataFile,existsIsGood);
			}
			if(!good)												// No match. Then check the default file 
			{	dataFile = defaultFile;
				good = correctFile(dataFile, existsIsGood);
			}
			if(good)												// Match has been found, set default and returns file.
			{	defaultFile = dataFile;
				textFieldToCheck.setText(dataFile.getAbsolutePath());
				return dataFile;
			}
		}
		return null;											// No match for any of the three tests, return null.
	}
	
	/**
	 * Verifies the status of a file in the file system. The method checks it against your desired outcome.
	 * @param fileToCheck The file you want to verify the existence of.
	 * @param existsIsGood Set to true if you are searching for an existing file. Set to false if you are checking that a file does not exist.
	 * @return True conditions are met: file exists and exists is good, or file does not exist and exists is not good.
	 */
	private static boolean correctFile(File fileToCheck, boolean existsIsGood)
	{	boolean good;
		if(fileToCheck.exists() && existsIsGood)			//		the file exists and that is desired --> 1
			good = true;
		else if(fileToCheck.exists() && !existsIsGood)		//		the file exists --> get user input
		{	int overwrite;
			overwrite = JOptionPane.showConfirmDialog(null, "That file already exists.  Do you want to overwrite?");
			if (overwrite == JOptionPane.YES_OPTION)		//			the file exists and that is desired --> 1
				good = true;
			else 	 										// 			the file exists and that is not desired --> 2
				good = false;// check checkFile? --> separate method?
		}
		else if(!fileToCheck.exists() && existsIsGood)		//		the file does not exist but that is not desired --> 2
			good = false;
		else// if(!fileToCheck.exists() && !existsIsGood)	//		the file does not exist and that is desired --> 1
			good = true;
		return good;
	}
	
	public static String arrayToString(String[] list)
	{	String result = "";
		if (list.length > 0)
			for (String current:list)
				result += current + "\n";
		return result;
	}
	
	public static String[] stringToArray(String string)
	{	
		return string.split("\n");
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) 
	{	if ("progress" == evt.getPropertyName()) // <-- interpret the event
		{    int prog = (Integer) evt.getNewValue();
            progress.setValue(prog);			// <-- set the value to the progress bar
            String source = evt.getSource().toString();	// <-- parse the source and display it in the progress bar
            source = source.substring(source.indexOf("$") + 1, source.indexOf("@"));
            progress.setString(source + " " + new Integer(prog).toString()+"%");
		}
	}

	@Override
	public void actionPerformed(ActionEvent event) 
	{	diagnosticButton.setEnabled(false);
		convertButton.setEnabled(false);
		mblButton.setEnabled(false);
		kmlButton.setEnabled(false);
		outButton.setEnabled(false);
	    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));	// <-- changes the mouse pointer to a spinning wait disk.
	    //Instances of javax.swing.SwingWorker are not reusuable, so we create new instances as needed.
		if(event.getActionCommand().equals(CONVERT_TO_GIS_FILES))
		{	Convert convert = new Convert();
	    	convert.addPropertyChangeListener(this);	// <-- tie the property change listener to 'task'
	    	convert.execute();	// <-- swing worker method to execute the doInBackground() method of Task.
		}	
		else if(event.getActionCommand().equals(RUN_DATA_DIAGNOSTIC))
		{	Diagnostic diagnostic = new Diagnostic();
			diagnostic.addPropertyChangeListener(this);
			diagnostic.execute();
		}
	}
}