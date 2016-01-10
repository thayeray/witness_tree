import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JOptionPane;


/**
 * This class contains the main and supporting methods for the WitnessGUI, that converts a Deed Mapper .mbl file to a 
 *   flat table and ties tract descriptions to the point geometry in the DM .kml files.
 * 08/2015 - 12/2015  
 * @author thayer young
 */
public class Witness
{	
	/**
	 * The main method that launches the GUI for the program
	 * @param args
	 */
	public static void main(String[] args)
	{	
		@SuppressWarnings("unused")
		WitnessGUI frame = new WitnessGUI(); // launch GUI
	}
	//field order for MBL DataRecord: 
	// {fieldName,rcrdCntStr,allFieldsCnt,cmntCntStr,fieldCntStr,edgePtCntStr,comment/value or additional fields: 
	//		for geometry:direction,distance,ddComment,id; for 'loc': it is split on the " " character}
	//   fieldName may be 'commentLabel', 'before' or 'loc_tay'
	/** The index of the field name for the MBL DataRecordW. */
	public static final int MBL_FIELDNAME = 0;
	/** The index of the parcel count for the MBL DataRecordW. */
	public static final int MBL_RECORDCOUNT = 1;
	/** The index of the count of all fields: comments, regular and geometry for the MBL DataRecordW. */
	public static final int MBL_ALLFIELDSCOUNT = 2;
	/** The index of the count of comment fields for the MBL DataRecordW. */
	public static final int MBL_COMMENTCOUNT = 3;
	/** The index of the count of regular fields (without comments or geometry) for the MBL DataRecordW. */
	public static final int MBL_FIELDCOUNT = 4;
	/** The index of the count of geometry fields for the MBL DataRecordW. */
	public static final int MBL_EDGEPOINTCOUNT = 5;
	/** The index of the value for regular fields or comments for the MBL DataRecordW. */
	public static final int MBL_VALUE = 6;
	/** The index of the direction for geometry fields for the MBL DataRecordW. */
	public static final int MBL_G_DIRECTION = 6;
	/** The index of the distance for geometry fields for the MBL DataRecordW. */
	public static final int MBL_G_DISTANCE = 7;
	/** The index of the comments for geometry fields for the MBL DataRecordW. */
	public static final int MBL_G_DDCOMMENT = 8;
	/** The index of the ID for geometry fields for the MBL DataRecordW. */
	public static final int MBL_G_ID = 9;
	
	// A key to the positions in the DataRecord is as follows (note that position 3 is id except when gType is name):
    //    0:pid, 1:gidStr, 2:gType, 3:name or id, 4:x, 5:y 
	/** The index of the parcel count for the KML DataRecordW. */
	public static final int KML_PID = 0;
	/** The index of the geometry count for the KML DataRecordW. */
	public static final int KML_GID = 1;
	/** The index of the type of record for the KML DataRecordW. */
	public static final int KML_GTYPE = 2;
	/** The index of the ID for the KML DataRecordW. */
	public static final int KML_ID = 3;
	/** The index of the name for the KML DataRecordW. */
	public static final int KML_NAME = 3;
	/** The index of the X coordinate for the KML DataRecordW. */
	public static final int KML_X = 4;
	/** The index of the Y coordinate for the KML DataRecordW. */
	public static final int KML_Y = 5;
	
	/** checkCustomFieldType() code for a single line comment field */
	private static final int SINGLE_LINE_FIELD = 1;
	/** checkCustomFieldType() code for a multiple line comment field */
	private static final int MULTIPLE_LINE_FIELD = 2;
	/** checkCustomFieldType() code when the method can not distinguish the comment field type (error) */
	private static final int UNKNOWN = 0;

	/** kmlToTable() integer code denoting that the field is not a geometry field */
	private static final int NOT_GEOMETRY = -1;
	/** kmlToTable() String code denoting that the field is not a geometry field */
	private static final String NOT_GEOMETRYs = "-1";

	/**
	 * This method based on: http://www.mkyong.com/java/how-to-read-file-from-java-bufferedreader-example/
	 * @param inFile The data file that will be read in line by line and returned as a linked list of strings
	 * @return a linked list of strings representing the original data file.
	 */
	public static LinkedList<String> readInLines(File inFile)	// infinite loop bug reading in files that do not end with end, end with "!".
	{	LinkedList<String> lines = new LinkedList<String>(); 
		BufferedReader br = null;
		try 
		{	String sCurrentLine;
			br = new BufferedReader(new FileReader(inFile));
			while ((sCurrentLine = br.readLine()) != null) 
			{	if (sCurrentLine.contains("&#62;"))
					sCurrentLine = sCurrentLine.replace("&#62;", ">");
				if (sCurrentLine.contains("&#60;"))
					sCurrentLine = sCurrentLine.replace("&#60;", "<");
				lines.add(sCurrentLine);
			}
		} catch (IOException e) {
			popupErrorDialog("There was a problem reading the file: "+inFile.getPath(),"File Read Error",e);
		}catch(Exception e)
		{	 popupErrorDialog("There was a problem reading the file: "+inFile.getPath(),"File Read Error",e);
		}finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return lines;
	}
	
	/**
	 * The main logic for parsing the "read in" Deed Mapper ".mbl" data file.
	 * @param linesOfText The "read in" data file (created using the Witness.readInLines method)
	 * @return The formatted table containing a LinkedList of records and a CountingTree of field names
	 */
	public static DataTableW<String> textToTable(LinkedList<String> linesOfText, String[] singleLineFields, String[] multipleLineFields)
	{	// unique field names are listed in the 'fieldList', except for geometry field names: pt, lc, lm, ln.  
		CountingTree fieldList = new CountingTree();
		// This is a unique list of the comments in the geometry sub-records, key is comment, value is count for that comment
		CountingTree geometryCommentMap = new CountingTree(); 
	    // one 'record' per parcel, each 'record' contains a list of 'DataRecordW' objects containing the fields and geometry of each parcel
		Parcel<String> record = null; 
		// the 'table' contains all of the parcels in the data file
		LinkedList<Parcel<String>> table = new LinkedList<Parcel<String>>();  
		int commentCntMax = 0, locLengthMin = 100, locLengthMax = 0;
		int recordCount = 0;
		String rcrdCntStr = (new Integer(0)).toString();
		String allFieldsCnt = "", cmntCntStr = "", fieldCntStr = "", edgePtCntStr = "";
		int commentCnt, fieldCnt, edgePtCnt;
		int firstSemi, secondSemi;
		String current;
		String comment, commentLabel = "", before = "", after, id="";
		String direction, distance, ddComment;
		String[] locParam;
		String[] temp;
		int pos;
		boolean multiCustom = false;
		int customFieldType = 0;
		try	
		{	while(!linesOfText.isEmpty())		// begin loop that reads the data file into a 'table',  
			{	recordCount++;	// parcel count
				rcrdCntStr = (new Integer(recordCount)).toString();
				commentCnt = 0;
				fieldCnt = 0;
				edgePtCnt = 0;
				locParam = null;
				temp = null;
				id = "";
				current = linesOfText.poll();
				record = new Parcel<String>();	// TODO change all uses of 'record' if order changes
				//field order for 'record' {fieldName,rcrdCntStr,allFieldsCnt,cmntCntStr,fieldCntStr,edgePtCntStr,comment or additional fields: for geometry:direction,distance,ddComment,id; for 'loc': it is split on the " " character}
				//   fieldName may be 'commentLabel', 'before' or 'loc_tay'
				while (!current.startsWith("end") && !linesOfText.isEmpty())	// begin parcel loop --> prepares a 'record' to add to the 'table'
				{	firstSemi = 0;
					secondSemi = 0;
					direction = "";
					distance = "";
					ddComment = "";
					multiCustom = false;
					customFieldType = checkCustomFieldType(current, singleLineFields, multipleLineFields);
					if(customFieldType == SINGLE_LINE_FIELD)	// convert single line custom fields to fields. <-- exit comment logic	
						current = current.substring(1).trim(); 	// remove the '!' and concatenate (desirable??)
					if(customFieldType == MULTIPLE_LINE_FIELD)	// flag multiline custom fields. <-- remain in comment logic
					{	multiCustom = true;
						commentLabel = getFieldMatch(current, multipleLineFields);
					}
					if (current.startsWith("!"))				// comment logic, concatenates comment into single list entry
					{	if(!multiCustom)
						{	commentCnt++;
							commentLabel = "z_cmnt" + commentCnt;
						}
						cmntCntStr = (new Integer(commentCnt)).toString();
						allFieldsCnt = (new Integer(commentCnt + fieldCnt + edgePtCnt)).toString();
						comment = "";					
						while (current.startsWith("!"))	
						{	current = current.replace("\t", " ");	// TODO may want to do this to all lines, not just comments.
							comment += current.substring(1);	// remove the '!' and concatenate (desirable??)	
							if (!linesOfText.isEmpty())
							{	current = linesOfText.poll();	// get next line, preview its contents, converting single line to fields
								customFieldType = checkCustomFieldType(current, singleLineFields, multipleLineFields);
								if(customFieldType == SINGLE_LINE_FIELD)		// convert single line custom fields to fields	
								{	current = current.substring(1).trim();	// remove the '!' and concatenate (desirable??)
								}
								else if(customFieldType == MULTIPLE_LINE_FIELD)	// convert multiple line custom fields to fields
								{	// add the current record
									if(!comment.trim().equals("")) 
									{	record.add(new DataRecordW<String>(new String[]{commentLabel,rcrdCntStr,allFieldsCnt,cmntCntStr,"0","0",comment},1,MBL_FIELDNAME),false);
										fieldList.add(commentLabel);
									}
									else commentCnt--;	// discard comments that contain nothing but white space
									// start a new record
									commentLabel = getFieldMatch(current, multipleLineFields);
								}
							}
						}// end multiline comment/field loop
						if(!comment.trim().equals("")) 
						{	record.add(new DataRecordW<String>(new String[]{commentLabel,rcrdCntStr,allFieldsCnt,cmntCntStr,"0","0",comment},1,MBL_FIELDNAME),false);
							fieldList.add(commentLabel);
						}
						else commentCnt--;	// discard comments that contain nothing but white space
					}// end comment logic
					if (current.startsWith("end"))		// start next parcel
					{	if(!linesOfText.isEmpty())
							current = linesOfText.poll();
					}
					else	// field logic
					{	if(current.contains(" ") && current.length() > current.indexOf(" ") + 1) // if 'after' exists
						{	before = current.substring(0, current.indexOf(" ")); // the field name (before the first space)
							after = current.substring(current.indexOf(" ") + 1); // the field content, which may be further subdivided
							if (before.equals("id"))
							{	id = after;			// capture the id so it can be added to the geometry sub-records
								record.setComparator(id);	// the parcels will be compared using 'id' for .equals(), .contains(), etc.
							}
							if (!isMBLgeoField(before))
							{// handle field names for non geometry fields	
								fieldList.add(before);							// add all field names to fieldList
							}// end handle field names for non geometry fields
							if (isMBLgeoField(before))
							{// add geometry sub-record
								edgePtCnt++;
								if (after.contains(";"))	// format of geometry is: 'before';'direction';'distance';'ddComment'
								{	firstSemi = after.indexOf(";");
									direction = after.substring(0, firstSemi);  // 'direction' assigned
									if (after.substring(firstSemi+1).contains(";"))
									{	secondSemi = (after.substring(firstSemi+1)).indexOf(";") + firstSemi + 1;
										distance = after.substring(firstSemi + 1, secondSemi); // 'distance' assigned
										if (after.length() > secondSemi + 1)
											ddComment = after.substring(secondSemi + 1); // 'ddComment' assigned									
									}
									else ddComment = after.substring(firstSemi+1); // in case there is no distance, 'ddComment' assigned
								}
								else ddComment = after; // mostly used for 'pt' start points of tract description, 'ddComment' assigned
								edgePtCntStr = (new Integer(edgePtCnt)).toString();
								allFieldsCnt = (new Integer(commentCnt + fieldCnt + edgePtCnt)).toString();
								record.add(new DataRecordW<String>(new String[]{before,rcrdCntStr,allFieldsCnt,"0","0",edgePtCntStr,direction,distance,ddComment,(id+"    ["+edgePtCnt+"]")},1,MBL_FIELDNAME),true);
								geometryCommentMap.add(ddComment);
							}// end add geometry sub-record
							else if (before.equals("loc"))	// add 'loc' field. TODO change this if 'loc' handling changes
							{	fieldCnt++;
								fieldCntStr = (new Integer(fieldCnt)).toString();
								allFieldsCnt = (new Integer(commentCnt + fieldCnt + edgePtCnt)).toString();
								// standard method for handling a field
								record.add(new DataRecordW<String>(new String[]{before,rcrdCntStr,allFieldsCnt,"0",fieldCntStr,"0",after},1,MBL_FIELDNAME),false);
								if (after.length() > 0) // non-standard method: splits the 'after' for 'loc' on ' ' and attaches the split on the end of the 'record'  
								{	fieldCntStr = (new Integer(++fieldCnt)).toString();
									allFieldsCnt = (new Integer(commentCnt + fieldCnt + edgePtCnt)).toString();
									locParam = after.split(" ");  // splits 'after' portion of 'loc' using ' ' as the delimeter
									temp = new String[locParam.length + 6]; // creates a new array and puts the 6 basic fields up front in that array
									temp[MBL_FIELDNAME] = "loc_tay";
									temp[MBL_RECORDCOUNT] = rcrdCntStr;
									temp[MBL_ALLFIELDSCOUNT] = allFieldsCnt;
									temp[MBL_COMMENTCOUNT] = "0";
									temp[MBL_FIELDCOUNT] = fieldCntStr;
									temp[MBL_EDGEPOINTCOUNT] = "0";
									pos = 5;
									for (String cur:locParam) // then copy in the split out portions from 'loc' 
										temp[++pos] = cur;
									record.add(new DataRecordW<String>(temp,1,MBL_FIELDNAME),false); // add the non-standard 'loc'
									if(pos + 1 > locLengthMax)
										locLengthMax = pos + 1;
									if(pos + 1 < locLengthMin)
										locLengthMin = pos + 1;
								} // end non-standard method for handling 'loc'
							}// end add 'loc' field
							else // add record for non geometry and non loc fields
							{	fieldCnt++;
								fieldCntStr = (new Integer(fieldCnt)).toString();
								allFieldsCnt = (new Integer(commentCnt + fieldCnt + edgePtCnt)).toString();
								record.add(new DataRecordW<String>(new String[]{before,rcrdCntStr,allFieldsCnt,"0",fieldCntStr,"0",after},1,MBL_FIELDNAME),false);
							}// end add record for non geometry and non loc fields
						}// end "if 'after' exists"
						if(!linesOfText.isEmpty())
							current = linesOfText.poll();
						else current = "end";
					}// end field logic
				}// end parcel loop
				table.add(record);
				if (commentCnt > commentCntMax)
					commentCntMax = commentCnt;
			}// end data file loop
		}catch(Exception e)
		{	popupErrorDialog("An error occured while reading the mbl file.","MBL File Error", e);
		}
		return new DataTableW<String>(table,fieldList, geometryCommentMap);
	}// end of textToTable()
	
	/**
	 * Pops up an error dialog with the corresponding message, label and the first 10 lines of the stack trace.
	 * @param message The message that precedes the error detail.
	 * @param label The label of the dialog.
	 * @param e The caught exception object.
	 */
	public static void popupErrorDialog(String message, String label, Exception e)
	{	message += "\n\n"+e.toString() + "\n";
		StackTraceElement[] trace = e.getStackTrace();
		int count = 0;
		while (count < 10 && count < trace.length)
		{	message += trace[count++].toString() + "\n";	
		}	
		JOptionPane.showMessageDialog(null,message,label,JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * This checks if the string begins with any of the elements in either of 2 parameter arrays.
	 *   The second array is not checked if the first array contains a match.
	 * @param current The string to be checked
	 * @param singleLineFields The first array to be checked against
	 * @param multipleLineFields The second array to be checked against
	 * @return 0 if no match, 1 if matches a single line element, 2 if multiple line
	 */
	private static int checkCustomFieldType(String current, String[] singleLineFields, String[] multipleLineFields)
	{	if(checkField(current, singleLineFields))
			return SINGLE_LINE_FIELD;
		if(checkField(current, multipleLineFields))
			return MULTIPLE_LINE_FIELD;
		return UNKNOWN;
	}
	
	/**
	 * Checks if a string begins with a phrase in a string array
	 * @param current The line to be checked  
	 * @param fieldArray The array of field names to be checked against
	 * @return 'true' if 'current' starts with a field in the 'fieldArray'
	 */
	private static boolean checkField(String current, String[] fieldArray)
	{	for(String field:fieldArray)
			if (current.startsWith(field))
				return true;
		return false;
	}
	
	public static String getFieldMatch(String commentStart, String[] multipleLineFields)
	{	for(String match:multipleLineFields)
			if (commentStart.startsWith(match))
				return match.substring(1).trim();
		return "NoMatch";
	}
	
	/**
	 * Checks a field for the codes that denote it is a course in an mbl tract description
	 * @param field The field to be checked.
	 * @return True if the field is a mbl geometry field.
	 */
	public static boolean isMBLgeoField(String field)
	{	if (field.equals("lm") || field.equals("ln") || field.equals("lc") || field.equals("pt"))
			return true;
		else return false;
	}
	
	/**
	 * Converts lines of KML into a table of records 
	 * @param linesOfKML The readin KML file, e.g. the output of readInLines(.KML)
	 * @return The parsed table of parcels
	 */
	public static DataTableW<String> kmlToTable(LinkedList<String> linesOfKML)
	{	CountingTree fieldList = new CountingTree();
		Parcel<String> record = null;
		LinkedList<Parcel<String>> table = new LinkedList<Parcel<String>>();
		CountingTree geometryCommentMap = new CountingTree(); // not used in this method, except to create a DataTableW<String>
		
		String current = "";
		int recordCount = 0, gid = NOT_GEOMETRY;
		String rcrdCntStr = (new Integer(0)).toString();
		String field = "", coords = "", gidStr = "", id = "";
		String[] lsCoords = null;
		String[] xyz = null;
		try	
		{	while(!linesOfKML.isEmpty() && !current.toLowerCase().equals("</kml>"))	
			{	recordCount++;
				rcrdCntStr = (new Integer(recordCount)).toString();
				current = linesOfKML.poll().trim();
				id = "";
				// A key to the positions in the DataRecord is as follows (note that position 3 is id except when gType is name):
			    //    0:pid, 1:gidStr, 2:gType, 3:name or id, 4:x, 5:y   
				record = new Parcel<String>();	
				while (!current.startsWith("</Placemark>") && !current.startsWith("</kml>"))
				{	if (current.startsWith("<name>") && current.endsWith("</name>"))
					{	String kmlName = getValueFromBetweenHTMLTags(current,"<name>");
						record.add(new DataRecordW<String>(new String[]{rcrdCntStr,NOT_GEOMETRYs,"name",kmlName},1,KML_PID),false);
						record.setKmlName(kmlName);
					}	
					else if (current.startsWith("<SimpleData") && current.endsWith("</SimpleData>"))
					{	if (current.toLowerCase().contains("\"id\""))
						{	id = getValueFromBetweenHTMLTags(current,"<SimpleData");
							record.add(new DataRecordW<String>(new String[]{rcrdCntStr,NOT_GEOMETRYs,"id",id},1,KML_PID),false);
							record.setComparator(id);//the parcels will be sorted by 'id' before joining tables
						}
					}
					else if (current.startsWith("<Point>") || current.startsWith("<LineString>"))	// actual geometry
					{	gid++;	// gid should begin at 0 because "point" does not join with the MBL
						gidStr = new Integer(gid).toString();
						field = current.substring(current.indexOf("<")+1, current.indexOf(">"));
						while (!current.startsWith("<coordinates>"))	
						{	current = linesOfKML.poll().trim();
							if (current.startsWith("<coordinates>"))
							{	coords = getValueFromBetweenHTMLTags(current,"<coordinates>");
								if (field.equals("Point"))		// point is a centroid, not a tract description point. So gid = 0		 
								{	xyz = coords.split(",");
									record.add(new DataRecordW<String>(new String[]{rcrdCntStr,gidStr,field,(id+"    ["+gid+"]"),xyz[0],xyz[1]},1,KML_PID),true);
								}
								else if (field.equals("LineString"))	// the actual points that join with the MBL, start with 1.
								{	lsCoords = coords.split(" ");
									for (String ls:lsCoords)
									{	xyz = ls.split(",");
										record.add(new DataRecordW<String>(new String[]{rcrdCntStr,gidStr,field,(id+"    ["+gid+"]"),xyz[0],xyz[1]},1,KML_PID),true);
										gid++;
										gidStr = new Integer(gid).toString();
									}
								}
							}
						}
					}
					else;
					if (!linesOfKML.isEmpty())
						current = linesOfKML.poll().trim(); 
				}
				table.add(record);
				gid = NOT_GEOMETRY;
			}
		}catch(Exception e)
		{	popupErrorDialog("An error occured while reading the kml file.","KML File Error", e);
		}
		return new DataTableW<String>(table,fieldList,geometryCommentMap);
	}
	
	
	public static String getValueFromBetweenHTMLTags(String lineOfKML, String tag)
	{	try
		{	String close = "</"; 
			if (tag.length() > 2)		
				close += tag.substring(1);
			return lineOfKML.substring(lineOfKML.indexOf(">")+1,lineOfKML.indexOf(close));	
		}catch(Exception e)
		{	popupErrorDialog("There was a problem reading the KML line: "+lineOfKML,"KML Read Error",e);
			return "";
		}
	}
	
	/**
	 * Joins the matching parcels and their records from the mbl and kml data tables.
	 * @param tableMBL The data table of a DeedMapper data file, e.g. from readInLines() then textToTable().
	 * @param tableKML The geometry table of a DeedMapper kml file, e.g. from readInLines() then kmlToTable().
	 * @return The joined data table of only geometry records, a.k.a. the courses from the tract descriptions.
	 */
	public static DataTableW<String> joinTables(DataTableW<String> tableMBL, DataTableW<String> tableKML)
	{	// Combine the tables and print out the number of joined parcels to the terminal
		int[] combineCounts = tableMBL.combineTables(tableKML, true);	// <-- combine the tables	
		int restoreCO = DataRecordW.getCompareOn();			// copy the comparison and key indices
		int[] restoreKI = Arrays.copyOf(DataRecordW.getKeyIndices(),DataRecordW.getKeyIndices().length);
		DataTableW<String> joinedTable = new DataTableW<String>();
		String message = 
			"KML & MBL Parcels combined: " + combineCounts[DataTableW.COMBINED_INDEX] 
		+ "\nKML parcels failed:         " + combineCounts[DataTableW.FAILED_INDEX]
		+ "\nKML parcels not matching:   " + combineCounts[DataTableW.NO_MATCH_KML_INDEX]
		+ "\nMBL parcels not matching:   " + combineCounts[DataTableW.NO_MATCH_MBL_INDEX]
		+ "\nTotal parcels: " + (combineCounts[DataTableW.COMBINED_INDEX]+combineCounts[DataTableW.FAILED_INDEX]
				+combineCounts[DataTableW.NO_MATCH_KML_INDEX]+combineCounts[DataTableW.NO_MATCH_MBL_INDEX])
		+ "\n\n To combine the 'id' must match and the number of tract courses must be equal."
		+   "\n Failed means that the parcel id's match but their number of courses differ."
		+   "\n Not matching means that no matching id can be found."
		+	"\n Note that the failed and not matching likely double count parcels.";
		
		Iterator<Parcel<String>> parcels = tableMBL.iterator();
		Parcel<String> currentParcel, joinedParcel;
		DataRecordW<String> currentRecord, findMe, joinMe = new DataRecordW<String>();
		int size = 0, index = 0;
		String fieldName = "", id = "", kmlName = "", mblID = "";
		int joinedCount = 0, kmlFailedCount = 0, kmlNoMatchCount = 0, mblNoMatchCount = 0;
		String[] fillMe;
		Iterator<String> joinMeIter;
		try	
		{	while(parcels.hasNext())
			{	currentParcel = parcels.next();
				joinedParcel = new Parcel<String>();
				kmlName = "";
				while(!currentParcel.isEmpty())
				{	currentRecord = currentParcel.poll();
					size = currentRecord.size();
					if(size >= MBL_VALUE)											// 1st check for being MBL geometry
					{	fieldName = currentRecord.get(MBL_FIELDNAME);
						if(fieldName.equals("id"))									// set the replacement parcel's comparator to the parcel id
						{	mblID = currentRecord.get(MBL_VALUE);
							joinedParcel.setComparator(mblID);
						}
						if (isMBLgeoField(fieldName) && size >= MBL_G_DDCOMMENT)	// 2nd check for being MBL geometry
						{	id = currentRecord.get(MBL_G_ID);						// gather info to make search record pt1
							fillMe = new String[MBL_G_ID + 1];						//   "      "   "  "      "     "    pt2
							Arrays.fill(fillMe, id);								//   "      "   "  "      "     "    pt3
							findMe = new DataRecordW<String>(fillMe,1,KML_ID);	// make the search record
							if(currentParcel.contains(findMe))						// search for the search record
							{	joinMe = currentParcel.remove(currentParcel.indexOf(findMe));	// remove the found KML record
								joinMeIter = joinMe.iterator();
								while(joinMeIter.hasNext())							// copy the KML record into the MBL record
								{	currentRecord.add(joinMeIter.next());
								}
								kmlName = currentParcel.getKmlName();
								currentRecord.getRecord().add(kmlName);
								joinedCount++;
								joinedParcel.add(currentRecord, true);					// add the joined record to the replacement parcel
							}
						}
					}				
					// Failed or KML records that do not match 
					if(currentParcel.isFailed() || currentParcel.isNoMatchKML()) // fill in blank MBL positions in KML records
					{	fieldName = currentRecord.get(KML_GTYPE);
						if(fieldName.equalsIgnoreCase("name"))
							kmlName = currentRecord.get(KML_NAME);
						if (!fieldName.equals("id") && !fieldName.equals("name") && !fieldName.equalsIgnoreCase("point"))
						{	fillMe = new String[MBL_G_ID + 1 + KML_Y + 1 + 1];
							index = MBL_G_ID + 1;
							for(String cur:currentRecord)	// copy the KML portions into the new array.
								fillMe[index++] = cur;
							fillMe[index++] = kmlName;			// add KML name on at the end
							if(currentParcel.isFailed())
								kmlFailedCount++;
							if(currentParcel.isNoMatchKML())
								kmlNoMatchCount++;
							joinedParcel.add(new DataRecordW<String>(fillMe), true);
						}
					}	
					// MBL record that did not match
					if(currentParcel.isNoMatchMBL())
					{	fillMe = new String[MBL_G_ID + 1 + KML_Y + 1 + 1];
						currentRecord.getRecord().toArray(fillMe);
						fillMe = Arrays.copyOf(fillMe, MBL_G_ID + 1 + KML_Y + 1 + 1); // fill in blanks at the end for KML 
						fillMe[MBL_G_ID + 1 + KML_Y + 1] = "";	// use nothing instead of KML name at the end
						mblNoMatchCount++;
						joinedParcel.add(new DataRecordW<String>(fillMe), true);
					}
				}	
				if(joinedParcel.size() > 0)
					joinedTable.add(joinedParcel);
			}
			DataRecordW.setCompareOn(restoreCO);
			for (index = 0; index < restoreKI.length; index++)
				DataRecordW.setKeyIndex(index, restoreKI[index]);
		}catch(Exception e)
		{	popupErrorDialog("There was a problem joining the tables.","Table Join Error",e);
		}
		message += "\n\nTract courses joined:     " + joinedCount 
				+    "\nKML courses failed:       " + kmlFailedCount
				+	 "\nKML courses not matching: " + kmlNoMatchCount
				+    "\nMBL courses not matching: " + mblNoMatchCount
				+    "\nTotal courses: " + (joinedCount + kmlFailedCount + kmlNoMatchCount + mblNoMatchCount);
		String label = "Conversion Results";
		JOptionPane.showMessageDialog(null,message,label,JOptionPane.INFORMATION_MESSAGE);
		return joinedTable;
	}
		
	/**
	 * This method joins the MBL and KML tables and writes the result to a tab delimited text file.
	 * @param tableMBL The MBL table, from readInLines() then textToTable().  
	 * @param tableKML The KML table, from readInLines() then kmlToTable().  
	 * @param geoCommentSearchTerms The array of terms to be searched for in the course description comments. 
	 * @param outputFile The tab delimited text file that will be written to.
	 * @param fileExtension The file extension to be appended to both of the output files.
	 * @return True if the file is written successfully.
	 */
	public static boolean writeOutputFiles(DataTableW<String> tableMBL, DataTableW<String> tableKML, String[] geoCommentSearchTerms, File outputFile, String fileExtension)
	{			// output the formatted HTML to the outputHTMLFile
		boolean result = false;
		String encoding = "UTF-8";
		Writer out = null;
		DataTableW<String> mblCopy = tableMBL.clone();
		
		File geoFile = appendSuffix(outputFile, true, false, fileExtension); 
		File flatFile = appendSuffix(outputFile, false, false, fileExtension);
		
		DataTableW<String> joinedTable = Witness.joinTables(tableMBL,tableKML); 	
		try
		{	out = new OutputStreamWriter(new FileOutputStream(geoFile), encoding);
			writeGeoFile(out,joinedTable.getTable(),geoCommentSearchTerms);
			out.close();
			out = new OutputStreamWriter(new FileOutputStream(flatFile), encoding);
			writeFlatFile(out,mblCopy.getFieldList(),mblCopy.getTable());
			result = true;			
		}catch (IOException ioe)
	    {	popupErrorDialog("There was a problem writing the output files.","File Write Error",ioe);
	    }catch (Exception e)
	    {	popupErrorDialog("There was a problem writing the files.","File Write Error",e);
	    }
		finally 
	    {	try 
	    	{	out.close();
	    	} catch (IOException ioe) 
	    	{	popupErrorDialog("There was a problem closing the writer.","File Writer Close Error",ioe);
	    	}
	    }
		return result;
	}
	
	public static boolean writeDuplicateFiles(DataTableW<String> tableMBL, DataTableW<String> tableKML, File outputFile, String fileExtension)
	{			// output the formatted HTML to the outputHTMLFile
		boolean result = false;
		String encoding = "UTF-8";
		Writer out = null;
		File kmlFile = appendSuffix(outputFile, true, true, fileExtension); 
		File mblFile = appendSuffix(outputFile, false, true, fileExtension);
		try
		{	out = new OutputStreamWriter(new FileOutputStream(kmlFile), encoding);		
			writeKMLflatFile(out,tableKML.getTable());
			out.close();
			out = new OutputStreamWriter(new FileOutputStream(mblFile), encoding);
			writeFlatFile(out,tableMBL.getFieldList(),tableMBL.getTable());
			result = true;			
		}catch (IOException ioe)
	    {	popupErrorDialog("There was an IO problem writing the duplicate files.","File Write IO Error",ioe);
	    }catch (Exception e)
	    {	popupErrorDialog("There was a problem writing the duplicate files.","File Write Error",e);
	    }
		finally 
	    {	try 
	    	{	out.close();
	    	} catch (IOException ioe) 
	    	{	popupErrorDialog("There was a problem closing the writer.","File Writer Close Error",ioe);
	    	}
	    }
		return result;
	}
	
	public static final String GEO_SUFFIX = "_geo";
	public static final String DATA_SUFFIX = "_flat";
	public static final String GEO_DUPLICATES_SUFFIX = "_kmlDup";
	public static final String DATA_DUPLICATES_SUFFIX = "_mblDup";

	/**
	 * Adds the geo or data file suffix to a file name stub.
	 * @param file The file onto which the suffix will be appended.
	 * @param isGeo When true the geo suffix will be appended, otherwise the data suffix.
	 * @param fileExtension The extension for the output files.
	 * @param isForDuplicates True if the appended suffixes are for a file listing duplicates.
	 * @return The altered file.
	 */
	public static File appendSuffix(File file, boolean isGeo, boolean isForDuplicates, String fileExtension)
	{	String path = "", stub = "", name = "";
		File returnFile = null;
		if (file != null)
		{	if(!file.isDirectory())
			{	path = file.getParent();
				name = file.getName();
				if(name.length() > 1 && name.contains("."))
					stub = name.substring(0, name.lastIndexOf("."));
				else stub = name;
			}
			else path = file.getAbsolutePath();
		}
		if (isGeo && !isForDuplicates)
			returnFile = new File(path,stub+GEO_SUFFIX+fileExtension); 
		else if (!isGeo && !isForDuplicates)
			returnFile = new File(path,stub+DATA_SUFFIX+fileExtension);
		else if (isGeo && isForDuplicates)
			returnFile = new File(path,stub+GEO_DUPLICATES_SUFFIX+fileExtension); 
		else if (!isGeo && isForDuplicates)
			returnFile = new File(path,stub+DATA_DUPLICATES_SUFFIX+fileExtension);
		return returnFile;
	}

	/**
	 * A poorly written method to write the joined geometry point file
	 * @param out 
	 * @param parcels
	 * @param geoCommentSearchTerms
	 * @return
	 * @throws IOException
	 */
	private static void writeGeoFile(Writer out, LinkedList<Parcel<String>> parcels, String[] geoCommentSearchTerms) throws IOException
	{	Parcel<String> parcel = null;
		DataRecordW<String> current;
		String uidStr = "", pid = "", gidStr = "", id = "", gType = "", dir = "", dist = "", gCmnt = "", found = "";
		int uid = 0, gid = 0;
		out.write("UID\tPID\tGID\tid\tGType\tDir\tDist\tGCmnt\tFoundTerms\tKML_pid\tKML_gid\tKML_gtype\tKML_name\tKML_id\tKML_x\tKML_y\n");
		while (!parcels.isEmpty())
		{	parcel = parcels.poll();
			while (!parcel.isEmpty())
			{	current = parcel.poll();
			//field order for 'record' {fieldName,rcrdCntStr,allFieldsCnt,cmntCntStr,fieldCntStr,edgePtCntStr,comment or additional fields: for geometry:direction,distance,ddComment,id; for 'loc': it is split on the " " character}
			//   fieldName may be 'commentLabel', 'before' or 'loc_tay'
				pid = current.getRecord().get(MBL_RECORDCOUNT);					// TODO change if order changes
				gidStr = current.getRecord().get(MBL_EDGEPOINTCOUNT);				// TODO change if order changes
				if(gidStr != null)
					gid = Integer.parseInt(gidStr);
				gType = current.getRecord().get(MBL_FIELDNAME);					// TODO change if order changes
				if (current.getRecord().size() > MBL_VALUE)					// TODO change if order changes
					dir = current.getRecord().get(MBL_G_DIRECTION);				// TODO change if order changes
				if (current.getRecord().size() > MBL_G_DISTANCE)					// TODO change if order changes
					dist = current.getRecord().get(MBL_G_DISTANCE);				// TODO change if order changes
				if (current.getRecord().size() > MBL_G_DDCOMMENT)					// TODO change if order changes
					gCmnt = current.getRecord().get(MBL_G_DDCOMMENT);				// TODO change if order changes
				if (current.getRecord().size() > MBL_G_ID)					// TODO change if order changes
					id = current.getRecord().get(MBL_G_ID);				// TODO change if order changes
				
				String kpid = "", kgid = "", kgtype = "", kid = "", kx = "", ky = "", kname = "";
				if (current.getRecord().size() > MBL_G_ID + 1 + KML_PID)					// TODO change if order changes
					kpid = current.getRecord().get(MBL_G_ID + 1 + KML_PID);				// TODO change if order changes
				if (current.getRecord().size() > MBL_G_ID + 1 + KML_GID)					// TODO change if order changes
					kgid = current.getRecord().get(MBL_G_ID + 1 + KML_GID);				// TODO change if order changes
				if (current.getRecord().size() > MBL_G_ID + 1 + KML_GTYPE)					// TODO change if order changes
					kgtype = current.getRecord().get(MBL_G_ID + 1 + KML_GTYPE);				// TODO change if order changes
				if (current.getRecord().size() > MBL_G_ID + 1 + KML_ID)					// TODO change if order changes
					kid = current.getRecord().get(MBL_G_ID + 1 + KML_ID);				// TODO change if order changes
				if (current.getRecord().size() > MBL_G_ID + 1 + KML_X)					// TODO change if order changes
					kx = current.getRecord().get(MBL_G_ID + 1 + KML_X);				// TODO change if order changes
				if (current.getRecord().size() > MBL_G_ID + 1 + KML_Y)					// TODO change if order changes
					ky = current.getRecord().get(MBL_G_ID + 1 + KML_Y);				// TODO change if order changes
				if (current.getRecord().size() > MBL_G_ID + 1 + KML_Y + 1)			// TODO change if order changes
					kname = current.getRecord().get(MBL_G_ID + 1 + KML_Y + 1);			// TODO change if order changes

				if (gid > 0)
				{	uidStr = (new Integer(++uid)).toString();
					found = parseGCmnt(gCmnt, geoCommentSearchTerms);
					if(found.length() > 1)
						found = found.substring(0, found.length()-2);
					out.write(uidStr+"\t"+pid+"\t"+gidStr+"\t"+id+"\t"+gType+"\t"+dir+"\t"+dist+"\t"+gCmnt+"\t"+found
							  +"\t"+kpid+"\t"+kgid+"\t"+kgtype+"\t"+kname+"\t"+kid+"\t"+kx+"\t"+ky+"\n");
				}
			}
		}
	}

	
	/**
	 * Writes a file of the overview information for each parcel in a tab delimited form, with one row per parcel. 
	 *   Fields are output in alphabetical order. 
	 * @param tree The sorted list of unique field names.
	 * @param parcels The list of parcels from which the overview information will be printed.
	 * @throws IOException 
	 */
	public static void writeFlatFile(Writer out, CountingTree fieldList, LinkedList<Parcel<String>> parcels) throws IOException
	{
		Parcel<String> parcel = null;
		Iterator<Object> iterFN = fieldList.iterator();
		String fieldNames = "", key = "";
		while (iterFN.hasNext())						// Make the string of field names
			fieldNames += iterFN.next() + "\t";
		out.write("PID\t" + fieldNames+"PointCount\n");			// Write the field names 
		Iterator<DataRecordW<String>> iterP = null;
		LinkedList<String> fieldOrder = null;
		while (!parcels.isEmpty())						// loop through the parcels
		{	parcel = parcels.poll();
			out.write(parcel.peek().getRecord().get(MBL_RECORDCOUNT) + "\t"); // write the parcel number
			iterP = parcel.iterator();
			fieldOrder = new LinkedList<String>();
			String field = "";
			while (iterP.hasNext())
			{	field = iterP.next().getRecord().get(MBL_FIELDNAME);	
				fieldOrder.add(field);										// list of field names
			}
			iterFN = fieldList.iterator();
			int index = 0, cnt = 0, treeSize = fieldList.size();
			key = "";
			while(iterFN.hasNext())
			{	cnt++;
				key = (String) iterFN.next();
				index = fieldOrder.indexOf(key);
				if (!key.equals("loc_tay") && !key.equals("lc") && !key.equals("lm") && !key.equals("ln") && !key.equals("pt"))
				{	
					if (index > -1 && cnt < treeSize && parcel.get(index).getRecord().size() > MBL_VALUE && !key.startsWith("z_cmnt"))
						out.write(parcel.get(index).getRecord().get(MBL_G_DIRECTION) + "\t");
					else if (index > -1 && cnt < treeSize && parcel.get(index).getRecord().size() > MBL_VALUE && key.startsWith("z_cmnt"))
						out.write(parcel.get(index).getRecord().get(MBL_VALUE) + "\t");
					else if (index > -1 && cnt == treeSize && parcel.get(index).getRecord().size() > MBL_VALUE)
						out.write(parcel.get(index).getRecord().get(MBL_VALUE) + "\t");
					else out.write("\t");
				}
			}
			out.write(parcel.getGeometryCount()+"\n");
		}
	}
	
	/**
	 * Use this method to find parcel 'comparator' values that are not unique. The 'fieldList' of the returned
	 *   DataTableW will have the correct field names, but the counts will be incorrect. 
	 * @param table The table to be tested.
	 * @param dupsOnly True if you want the return list to contain only duplicates, or false to contain unique.
	 * @return A list of either duplicate or unique parcels.
	 */
	public static DataTableW<String> countComparatorDuplicates(DataTableW<String> table, boolean returnDups)
	{	CountingHash idCounts = new CountingHash();
		LinkedList<Parcel<String>> tab = table.getTable();
		LinkedList<Parcel<String>> unique = new LinkedList<Parcel<String>>();
		LinkedList<Parcel<String>> duplicates = new LinkedList<Parcel<String>>();
		String test = "";
		boolean worked = true;
		try	
		{	for(Parcel<String> parcel:tab)
			{	test = parcel.getComparator();
				worked = idCounts.add(test);
				if (!worked)
					throw new Exception("CountingHash add error. Could not add: " + test);
				else	// no error in adding the key
				{	if(idCounts.getCount(test) > 1)	// key is not unique
					{	duplicates.add(parcel);		// add current parcel to 'duplicates'
						Parcel<String> find = new Parcel<String>(test);
						if(unique.contains(find))	// move the first instance of the non-unique comparator to 'duplicates'
							duplicates.add(unique.remove(unique.indexOf(find)));
					}
					else unique.add(parcel); // key is unique
				}
			}
		}catch(Exception e)
		{	popupErrorDialog("There was a problem counting duplicates.","Duplicate Count Error",e);
		}
		if (returnDups)
			return new DataTableW<String>(duplicates, table.getFieldList(),null);
		else return new DataTableW<String>(unique, table.getFieldList(),null);
	}

	/**
	 * Writes the KML table as a tab delimited file, with the point count replacing the individual 
	 * 	 courses of the tract description. 
	 * @param placemarks The table containing the KML geometry (output from kmlToTable())
	 */
	public static void writeKMLflatFile(Writer out, LinkedList<Parcel<String>> placemarks)
	{	Parcel<String> placemark = null;
		DataRecordW<String> current;
		String pid = "", name = "", id = "", gType = "", nameOrId = "", pointCount = "";
		try 
		{	out.write("pid\tname\tid\tPointCount");
			while (!placemarks.isEmpty())
			{	placemark = placemarks.poll();
				id = placemark.getComparator();
				pointCount = (new Integer(placemark.getGeometryCount())).toString();
				while (!placemark.isEmpty())	
				{	current = placemark.poll(); 
					// A key to the positions in the DataRecord is as follows (note that position 3 is id except when gType is name):
			    	//    0:pid, 1:gidStr, 2:gType, 3:name or id, 4:x, 5:y   
					gType = current.get(KML_GTYPE);							// TODO change if order changes
					nameOrId = current.getRecord().get(KML_ID);				// TODO change if order changes
					if (gType.toLowerCase().equals("name"))
						name = nameOrId;
					if (gType.toLowerCase().equals("id"))
						id = nameOrId;
				}
				out.write(pid+"\t"+name+"\t"+id+"\t"+pointCount+"\n");
			}
			out.write("\n");
		}catch (IOException e) 
		{	Witness.popupErrorDialog("There was an error writing the KML flat file.", "Error Writing KML Flat File", e);
		}
	}

	private static String parseGCmnt(String gCmnt, String[] geoCommentSearchTerms)
	{	String result = "";
		for(String searchTerm:geoCommentSearchTerms)
			if(gCmnt != null && searchTerm != null)
				if(gCmnt.toLowerCase().contains(searchTerm.toLowerCase()))
					result += searchTerm + ", ";
		return result;
	}

	/**
	 * Prints to the terminal the overview information for each parcel in a tab delimited form, with one row per parcel. 
	 *   Fields are output in alphabetical order. 
	 * @param tree The sorted list of unique field names.
	 * @param parcels The list of parcels from which the overview information will be printed.
	 */
	public static void printFlatFile(CountingTree tree, LinkedList<Parcel<String>> parcels)
	{
		Parcel<String> parcel = null;
		Iterator<Object> iterFN = tree.iterator();
		String fieldNames = "", key = "";
		while (iterFN.hasNext())
			fieldNames += iterFN.next() + "\t";
		System.out.println("PID\t" + fieldNames.trim());
		Iterator<DataRecordW<String>> iterP = null;
		LinkedList<String> fieldOrder = null;
		while (!parcels.isEmpty())
		{	parcel = parcels.poll();
			System.out.print(parcel.peek().getRecord().get(MBL_RECORDCOUNT) + "\t");
			iterP = parcel.iterator();
			fieldOrder = new LinkedList<String>();
			String field = "";
			while (iterP.hasNext())
			{	field = iterP.next().getRecord().get(MBL_FIELDNAME);
				fieldOrder.add(field);
			}
			iterFN = tree.iterator();
			int index = 0, cnt = 0, treeSize = tree.size();
			key = "";
			while(iterFN.hasNext())
			{	cnt++;
				key = (String)iterFN.next();
				index = fieldOrder.indexOf(key);
				if (!key.equals("loc_tay") && !key.equals("lc") && !key.equals("lm") && !key.equals("ln") && !key.equals("pt"))
				{	
					if (index > -1 && cnt < treeSize && parcel.get(index).getRecord().size() > MBL_VALUE && !key.startsWith("z_cmnt"))
						System.out.print(parcel.get(index).getRecord().get(MBL_G_DIRECTION) + "\t");
					else if (index > -1 && cnt < treeSize && parcel.get(index).getRecord().size() > MBL_VALUE && key.startsWith("z_cmnt"))
						System.out.print(""+parcel.get(index).getRecord().get(MBL_VALUE) + "\t");
					else if (index > -1 && cnt == treeSize && parcel.get(index).getRecord().size() > MBL_VALUE)
						System.out.print(parcel.get(index).getRecord().get(MBL_VALUE)+"\t");
					else System.out.print("\t");
				}
			}
			System.out.println(parcel.getGeometryCount()+"\n");
		}
	}
	
	/**
	 * Prints the key and count for a CountingTree
	 * @param tree The CountingTree you want printed
	 */
	public static void printTreeAndCount(CountingTree tree)
	{	
		System.out.println("Number of unique Keys: " + tree.size() +"\nCounts for each key:\n" + tree);
	}
	
	public static void printMBLgeo(LinkedList<Parcel<String>> parcels, String[] geoCommentSearchTerms)
	{
		Parcel<String> parcel = null;
		DataRecordW<String> current;
		String uidStr = "", pid = "", gidStr = "", to = "", dat = "", id = "", gType = "", dir = "", dist = "", gCmnt = "", found = "";
		int uid = 0, gid = 0;
		System.out.println("UID\tPID\tGID\tto\tdat\tid\tGType\tDir\tDist\tGCmnt\tFoundTerms");
		while (!parcels.isEmpty())
		{	parcel = parcels.poll();
			while (!parcel.isEmpty())
			{	current = parcel.poll();
			//field order for 'record' {fieldName,rcrdCntStr,allFieldsCnt,cmntCntStr,fieldCntStr,edgePtCntStr,comment or additional fields: for geometry:direction,distance,ddComment,id; for 'loc': it is split on the " " character}
			//   fieldName may be 'commentLabel', 'before' or 'loc_tay'
				pid = current.getRecord().get(MBL_RECORDCOUNT);					// TODO change if order changes
				gidStr = current.getRecord().get(MBL_EDGEPOINTCOUNT);				// TODO change if order changes
				gid = Integer.parseInt(gidStr);
				gType = current.getRecord().get(MBL_FIELDNAME);					// TODO change if order changes
				if (current.getRecord().size() > MBL_VALUE)					// TODO change if order changes
					dir = current.getRecord().get(MBL_G_DIRECTION);				// TODO change if order changes
				if (current.getRecord().size() > MBL_G_DISTANCE)					// TODO change if order changes
					dist = current.getRecord().get(MBL_G_DISTANCE);				// TODO change if order changes
				if (current.getRecord().size() > MBL_G_DDCOMMENT)					// TODO change if order changes
					gCmnt = current.getRecord().get(MBL_G_DDCOMMENT);				// TODO change if order changes
				if (current.getRecord().size() > MBL_G_ID)					// TODO change if order changes
					id = current.getRecord().get(MBL_G_ID);				// TODO change if order changes
				if (gType.equals("to"))		
					to = dir;
				else if (gType.equalsIgnoreCase("dat"))
					dat = dir;
				else if (gid > 0)
				{	uidStr = (new Integer(++uid)).toString();
					found = parseGCmnt(gCmnt, geoCommentSearchTerms);
					if(found.length() > 1)
						found = found.substring(0, found.length()-2);
					System.out.println(uidStr+"\t"+pid+"\t"+gidStr+"\t"+to+"\t"+dat+"\t"+id+"\t"+gType+"\t"+dir+"\t"+dist+"\t"+gCmnt+"\t"+found);
				}
			}
		}
		System.out.println();
	}
	
	/**
	 * Prints to the terminal the geometry of a KML file as a tab delimited table,
	 *   with each row being a single point
	 * @param placemarks The table containing the KML geometry (output from kmlToTable())
	 */
	public static void printKMLgeom(LinkedList<Parcel<String>> placemarks)
	{	
		Parcel<String> placemark = null;
		DataRecordW<String> current;
		String uidStr = "", pid = "", gidStr = "", name = "", id = "", gType = "", nameOrId = "", x = "", y = "";
		int uid = 0, gid = 0;
		System.out.println("uid\tpid\tgid\tname\tid\tgType\tx\ty");
		while (!placemarks.isEmpty())
		{	placemark = placemarks.poll();
			while (!placemark.isEmpty())	
			{	current = placemark.poll(); 
				// A key to the positions in the DataRecord is as follows (note that position 3 is id except when gType is name):
		    	//    0:pid, 1:gidStr, 2:gType, 3:name or id, 4:x, 5:y   
				pid = current.getRecord().get(KML_PID);					// TODO change if order changes
				gidStr = current.getRecord().get(KML_GID);				// TODO change if order changes
				gid = Integer.parseInt(gidStr);
				gType = current.getRecord().get(KML_GTYPE);					// TODO change if order changes
				nameOrId = current.getRecord().get(KML_ID);				// TODO change if order changes
				if (gType.equals("name"))		
					name = nameOrId;
				else
					id = nameOrId;
				if (gid >= 0)
				{	uidStr = (new Integer(++uid)).toString();
					x = current.getRecord().get(KML_X);					// TODO change if order changes
					y = current.getRecord().get(KML_Y);					// TODO change if order changes
					System.out.println(uidStr+"\t"+pid+"\t"+gidStr+"\t"+name+"\t"+id+"\t"+gType+"\t"+x+"\t"+y);
				}
			}
		}
		System.out.println();	
	}
	
	
	/**
	 * Prints the parcels with their full information in the order that they were read in. Each parcel is multiple lines,
	 *   separated by "== Parcel #000 ===================="
	 * @param parcels The list of parcels to be printed.
	 * @param idOnly True: prints only the id, false prints the entire parcel.
	 */
	public static void printParcels(LinkedList<Parcel<String>> parcels, boolean isMBL, boolean idOnly)
	{	int parcelCount = 0;
		Parcel<String> parcel = null;
		Parcel.setMessagesSilent(false);
		while (!parcels.isEmpty())
		{	parcel = parcels.poll();
			if (!idOnly)
			{	System.out.println("\n== Parcel #" + ++parcelCount + " ===================================================================================");
				System.out.println(parcel.toString());
/*				while (!parcel.isEmpty())
				{	System.out.println(parcel.poll().toString());		
				}
*/			}
			else
			{	if(!isMBL)
				{	try
					{	System.out.println(parcel.poll().getRecord().get(MBL_FIELDNAME) + "\t" + parcel.getComparator());
					}
					catch(Exception e)
					{	// do nothing	
					}
				}
				else
				{	try
					{	System.out.println(parcel.poll().getRecord().get(KML_GID) + "\t" + parcel.getComparator());
					}
					catch(Exception e)
					{	// do nothing	
					}
				}
			}
		}
		System.out.println();		
	}
	
	/**
	 * Prints the frequency of each field in the records of the table 
	 * @param fieldList the field list
	 */
	public static void printFieldFrequency(CountingTree fieldList)  
	{	// Print field frequency
		System.out.println("Frequency of each field name:\n"+fieldList); // TODO indexing for comments needs to change to reflect actual position in list, so the comments in the parcel description can be picked up properly
	}
	
}